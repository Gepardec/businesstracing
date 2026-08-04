package at.gepardec.fachtracing.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Resolves explicitly named Maven source artifacts and extracts bounded Java source inputs. */
final class SourceInputResolver {
    private final RepositorySystem repositorySystem;
    private final MavenSession session;
    private final MavenProject project;
    private final Path extractionDirectory;
    private final ArchiveLimits limits;

    SourceInputResolver(
            RepositorySystem repositorySystem,
            MavenSession session,
            MavenProject project,
            Path extractionDirectory,
            ArchiveLimits limits) {
        this.repositorySystem = Objects.requireNonNull(repositorySystem, "repositorySystem");
        this.session = Objects.requireNonNull(session, "session");
        this.project = Objects.requireNonNull(project, "project");
        this.extractionDirectory = Objects.requireNonNull(extractionDirectory, "extractionDirectory")
                .toAbsolutePath().normalize();
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    List<ResolvedSourceArtifact> resolve(List<String> coordinates) throws IOException {
        if (coordinates == null || coordinates.isEmpty()) return List.of();
        var resolved = new ArrayList<ResolvedSourceArtifact>();
        for (String coordinate : coordinates.stream().distinct().sorted().toList()) {
            Coordinate parsed = Coordinate.parse(coordinate);
            Artifact artifact = repositorySystem.createArtifactWithClassifier(
                    parsed.groupId(), parsed.artifactId(), parsed.version(), "jar", "sources");
            var request = new ArtifactResolutionRequest()
                    .setArtifact(artifact)
                    .setLocalRepository(session.getLocalRepository())
                    .setRemoteRepositories(project.getRemoteArtifactRepositories())
                    .setOffline(session.getRequest().isOffline())
                    .setResolveRoot(true)
                    .setResolveTransitively(false);
            var result = repositorySystem.resolve(request);
            if (!result.isSuccess() || artifact.getFile() == null || !artifact.getFile().isFile()) {
                String mode = session.getRequest().isOffline() ? " while Maven is offline" : "";
                throw new IllegalArgumentException("could not resolve exact source artifact "
                        + coordinate + mode);
            }
            Path archive = artifact.getFile().toPath().toAbsolutePath().normalize();
            String checksum = sha256(Files.readAllBytes(archive));
            Path root = extractionDirectory.resolve(checksum);
            List<Path> sources = extract(archive, root, limits);
            resolved.add(new ResolvedSourceArtifact(parsed.toString(), checksum, root, sources));
        }
        return List.copyOf(resolved);
    }

    static List<Path> extract(Path archive, Path outputRoot, ArchiveLimits limits) throws IOException {
        Objects.requireNonNull(archive, "archive");
        Objects.requireNonNull(outputRoot, "outputRoot");
        Objects.requireNonNull(limits, "limits");
        Path root = outputRoot.toAbsolutePath().normalize();
        Files.createDirectories(root.getParent());
        Path staging = Files.createTempDirectory(root.getParent(), root.getFileName() + ".tmp-");
        var relativeSources = new ArrayList<Path>();
        Set<Path> paths = new HashSet<>();
        long totalBytes = 0;
        int entries = 0;

        try (var zip = new ZipFile(archive.toFile())) {
            var iterator = zip.entries().asIterator();
            while (iterator.hasNext()) {
                ZipEntry entry = iterator.next();
                entries++;
                if (entries > limits.maxEntries()) {
                    throw new IllegalArgumentException("source archive entry limit exceeded: " + archive);
                }
                String name = entry.getName();
                if (name.indexOf('\\') >= 0) {
                    throw new IllegalArgumentException("source archive entry uses a backslash path: " + name);
                }
                Path relative;
                try {
                    relative = Path.of(name).normalize();
                } catch (InvalidPathException invalid) {
                    throw new IllegalArgumentException("invalid source archive entry: " + name, invalid);
                }
                String canonicalName = entry.isDirectory() ? relative + "/" : relative.toString();
                if (relative.isAbsolute() || relative.getNameCount() == 0
                        || relative.startsWith("..") || !canonicalName.equals(name)) {
                    throw new IllegalArgumentException("unsafe source archive entry: " + name);
                }
                Path destination = staging.resolve(relative).normalize();
                if (!destination.startsWith(staging)) {
                    throw new IllegalArgumentException("source archive entry escapes extraction root: " + name);
                }
                if (!paths.add(destination)) {
                    throw new IllegalArgumentException("duplicate source archive entry: " + name);
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }
                long declared = entry.getSize();
                if (declared > limits.maxEntryBytes()) {
                    throw new IllegalArgumentException("source archive entry is too large: " + name);
                }
                Files.createDirectories(destination.getParent());
                long written = copyBounded(zip.getInputStream(entry), destination, limits.maxEntryBytes());
                totalBytes = Math.addExact(totalBytes, written);
                if (totalBytes > limits.maxTotalBytes()) {
                    throw new IllegalArgumentException("source archive total size limit exceeded: " + archive);
                }
                if (destination.toString().endsWith(".java")) relativeSources.add(relative);
            }
        } catch (IOException | RuntimeException error) {
            deleteTree(staging);
            throw error;
        }
        deleteTree(root);
        try {
            Files.move(staging, root, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(staging, root);
        }
        return relativeSources.stream().map(root::resolve)
                .sorted(Comparator.comparing(Path::toString)).toList();
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static long copyBounded(InputStream input, Path destination, long maxBytes) throws IOException {
        try (input; var output = Files.newOutputStream(destination,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("source archive entry exceeds configured size: " + destination);
                }
                output.write(buffer, 0, read);
            }
            return total;
        }
    }

    private static String sha256(byte[] value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    record ArchiveLimits(int maxEntries, long maxEntryBytes, long maxTotalBytes) {
        ArchiveLimits {
            if (maxEntries < 1) throw new IllegalArgumentException("maxEntries must be positive");
            if (maxEntryBytes < 1) throw new IllegalArgumentException("maxEntryBytes must be positive");
            if (maxTotalBytes < maxEntryBytes) {
                throw new IllegalArgumentException("maxTotalBytes must be at least maxEntryBytes");
            }
        }
    }

    record ResolvedSourceArtifact(String coordinate, String checksum, Path root, List<Path> sourceFiles) {
        ResolvedSourceArtifact {
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            checksum = Objects.requireNonNull(checksum, "checksum");
            root = Objects.requireNonNull(root, "root");
            sourceFiles = List.copyOf(sourceFiles);
        }
    }

    private record Coordinate(String groupId, String artifactId, String version) {
        private static Coordinate parse(String value) {
            if (value == null) throw new IllegalArgumentException("source dependency coordinate is required");
            String[] parts = value.trim().split(":", -1);
            if (parts.length != 3 || java.util.Arrays.stream(parts).anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("source dependency must use groupId:artifactId:version: " + value);
            }
            return new Coordinate(parts[0], parts[1], parts[2]);
        }

        @Override public String toString() {
            return groupId + ':' + artifactId + ':' + version;
        }
    }
}
