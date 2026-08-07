package at.gepardec.fachtracing.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Exact dependency archives that the caller declares as technical operation boundaries. */
public record OpaqueLibraryBoundary(Set<Path> archiveFiles) {
    public OpaqueLibraryBoundary {
        Objects.requireNonNull(archiveFiles, "archiveFiles");
        var normalized = new TreeSet<Path>(Comparator.comparing(Path::toString));
        for (Path archive : archiveFiles) {
            Path path = Objects.requireNonNull(archive, "archive file")
                    .toAbsolutePath().normalize();
            if (!Files.isRegularFile(path) || !Files.isReadable(path)
                    || !path.getFileName().toString().endsWith(".jar")) {
                throw new IllegalArgumentException("opaque library boundary is not a readable JAR: " + path);
            }
            normalized.add(path);
        }
        archiveFiles = Collections.unmodifiableSet(normalized);
    }

    /** Creates an empty boundary that keeps all source-unavailable dependency logic fail-closed. */
    public static OpaqueLibraryBoundary empty() {
        return new OpaqueLibraryBoundary(Set.of());
    }

    /** Creates a boundary from exact resolved archive paths. */
    public static OpaqueLibraryBoundary of(Collection<Path> archiveFiles) {
        Objects.requireNonNull(archiveFiles, "archiveFiles");
        return new OpaqueLibraryBoundary(Set.copyOf(archiveFiles));
    }

    /** Tests one normalized binary origin against the declared archive set. */
    public boolean contains(Path archive) {
        return archive != null && archiveFiles.contains(archive.toAbsolutePath().normalize());
    }
}
