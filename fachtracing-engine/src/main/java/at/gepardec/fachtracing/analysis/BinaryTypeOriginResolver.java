package at.gepardec.fachtracing.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;

/** Resolves the first ordered classpath origin for an exact binary type. */
final class BinaryTypeOriginResolver {
    private final List<Path> classpath;
    private final Map<String, Resolution> origins = new HashMap<>();

    BinaryTypeOriginResolver(List<Path> classpath) {
        this.classpath = classpath.stream()
                .map(path -> path.toAbsolutePath().normalize()).toList();
    }

    Resolution resolve(String binaryName) {
        return origins.computeIfAbsent(binaryName, this::locate);
    }

    private Resolution locate(String binaryName) {
        String entry = binaryName.replace('.', '/') + ".class";
        for (Path location : classpath) {
            try {
                if (Files.isDirectory(location) && Files.isRegularFile(location.resolve(entry))) {
                    return new Resolution(Origin.DIRECTORY, Optional.of(location));
                }
                if (Files.isRegularFile(location) && location.toString().endsWith(".jar")) {
                    try (var archive = new ZipFile(location.toFile())) {
                        if (archive.getEntry(entry) != null) {
                            return new Resolution(Origin.ARCHIVE, Optional.of(location));
                        }
                    }
                }
            } catch (IOException failure) {
                throw new IllegalArgumentException("could not inspect binary type origin " + location, failure);
            }
        }
        return new Resolution(Origin.UNAVAILABLE, Optional.empty());
    }

    record Resolution(Origin origin, Optional<Path> location) {
        Resolution {
            if (origin == Origin.UNAVAILABLE && location.isPresent()) {
                throw new IllegalArgumentException("unavailable binary origin has a location");
            }
            if (origin != Origin.UNAVAILABLE && location.isEmpty()) {
                throw new IllegalArgumentException("resolved binary origin has no location");
            }
        }
    }

    enum Origin {
        ARCHIVE,
        DIRECTORY,
        UNAVAILABLE
    }
}
