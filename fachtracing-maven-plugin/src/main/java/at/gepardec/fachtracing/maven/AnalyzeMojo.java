package at.gepardec.fachtracing.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    @Override public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Fachtracing analysis skipped");
            return;
        }
        String module = project.getArtifactId() == null ? "current module" : project.getArtifactId();
        try {
            List<Path> rootSources = sourceFiles(project.getCompileSourceRoots());
            Charset charset = encoding == null || encoding.isBlank()
                    ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            var generator = new ProjectGraphGenerator();
            ProjectGraphGenerator.GenerationResult result;
            if (rootSources.isEmpty()) {
                result = generator.generate(
                        rootSources, rootSources, List.of(), charset, outputDirectory.toPath(),
                        failOnIncomplete, developerOutput());
            } else {
                List<MavenProject> analysisProjects = analysisProjects();
                List<Path> sources = sourceFiles(analysisProjects.stream()
                        .flatMap(candidate -> candidate.getCompileSourceRoots().stream()).toList());
                List<Path> classpath = compileClasspath(analysisProjects);
                result = generator.generate(
                        rootSources, sources, classpath, charset, outputDirectory.toPath(),
                        failOnIncomplete, developerOutput());
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

    private Optional<ProjectGraphGenerator.DeveloperOutput> developerOutput() {
        return ProjectGraphGenerator.developerOutput(
                project.getBasedir().toPath(), repositoryUrl, sourceUrlTemplate);
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
