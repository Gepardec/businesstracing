package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.analysis.ApplicationSourceBoundary;
import at.gepardec.fachtracing.developer.DeveloperGraphExporter;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.repository.RepositorySystem;
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
import java.util.LinkedHashMap;
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

    @Component
    private RepositorySystem repositorySystem;

    /** Coordinates to retain after Maven applies its reactor selection. */
    @Parameter
    private List<String> includeProjects;

    /** Coordinates to remove after Maven applies its reactor selection. */
    @Parameter
    private List<String> excludeProjects;

    /** Local Java roots used only for aggregate call and implementation resolution. */
    @Parameter(property = "fachtracing.additionalSourceRoots")
    private List<File> additionalSourceRoots;

    /** Local Java roots that can also contain aggregate graph entries. */
    @Parameter(property = "fachtracing.additionalEntrySourceRoots")
    private List<File> additionalEntrySourceRoots;

    /** Exact source classifier coordinates used only for aggregate resolution. */
    @Parameter(property = "fachtracing.sourceDependencies")
    private List<String> sourceDependencies;

    @Parameter(defaultValue = "${session.executionRootDirectory}/target/fachtracing-source-dependencies",
            property = "fachtracing.sourceExtractionDirectory")
    private File sourceExtractionDirectory;

    @Parameter(defaultValue = "10000", property = "fachtracing.sourceArchiveMaxEntries")
    private int sourceArchiveMaxEntries;

    @Parameter(defaultValue = "4194304", property = "fachtracing.sourceArchiveMaxEntryBytes")
    private long sourceArchiveMaxEntryBytes;

    @Parameter(defaultValue = "134217728", property = "fachtracing.sourceArchiveMaxTotalBytes")
    private long sourceArchiveMaxTotalBytes;

    @Parameter(property = "fachtracing.repositoryUrl")
    private String repositoryUrl;

    @Parameter(property = "fachtracing.sourceUrlTemplate")
    private String sourceUrlTemplate;

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
            Charset charset = encoding == null || encoding.isBlank()
                    ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            List<Path> entrySources = AnalyzeMojo.sourceFiles(paths(additionalEntrySourceRoots));
            List<SourceInputResolver.ResolvedSourceArtifact> artifacts = resolveSourceDependencies();
            List<ApplicationSourceBoundary.ResolutionSource> externalSources = externalSources(artifacts);
            ApplicationSourceBoundary boundary = boundary(
                    projects, additionalEntryHost(projects), entrySources, externalSources);
            List<DeveloperGraphExporter.SourceOrigin> origins = sourceOrigins(projects, artifacts);
            var result = new ProjectGraphGenerator().generate(
                    boundary, charset, outputDirectory.toPath(), failOnIncomplete,
                    developerOutput(origins));
            writeActivationBundle(projects, boundary, result);
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

    private static ApplicationSourceBoundary boundary(
            List<MavenProject> projects,
            String entryProjectId,
            List<Path> additionalEntries,
            List<ApplicationSourceBoundary.ResolutionSource> externalSources)
            throws IOException, DependencyResolutionRequiredException {
        var selectedIds = projects.stream().map(AnalyzeReactorMojo::coordinate)
                .collect(java.util.stream.Collectors.toSet());
        var models = new java.util.ArrayList<ApplicationSourceBoundary.ProjectSources>();
        for (MavenProject candidate : projects) {
            List<Path> classpath = candidate.getCompileClasspathElements().stream()
                    .map(Path::of).map(path -> path.toAbsolutePath().normalize()).toList();
            java.util.Optional<Path> module = moduleDescriptor(candidate.getCompileSourceRoots());
            var compiler = MavenCompilerModelResolver.resolve(candidate, classpath, module.isPresent());
            List<Path> sources = AnalyzeMojo.sourceFiles(compiler.compileSourceRoots());
            List<Path> entries = new java.util.ArrayList<>(sources);
            if (coordinate(candidate).equals(entryProjectId)) {
                entries.addAll(additionalEntries);
                sources = java.util.stream.Stream.concat(sources.stream(), additionalEntries.stream())
                        .distinct().sorted(Comparator.comparing(Path::toString)).toList();
            }
            List<String> dependencies = candidate.getDependencies().stream()
                    .map(dependency -> dependency.getGroupId() + ':' + dependency.getArtifactId())
                    .filter(selectedIds::contains).distinct().sorted().toList();
            models.add(new ApplicationSourceBoundary.ProjectSources(
                    coordinate(candidate), entries, sources, classpath,
                    compiler.compilerModel(), dependencies, module));
        }
        return new ApplicationSourceBoundary(models, externalSources);
    }

    private List<SourceInputResolver.ResolvedSourceArtifact> resolveSourceDependencies() throws IOException {
        if (sourceDependencies == null || sourceDependencies.isEmpty()) return List.of();
        if (repositorySystem == null || session == null) {
            throw new IllegalStateException("Maven source artifact resolver is unavailable");
        }
        var limits = new SourceInputResolver.ArchiveLimits(
                sourceArchiveMaxEntries, sourceArchiveMaxEntryBytes, sourceArchiveMaxTotalBytes);
        return new SourceInputResolver(repositorySystem, session, project,
                sourceExtractionDirectory.toPath(), limits).resolve(sourceDependencies);
    }

    private List<ApplicationSourceBoundary.ResolutionSource> externalSources(
            List<SourceInputResolver.ResolvedSourceArtifact> artifacts) throws IOException {
        var result = new java.util.ArrayList<ApplicationSourceBoundary.ResolutionSource>();
        int local = 0;
        for (File root : safeFiles(additionalSourceRoots)) {
            var origin = new ApplicationSourceBoundary.SourceOrigin(
                    ApplicationSourceBoundary.OriginKind.LOCAL, "aggregate-local-" + (++local), "");
            AnalyzeMojo.sourceFiles(List.of(root.getPath())).forEach(path ->
                    result.add(new ApplicationSourceBoundary.ResolutionSource(path, origin)));
        }
        for (SourceInputResolver.ResolvedSourceArtifact artifact : artifacts) {
            var origin = new ApplicationSourceBoundary.SourceOrigin(
                    ApplicationSourceBoundary.OriginKind.MAVEN_SOURCE,
                    artifact.coordinate(), artifact.checksum());
            artifact.sourceFiles().forEach(path ->
                    result.add(new ApplicationSourceBoundary.ResolutionSource(path, origin)));
        }
        return List.copyOf(result);
    }

    private List<DeveloperGraphExporter.SourceOrigin> sourceOrigins(
            List<MavenProject> projects,
            List<SourceInputResolver.ResolvedSourceArtifact> artifacts) {
        var result = new LinkedHashMap<Path, DeveloperGraphExporter.SourceOrigin>();
        int index = 0;
        for (File root : safeFiles(additionalSourceRoots)) {
            Path path = root.toPath().toAbsolutePath().normalize();
            result.putIfAbsent(path, DeveloperGraphExporter.SourceOrigin.external(
                    "aggregate-local-" + (++index), DeveloperGraphExporter.OriginKind.LOCAL,
                    path, "configured aggregate resolution source", ""));
        }
        for (File root : safeFiles(additionalEntrySourceRoots)) {
            Path path = root.toPath().toAbsolutePath().normalize();
            result.putIfAbsent(path, DeveloperGraphExporter.SourceOrigin.external(
                    "aggregate-entry-" + (++index), DeveloperGraphExporter.OriginKind.LOCAL,
                    path, "configured aggregate entry source", ""));
        }
        for (SourceInputResolver.ResolvedSourceArtifact artifact : artifacts) {
            result.put(artifact.root(), DeveloperGraphExporter.SourceOrigin.external(
                    "aggregate-maven-" + (++index), DeveloperGraphExporter.OriginKind.MAVEN_SOURCE,
                    artifact.root(), artifact.coordinate(), artifact.checksum()));
        }
        for (MavenProject candidate : projects) {
            Path build = Path.of(candidate.getBuild().getDirectory()).toAbsolutePath().normalize();
            for (String root : candidate.getCompileSourceRoots()) {
                Path path = Path.of(root).toAbsolutePath().normalize();
                if (path.startsWith(build) && Files.isDirectory(path)) {
                    result.putIfAbsent(path, DeveloperGraphExporter.SourceOrigin.external(
                            "aggregate-generated-" + (++index), DeveloperGraphExporter.OriginKind.GENERATED,
                            path, coordinate(candidate), ""));
                }
            }
        }
        return List.copyOf(result.values());
    }

    private java.util.Optional<ProjectGraphGenerator.DeveloperOutput> developerOutput(
            List<DeveloperGraphExporter.SourceOrigin> origins) {
        return ProjectGraphGenerator.developerOutput(
                        project.getBasedir().toPath(), repositoryUrl, sourceUrlTemplate)
                .map(output -> new ProjectGraphGenerator.DeveloperOutput(
                        output.repositoryRoot(), output.repositoryUrl(), output.sourceUrlTemplate(), origins));
    }

    private static List<String> paths(List<File> roots) {
        return safeFiles(roots).stream().map(File::getPath).toList();
    }

    private static List<File> safeFiles(List<File> roots) {
        return roots == null ? List.of() : roots.stream().filter(Objects::nonNull).toList();
    }

    private static java.util.Optional<Path> moduleDescriptor(List<String> roots) {
        return roots.stream().map(root -> Path.of(root).resolve("module-info.java"))
                .filter(Files::isRegularFile).map(path -> path.toAbsolutePath().normalize())
                .findFirst();
    }

    private String additionalEntryHost(List<MavenProject> projects) {
        if (!"pom".equals(project.getPackaging())
                && projects.stream().anyMatch(candidate -> coordinate(candidate).equals(coordinate(project)))) {
            return coordinate(project);
        }
        return projects.stream().filter(candidate -> !"pom".equals(candidate.getPackaging()))
                .map(AnalyzeReactorMojo::coordinate).findFirst().orElse(coordinate(project));
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
            ApplicationSourceBoundary boundary,
            ProjectGraphGenerator.GenerationResult result) throws IOException {
        if (result.analyses().isEmpty()) return;
        Files.createDirectories(outputDirectory.toPath());
        var decisions = result.analyses().stream()
                .map(analysis -> new RuntimeActivationBundle.DecisionDefinition(
                        analysis.graph(), analysis.manifest()))
                .toList();
        var bundle = new RuntimeActivationBundle(
                boundary.fingerprint(), javaAgentOption(projects),
                ClassFingerprintResolver.resolve(projects, result.analyses()), decisions);
        Files.write(outputDirectory.toPath().resolve("activation.json"), bundle.toJson());
    }

    private static String javaAgentOption(List<MavenProject> projects) {
        for (MavenProject candidate : projects) {
            for (var artifact : candidate.getArtifacts()) {
                if ("at.gepardec.fachtracing".equals(artifact.getGroupId())
                        && "fachtracing-agent".equals(artifact.getArtifactId())
                        && artifact.getFile() != null && artifact.getFile().isFile()) {
                    return "-javaagent:" + artifact.getFile().toPath().toAbsolutePath().normalize();
                }
            }
        }
        for (MavenProject candidate : projects) {
            if (!"at.gepardec.fachtracing".equals(candidate.getGroupId())
                    || !"fachtracing-agent".equals(candidate.getArtifactId())) continue;
            Path jar = Path.of(candidate.getBuild().getDirectory(),
                    candidate.getBuild().getFinalName() + ".jar").toAbsolutePath().normalize();
            if (Files.isRegularFile(jar)) return "-javaagent:" + jar;
        }
        throw new IllegalArgumentException("fachtracing-agent runtime artifact is unavailable; "
                + "add at.gepardec.fachtracing:fachtracing-agent as a runtime dependency");
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

}
