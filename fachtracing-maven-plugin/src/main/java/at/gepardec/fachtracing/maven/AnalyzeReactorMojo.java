package at.gepardec.fachtracing.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Runs one decision analysis for the effective Maven reactor selection. */
@Mojo(name = "analyze-reactor", aggregator = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public final class AnalyzeReactorMojo extends AbstractMojo {
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** Coordinates to retain after Maven applies its reactor selection. */
    @Parameter
    private List<String> includeProjects;

    /** Coordinates to remove after Maven applies its reactor selection. */
    @Parameter
    private List<String> excludeProjects;

    @Parameter(defaultValue = "${session.executionRootDirectory}/target/fachtracing",
            property = "fachtracing.aggregateOutputDirectory", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.sourceEncoding}", property = "fachtracing.encoding")
    private String encoding;

    @Parameter(defaultValue = "false", property = "fachtracing.failOnIncomplete")
    private boolean failOnIncomplete;

    @Parameter(defaultValue = "false", property = "fachtracing.skip")
    private boolean skip;

    @Override public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Fachtracing aggregate analysis skipped");
            return;
        }
        try {
            List<MavenProject> projects = selectedProjects();
            List<String> roots = projects.stream()
                    .flatMap(candidate -> candidate.getCompileSourceRoots().stream()).toList();
            List<Path> sources = AnalyzeMojo.sourceFiles(roots);
            Charset charset = encoding == null || encoding.isBlank()
                    ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            var result = new ProjectGraphGenerator().generate(
                    sources, sources, AnalyzeMojo.compileClasspath(projects), charset,
                    outputDirectory.toPath(), failOnIncomplete);
            writeActivationBundle(projects, sources, result);
            getLog().info("Generated " + result.graphCount()
                    + " aggregate Fachtracing decision graph(s) from " + projects.size() + " project(s)");
        } catch (ProjectGraphGenerator.IncompleteGraphException error) {
            throw new MojoFailureException(error.getMessage()
                    + ". Review the aggregate diagrams or set -Dfachtracing.failOnIncomplete=false.", error);
        } catch (DependencyResolutionRequiredException error) {
            throw new MojoExecutionException("Could not resolve the aggregate compile classpath", error);
        } catch (IllegalArgumentException error) {
            throw new MojoFailureException("Could not analyze the Maven reactor: " + error.getMessage(), error);
        } catch (IOException error) {
            throw new MojoExecutionException("Could not write aggregate Fachtracing output", error);
        }
    }

    private List<MavenProject> selectedProjects() {
        List<MavenProject> effective = session.getProjects();
        if (effective == null || effective.isEmpty()) effective = List.of(project);
        var includes = normalizedCoordinates(includeProjects);
        var excludes = normalizedCoordinates(excludeProjects);
        List<MavenProject> selected = effective.stream()
                .filter(candidate -> includes.isEmpty() || includes.contains(coordinate(candidate)))
                .filter(candidate -> !excludes.contains(coordinate(candidate)))
                .sorted(Comparator.comparing(AnalyzeReactorMojo::coordinate)).toList();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("the effective reactor selection contains no included project");
        }
        if (!includes.isEmpty()) {
            var missing = new LinkedHashSet<>(includes);
            effective.stream().map(AnalyzeReactorMojo::coordinate).forEach(missing::remove);
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("included projects are not in the effective reactor: " + missing);
            }
        }
        return selected;
    }

    private void writeActivationBundle(
            List<MavenProject> projects,
            List<Path> sources,
            ProjectGraphGenerator.GenerationResult result) throws IOException {
        Files.createDirectories(outputDirectory.toPath());
        String projectJson = projects.stream().map(AnalyzeReactorMojo::coordinate)
                .map(AnalyzeReactorMojo::json).reduce((left, right) -> left + "," + right).orElse("");
        String sourceJson = sources.stream().map(AnalyzeReactorMojo::fingerprint).sorted()
                .map(AnalyzeReactorMojo::json).reduce((left, right) -> left + "," + right).orElse("");
        String content = "{\"schema\":\"fachtracing-activation/v1\",\"projects\":["
                + projectJson + "],\"sourceFingerprints\":[" + sourceJson + "],\"graphCount\":"
                + result.graphCount() + ",\"incompleteCount\":" + result.incompleteCount() + "}\n";
        Files.writeString(outputDirectory.toPath().resolve("activation.json"), content, StandardCharsets.UTF_8);
    }

    private static String coordinate(MavenProject project) {
        return project.getGroupId() + ':' + project.getArtifactId();
    }

    private static java.util.Set<String> normalizedCoordinates(List<String> values) {
        if (values == null) return java.util.Set.of();
        var result = new LinkedHashSet<String>();
        for (String value : values) {
            String coordinate = Objects.requireNonNull(value, "project coordinate").trim();
            if (coordinate.split(":", -1).length != 2 || coordinate.startsWith(":") || coordinate.endsWith(":")) {
                throw new IllegalArgumentException("project coordinate must use groupId:artifactId: " + value);
            }
            result.add(coordinate);
        }
        return java.util.Set.copyOf(result);
    }

    private static String json(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String fingerprint(Path source) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(source));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        } catch (IOException error) {
            throw new IllegalStateException("could not fingerprint aggregate source", error);
        }
    }
}
