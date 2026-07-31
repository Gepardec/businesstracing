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

/** Generates static business-decision diagrams for the current Maven module. */
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public final class AnalyzeMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/fachtracing",
            property = "fachtracing.outputDirectory", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.sourceEncoding}", property = "fachtracing.encoding")
    private String encoding;

    @Parameter(defaultValue = "false", property = "fachtracing.failOnIncomplete")
    private boolean failOnIncomplete;

    @Parameter(defaultValue = "false", property = "fachtracing.skip")
    private boolean skip;

    @Override public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Fachtracing analysis skipped");
            return;
        }
        String module = project.getArtifactId() == null ? "current module" : project.getArtifactId();
        try {
            List<Path> sources = sourceFiles(project.getCompileSourceRoots());
            List<Path> classpath = project.getCompileClasspathElements().stream().map(Path::of).toList();
            Charset charset = encoding == null || encoding.isBlank()
                    ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            var result = new ProjectGraphGenerator().generate(
                    sources, classpath, charset, outputDirectory.toPath(), failOnIncomplete);
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
        } catch (IllegalArgumentException error) {
            throw new MojoFailureException("Could not analyze " + module + ": " + error.getMessage(), error);
        } catch (IOException error) {
            throw new MojoExecutionException("Could not write Fachtracing output for " + module, error);
        }
    }

    private static List<Path> sourceFiles(List<String> roots) throws IOException {
        var sources = new ArrayList<Path>();
        for (String root : roots) {
            Path directory = Path.of(root);
            if (!Files.isDirectory(directory)) continue;
            try (var files = Files.walk(directory)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(sources::add);
            }
        }
        sources.sort(Comparator.comparing(path -> path.toAbsolutePath().normalize().toString()));
        return List.copyOf(sources);
    }
}
