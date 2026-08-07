package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.analysis.ApplicationSourceBoundary;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Reads the effective Maven compiler settings that are safe for static attribution. */
final class MavenCompilerModelResolver {
    private static final String COMPILER_KEY = "org.apache.maven.plugins:maven-compiler-plugin";

    private MavenCompilerModelResolver() { }

    static EffectiveCompilerModel resolve(
            MavenProject project,
            List<Path> compileClasspath,
            boolean modular) {
        Objects.requireNonNull(project, "project");
        Xpp3Dom configuration = compilerConfiguration(project);
        rejectUnsupportedConfiguration(project, configuration);

        String encodingText = first(value(configuration, "encoding"),
                property(project, "project.build.sourceEncoding"), StandardCharsets.UTF_8.name());
        Charset charset = Charset.forName(interpolate(project, encodingText));
        LanguageLevel languageLevel = languageLevel(project, configuration);

        var configuredArguments = new ArrayList<String>();
        Xpp3Dom compilerArgs = child(configuration, "compilerArgs");
        if (compilerArgs != null) {
            for (Xpp3Dom argument : compilerArgs.getChildren("arg")) {
                String text = interpolate(project, argument.getValue());
                if (text != null && !text.isBlank()) configuredArguments.add(text.trim());
            }
        }
        if (Boolean.parseBoolean(first(value(configuration, "enablePreview"),
                property(project, "maven.compiler.enablePreview"), "false"))) {
            configuredArguments.add("--enable-preview");
        }
        if (Boolean.parseBoolean(first(value(configuration, "parameters"),
                property(project, "maven.compiler.parameters"), "false"))) {
            configuredArguments.add("-parameters");
        }
        List<String> arguments = analysisCompilerArguments(project, configuredArguments);

        List<Path> modulePath = modular ? modulePath(project, compileClasspath) : List.of();
        List<String> sourceRoots = new ArrayList<>(project.getCompileSourceRoots());
        for (Path path : generatedSourceRoots(project, configuration)) {
            if (sourceRoots.stream().map(Path::of).map(MavenCompilerModelResolver::normalize)
                    .noneMatch(path::equals)) {
                sourceRoots.add(path.toString());
            }
        }
        return new EffectiveCompilerModel(
                new ApplicationSourceBoundary.CompilerModel(
                        charset, languageLevel.version(), List.copyOf(arguments), modulePath,
                        languageLevel.mode()),
                List.copyOf(sourceRoots));
    }

    private static Xpp3Dom compilerConfiguration(MavenProject project) {
        Plugin compiler = project.getPlugin(COMPILER_KEY);
        if (compiler == null) return null;
        Xpp3Dom shared = copy(compiler.getConfiguration());
        PluginExecution compile = compiler.getExecutions().stream()
                .filter(execution -> "default-compile".equals(execution.getId())
                        || execution.getGoals().contains("compile"))
                .findFirst().orElse(null);
        if (compile == null) return shared;
        Xpp3Dom execution = copy(compile.getConfiguration());
        if (execution == null) return shared;
        return Xpp3Dom.mergeXpp3Dom(execution, shared);
    }

    private static void rejectUnsupportedConfiguration(MavenProject project, Xpp3Dom configuration) {
        if (Boolean.parseBoolean(first(value(configuration, "fork"),
                property(project, "maven.compiler.fork"), "false"))
                || hasText(value(configuration, "executable"))) {
            reject(project, "forked compiler executables are not supported");
        }
        if (hasText(value(configuration, "compilerArgument"))
                || child(configuration, "compilerArguments") != null) {
            reject(project, "legacy compilerArgument settings are not supported; use compilerArgs/arg");
        }
    }

    private static List<String> analysisCompilerArguments(
            MavenProject project,
            List<String> configuredArguments) {
        var arguments = new LinkedHashSet<String>();
        boolean skipProcessorValue = false;
        for (String argument : configuredArguments) {
            if (skipProcessorValue) {
                skipProcessorValue = false;
                continue;
            }
            if (processorArgumentWithSeparateValue(argument)) {
                skipProcessorValue = true;
                continue;
            }
            if (processorArgument(argument)) continue;
            validateCompilerArgument(project, argument);
            arguments.add(argument);
        }
        return List.copyOf(arguments);
    }

    private static boolean processorArgumentWithSeparateValue(String argument) {
        return List.of("-processor", "--processor", "-processorpath",
                "--processor-path", "--processor-module-path",
                "--default-module-for-created-files").contains(argument);
    }

    private static boolean processorArgument(String argument) {
        if (argument.startsWith("-A") || argument.equals("-proc") || argument.equals("--proc")
                || argument.startsWith("-proc:")
                || argument.equals("-XprintProcessorInfo") || argument.equals("-XprintRounds")) {
            return true;
        }
        return List.of("-proc", "--proc", "-processor", "--processor", "-processorpath",
                        "--processor-path", "--processor-module-path",
                        "--default-module-for-created-files").stream()
                .anyMatch(option -> argument.startsWith(option + "="));
    }

    static List<Path> generatedSourceRoots(MavenProject project) {
        Objects.requireNonNull(project, "project");
        return generatedSourceRoots(project, compilerConfiguration(project));
    }

    private static List<Path> generatedSourceRoots(
            MavenProject project,
            Xpp3Dom configuration) {
        var roots = new LinkedHashSet<Path>();
        if (project.getBuild() != null && hasText(project.getBuild().getDirectory())) {
            Path build = normalize(Path.of(project.getBuild().getDirectory()));
            project.getCompileSourceRoots().stream().map(Path::of)
                    .map(MavenCompilerModelResolver::normalize)
                    .filter(path -> path.startsWith(build))
                    .forEach(roots::add);
        }
        String generated = interpolate(project, value(configuration, "generatedSourcesDirectory"));
        if (hasText(generated)) roots.add(resolvePath(project, generated));
        return List.copyOf(roots);
    }

    private static LanguageLevel languageLevel(MavenProject project, Xpp3Dom configuration) {
        String release = first(value(configuration, "release"),
                property(project, "maven.compiler.release"), null);
        if (hasText(release)) {
            return new LanguageLevel(normalizeVersion(interpolate(project, release)),
                    ApplicationSourceBoundary.LanguageVersionMode.RELEASE);
        }
        String source = first(value(configuration, "source"), property(project, "maven.compiler.source"), null);
        String target = first(value(configuration, "target"), property(project, "maven.compiler.target"), null);
        source = normalizeVersion(interpolate(project, source));
        target = normalizeVersion(interpolate(project, target));
        if (hasText(source) && hasText(target) && !source.equals(target)) {
            reject(project, "source and target differ (" + source + " and " + target + ")");
        }
        String version = first(target, source, null);
        if (hasText(version)) {
            return new LanguageLevel(version, ApplicationSourceBoundary.LanguageVersionMode.SOURCE_TARGET);
        }
        return new LanguageLevel(Integer.toString(Runtime.version().feature()),
                ApplicationSourceBoundary.LanguageVersionMode.RELEASE);
    }

    private static void validateCompilerArgument(MavenProject project, String argument) {
        for (String prefix : List.of("-proc", "-processor", "--processor", "-A", "-source", "--source",
                "-target", "--target", "--release", "-classpath", "--class-path", "--module-path",
                "-p", "--module-source-path", "--patch-module", "-sourcepath", "--source-path",
                "-d", "-s", "-encoding")) {
            if (argument.equals(prefix) || argument.startsWith(prefix + "=")
                    || (prefix.length() > 2 && argument.startsWith(prefix))) {
                reject(project, "compiler argument is controlled by the analysis model: " + argument);
            }
        }
    }

    private static List<Path> modulePath(MavenProject project, List<Path> classpath) {
        Path ownOutput = project.getBuild() == null || project.getBuild().getOutputDirectory() == null
                ? null : normalize(Path.of(project.getBuild().getOutputDirectory()));
        return classpath.stream().map(MavenCompilerModelResolver::normalize)
                .filter(path -> ownOutput == null || !path.equals(ownOutput))
                .distinct().sorted(java.util.Comparator.comparing(Path::toString)).toList();
    }

    private static Path resolvePath(MavenProject project, String value) {
        Path path = Path.of(value);
        if (!path.isAbsolute()) path = project.getBasedir().toPath().resolve(path);
        return normalize(path);
    }

    private static String interpolate(MavenProject project, String value) {
        if (value == null) return null;
        String result = value;
        for (String name : project.getProperties().stringPropertyNames()) {
            result = result.replace("${" + name + "}", project.getProperties().getProperty(name));
        }
        result = result.replace("${project.basedir}", project.getBasedir().getAbsolutePath())
                .replace("${basedir}", project.getBasedir().getAbsolutePath());
        if (result.contains("${")) reject(project, "compiler setting is not resolved: " + result);
        return result;
    }

    private static String property(MavenProject project, String name) {
        return project.getProperties().getProperty(name);
    }

    private static String value(Xpp3Dom configuration, String name) {
        Xpp3Dom node = child(configuration, name);
        return node == null ? null : node.getValue();
    }

    private static Xpp3Dom child(Xpp3Dom configuration, String name) {
        return configuration == null ? null : configuration.getChild(name);
    }

    private static Xpp3Dom copy(Object configuration) {
        return configuration instanceof Xpp3Dom dom ? new Xpp3Dom(dom) : null;
    }

    private static String first(String first, String second, String fallback) {
        if (hasText(first)) return first.trim();
        if (hasText(second)) return second.trim();
        return fallback;
    }

    private static String normalizeVersion(String value) {
        if (!hasText(value)) return null;
        String normalized = value.trim();
        return normalized.startsWith("1.") ? normalized.substring(2) : normalized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static void reject(MavenProject project, String reason) {
        throw new IllegalArgumentException("unsupported effective compiler model for "
                + project.getGroupId() + ':' + project.getArtifactId() + ": " + reason);
    }

    record EffectiveCompilerModel(
            ApplicationSourceBoundary.CompilerModel compilerModel,
            List<String> compileSourceRoots) {
        EffectiveCompilerModel {
            compilerModel = Objects.requireNonNull(compilerModel, "compilerModel");
            compileSourceRoots = List.copyOf(compileSourceRoots);
        }
    }

    private record LanguageLevel(
            String version,
            ApplicationSourceBoundary.LanguageVersionMode mode) { }
}
