package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.analysis.OpaqueLibraryBoundary;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Maps explicit Maven dependency coordinates to exact resolved compile-classpath archives. */
final class OpaqueLibraryArtifactResolver {
    private OpaqueLibraryArtifactResolver() { }

    static OpaqueLibraryBoundary resolve(
            List<MavenProject> projects,
            List<String> configuredCoordinates) throws DependencyResolutionRequiredException {
        Objects.requireNonNull(projects, "projects");
        Set<String> selected = normalizedCoordinates(configuredCoordinates);
        if (selected.isEmpty()) return OpaqueLibraryBoundary.empty();

        var compileClasspath = new LinkedHashSet<Path>();
        for (MavenProject project : projects) {
            project.getCompileClasspathElements().stream()
                    .map(Path::of)
                    .map(OpaqueLibraryArtifactResolver::normalize)
                    .forEach(compileClasspath::add);
        }

        var resolvedByCoordinate = new LinkedHashMap<String, Set<Path>>();
        selected.forEach(coordinate -> resolvedByCoordinate.put(coordinate, new TreeSet<>(
                Comparator.comparing(Path::toString))));
        for (MavenProject project : projects) {
            for (Artifact artifact : project.getArtifacts()) {
                String coordinate = artifact.getGroupId() + ':' + artifact.getArtifactId();
                if (!selected.contains(coordinate) || artifact.getFile() == null) continue;
                Path file = normalize(artifact.getFile().toPath());
                if (compileClasspath.contains(file) && Files.isRegularFile(file)
                        && file.getFileName().toString().endsWith(".jar")) {
                    resolvedByCoordinate.get(coordinate).add(file);
                }
            }
        }

        List<String> missing = resolvedByCoordinate.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(java.util.Map.Entry::getKey).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "opaque library artifacts are not resolved compile-classpath JARs: " + missing);
        }
        List<Path> archives = resolvedByCoordinate.values().stream()
                .flatMap(Set::stream).distinct().sorted(Comparator.comparing(Path::toString)).toList();
        return OpaqueLibraryBoundary.of(archives);
    }

    static Set<String> normalizedCoordinates(List<String> values) {
        if (values == null) return Set.of();
        var result = new LinkedHashSet<String>();
        for (String configured : values) {
            Objects.requireNonNull(configured, "opaque library coordinate");
            for (String value : configured.split(",", -1)) {
                String coordinate = value.trim();
                String[] parts = coordinate.split(":", -1);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    throw new IllegalArgumentException(
                            "opaque library coordinate must use groupId:artifactId: " + value);
                }
                result.add(coordinate);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
