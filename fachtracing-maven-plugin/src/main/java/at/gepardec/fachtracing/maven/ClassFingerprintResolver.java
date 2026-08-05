package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

/** Resolves selected runtime classes and fingerprints their original build output. */
final class ClassFingerprintResolver {
    private ClassFingerprintResolver() { }

    static Map<String, String> resolve(
            List<MavenProject> projects,
            List<AnalysisManifest.AnalysisResult> analyses) throws IOException {
        Set<String> needed = selectedOwners(analyses);
        var found = new LinkedHashMap<String, FoundClass>();
        var locations = new LinkedHashSet<Path>();
        for (MavenProject project : projects) {
            if (project.getBuild() != null && project.getBuild().getOutputDirectory() != null) {
                locations.add(Path.of(project.getBuild().getOutputDirectory()).toAbsolutePath().normalize());
            }
            try {
                project.getCompileClasspathElements().stream().map(Path::of)
                        .map(path -> path.toAbsolutePath().normalize()).forEach(locations::add);
            } catch (org.apache.maven.artifact.DependencyResolutionRequiredException failure) {
                throw new IllegalArgumentException("compile classpath is unavailable for "
                        + project.getGroupId() + ':' + project.getArtifactId(), failure);
            }
            project.getArtifacts().stream().map(artifact -> artifact.getFile())
                    .filter(java.util.Objects::nonNull).map(java.io.File::toPath)
                    .map(path -> path.toAbsolutePath().normalize()).forEach(locations::add);
        }
        for (Path location : locations) {
            if (Files.isDirectory(location)) scanDirectory(location, needed, found);
            else if (Files.isRegularFile(location) && location.toString().endsWith(".jar")) {
                scanArchive(location, needed, found);
            }
        }
        var missing = new ArrayList<>(needed);
        missing.removeAll(found.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("compiled runtime classes are unavailable for activation: " + missing);
        }
        var result = new LinkedHashMap<String, String>();
        found.values().stream().sorted(java.util.Comparator.comparing(FoundClass::internalName))
                .forEach(item -> result.put(item.internalName(), item.fingerprint()));
        return Map.copyOf(result);
    }

    private static Set<String> selectedOwners(List<AnalysisManifest.AnalysisResult> analyses) {
        var result = new LinkedHashSet<String>();
        for (AnalysisManifest.AnalysisResult analysis : analyses) {
            analysis.manifest().probeSites().forEach(site -> result.add(site.ownerHint()));
            analysis.manifest().dispatchTargets().forEach(target -> result.add(target.ownerHint()));
            analysis.manifest().branchTargets().forEach(target -> result.add(target.ownerHint()));
        }
        return Set.copyOf(result);
    }

    private static void scanDirectory(
            Path root, Set<String> needed, Map<String, FoundClass> found) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(item -> item.toString().endsWith(".class")).toList()) {
                String internal = root.relativize(path).toString().replace(java.io.File.separatorChar, '/');
                internal = internal.substring(0, internal.length() - ".class".length());
                addIfNeeded(internal, Files.readAllBytes(path), path.toString(), needed, found);
            }
        }
    }

    private static void scanArchive(
            Path archive, Set<String> needed, Map<String, FoundClass> found) throws IOException {
        try (var zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries().asIterator();
            while (entries.hasNext()) {
                var entry = entries.next();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                String internal = entry.getName().substring(0, entry.getName().length() - ".class".length());
                try (InputStream input = zip.getInputStream(entry)) {
                    addIfNeeded(internal, input.readAllBytes(), archive + "!/" + entry.getName(), needed, found);
                }
            }
        }
    }

    private static void addIfNeeded(
            String internal,
            byte[] bytes,
            String origin,
            Set<String> needed,
            Map<String, FoundClass> found) {
        String canonical = internal.replace('/', '.').replace('$', '.');
        if (!needed.contains(canonical)) return;
        var candidate = new FoundClass(internal, sha256(bytes), origin);
        FoundClass previous = found.putIfAbsent(canonical, candidate);
        if (previous != null && (!previous.fingerprint().equals(candidate.fingerprint())
                || !previous.internalName().equals(candidate.internalName()))) {
            throw new IllegalArgumentException("conflicting compiled runtime class " + canonical
                    + " at " + previous.origin() + " and " + origin);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private record FoundClass(String internalName, String fingerprint, String origin) { }
}
