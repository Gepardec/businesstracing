package at.gepardec.fachtracing.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.repository.RepositorySystem;

import at.gepardec.fachtracing.developer.DeveloperGraphExporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/** Generates static decision graphs and optional developer JSON for the current Maven module. */
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public final class AnalyzeMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${project.build.directory}/fachtracing",
            property = "fachtracing.outputDirectory", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.sourceEncoding}", property = "fachtracing.encoding")
    private String encoding;

    @Parameter(defaultValue = "false", property = "fachtracing.failOnIncomplete")
    private boolean failOnIncomplete;

    @Parameter(defaultValue = "false", property = "fachtracing.skip")
    private boolean skip;

    /** Browser-facing repository URL stored in developer JSON. */
    @Parameter(property = "fachtracing.repositoryUrl")
    private String repositoryUrl;

    /** Commit-pinned source URL template with commit and path placeholders. */
    @Parameter(property = "fachtracing.sourceUrlTemplate")
    private String sourceUrlTemplate;

    /** Local Java source directories used only to resolve reachable calls and implementations. */
    @Parameter
    private List<File> additionalSourceRoots;

    /** Local Java source directories that can also contain annotated graph entries. */
    @Parameter
    private List<File> additionalEntrySourceRoots;

    /** Exact groupId:artifactId:version coordinates whose sources classifiers are resolution-only. */
    @Parameter
    private List<String> sourceDependencies;

    @Parameter(defaultValue = "${project.build.directory}/fachtracing-source-dependencies",
            property = "fachtracing.sourceExtractionDirectory")
    private File sourceExtractionDirectory;

    @Parameter(defaultValue = "10000", property = "fachtracing.sourceArchiveMaxEntries")
    private int sourceArchiveMaxEntries;

    @Parameter(defaultValue = "4194304", property = "fachtracing.sourceArchiveMaxEntryBytes")
    private long sourceArchiveMaxEntryBytes;

    @Parameter(defaultValue = "134217728", property = "fachtracing.sourceArchiveMaxTotalBytes")
    private long sourceArchiveMaxTotalBytes;

    @Override public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Fachtracing analysis skipped");
            return;
        }
        String module = project.getArtifactId() == null ? "current module" : project.getArtifactId();
        try {
            List<Path> rootSources = sourceFiles(concat(
                    project.getCompileSourceRoots(), paths(additionalEntrySourceRoots)));
            Charset charset = encoding == null || encoding.isBlank()
                    ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            var generator = new ProjectGraphGenerator();
            ProjectGraphGenerator.GenerationResult result;
            if (rootSources.isEmpty()) {
                result = generator.generate(
                        rootSources, rootSources, List.of(), charset, outputDirectory.toPath(),
                        failOnIncomplete, developerOutput(List.of()));
            } else {
                List<MavenProject> analysisProjects = analysisProjects();
                var sourceRoots = new ArrayList<String>(analysisProjects.stream()
                        .flatMap(candidate -> candidate.getCompileSourceRoots().stream()).toList());
                sourceRoots.addAll(paths(additionalSourceRoots));
                sourceRoots.addAll(paths(additionalEntrySourceRoots));
                var artifacts = resolveSourceDependencies();
                artifacts.stream().map(item -> item.root().toString()).forEach(sourceRoots::add);
                List<Path> sources = sourceFiles(sourceRoots);
                List<Path> classpath = compileClasspath(analysisProjects);
                result = generator.generate(
                        rootSources, sources, classpath, charset, outputDirectory.toPath(),
                        failOnIncomplete, developerOutput(sourceOrigins(artifacts)));
            }
            if (result.skipped()) {
                getLog().info("No @FachTracing decision found in " + module + "; skipping");
                return;
            }
            if (result.incompleteCount() > 0) {
                getLog().warn(result.incompleteCount() + " of " + result.graphCount()
                        + " decision graph(s) have incomplete static coverage");
            }
            getLog().info("Generated " + result.graphCount() + " Fachtracing decision graph(s) in "
                    + outputDirectory.toPath().toAbsolutePath().normalize());
        } catch (ProjectGraphGenerator.IncompleteGraphException error) {
            throw new MojoFailureException(error.getMessage()
                    + ". Review the generated diagrams or set -Dfachtracing.failOnIncomplete=false.", error);
        } catch (DependencyResolutionRequiredException error) {
            throw new MojoExecutionException("Could not resolve the compile classpath for " + module, error);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new MojoFailureException("Could not analyze " + module + ": " + error.getMessage(), error);
        } catch (IOException error) {
            throw new MojoExecutionException("Could not write Fachtracing output for " + module, error);
        }
    }

    private Optional<ProjectGraphGenerator.DeveloperOutput> developerOutput(
            List<DeveloperGraphExporter.SourceOrigin> origins) {
        Optional<ProjectGraphGenerator.DeveloperOutput> configured = ProjectGraphGenerator.developerOutput(
                project.getBasedir().toPath(), repositoryUrl, sourceUrlTemplate);
        return configured.map(output -> new ProjectGraphGenerator.DeveloperOutput(
                output.repositoryRoot(), output.repositoryUrl(), output.sourceUrlTemplate(), origins));
    }

    private List<DeveloperGraphExporter.SourceOrigin> sourceOrigins(
            List<SourceInputResolver.ResolvedSourceArtifact> artifacts) {
        var byRoot = new LinkedHashMap<Path, DeveloperGraphExporter.SourceOrigin>();
        int index = 0;
        for (File root : safeFiles(additionalSourceRoots)) {
            Path path = root.toPath().toAbsolutePath().normalize();
            byRoot.putIfAbsent(path, DeveloperGraphExporter.SourceOrigin.external(
                    "local-" + (++index), DeveloperGraphExporter.OriginKind.LOCAL,
                    path, "configured local resolution source", ""));
        }
        for (File root : safeFiles(additionalEntrySourceRoots)) {
            Path path = root.toPath().toAbsolutePath().normalize();
            byRoot.putIfAbsent(path, DeveloperGraphExporter.SourceOrigin.external(
                    "entry-" + (++index), DeveloperGraphExporter.OriginKind.LOCAL,
                    path, "configured local entry source", ""));
        }
        for (SourceInputResolver.ResolvedSourceArtifact artifact : artifacts) {
            byRoot.put(artifact.root(), DeveloperGraphExporter.SourceOrigin.external(
                    "maven-" + (++index), DeveloperGraphExporter.OriginKind.MAVEN_SOURCE,
                    artifact.root(), artifact.coordinate(), artifact.checksum()));
        }
        Path build = Path.of(project.getBuild().getDirectory()).toAbsolutePath().normalize();
        for (String root : project.getCompileSourceRoots()) {
            Path path = Path.of(root).toAbsolutePath().normalize();
            if (path.startsWith(build) && Files.isDirectory(path)) {
                byRoot.putIfAbsent(path, DeveloperGraphExporter.SourceOrigin.external(
                        "generated-" + (++index), DeveloperGraphExporter.OriginKind.GENERATED,
                        path, project.getGroupId() + ':' + project.getArtifactId(), ""));
            }
        }
        return List.copyOf(byRoot.values());
    }

    private static List<File> safeFiles(List<File> roots) {
        return roots == null ? List.of() : roots.stream().filter(Objects::nonNull).toList();
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

    private static List<String> paths(List<File> roots) {
        if (roots == null) return List.of();
        return roots.stream().filter(Objects::nonNull).map(File::getPath).toList();
    }

    private static List<String> concat(List<String> first, List<String> second) {
        var result = new ArrayList<String>(first);
        result.addAll(second);
        return List.copyOf(result);
    }

    private static List<Path> sourceFiles(List<String> roots) throws IOException {
        var sources = new TreeSet<Path>(Comparator.comparing(Path::toString));
        for (String root : roots) {
            Path directory = Path.of(root);
            if (!Files.isDirectory(directory)) continue;
            try (var files = Files.walk(directory)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !path.getFileName().toString().equals("module-info.java"))
                        .map(path -> path.toAbsolutePath().normalize())
                        .forEach(sources::add);
            }
        }
        return List.copyOf(sources);
    }

    private List<MavenProject> analysisProjects() {
        if (reactorProjects == null || reactorProjects.isEmpty()) return List.of(project);
        if (reactorProjects.contains(project)) return List.copyOf(reactorProjects);
        var projects = new ArrayList<>(reactorProjects);
        projects.add(project);
        return List.copyOf(projects);
    }

    private static List<Path> compileClasspath(List<MavenProject> projects)
            throws DependencyResolutionRequiredException {
        var classpath = new TreeSet<Path>(Comparator.comparing(Path::toString));
        for (MavenProject candidate : projects) {
            candidate.getCompileClasspathElements().stream()
                    .map(Path::of)
                    .map(path -> path.toAbsolutePath().normalize())
                    .forEach(classpath::add);
        }
        return List.copyOf(classpath);
    }
}
