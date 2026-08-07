package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.analysis.ApplicationSourceBoundary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/** Dependency-free executable contracts for project graph generation. */
public final class AnalyzeMojoTest {
    private static final Path FIXTURES = Path.of("fachtracing-engine/src/test/resources/fixtures");
    private static final List<Path> CLASSPATH = Arrays.stream(System.getProperty("java.class.path")
                    .split(java.io.File.pathSeparator))
            .map(Path::of).toList();

    private AnalyzeMojoTest() { }

    public static void main(String[] args) throws Exception {
        generatesBothFormatsAndIndexWithoutDeletingUnrelatedFiles();
        generatesParsedDeveloperJsonFromCleanRevision();
        validatesDeveloperOutputSettings();
        resolvesReactorSourcesWithoutGeneratingSiblingEntries();
        preventsAggregateOutputCollisions();
        extractsBoundedSourceArtifactsSafely();
        readsEffectiveCompilerPluginConfiguration();
        preservesSourceTargetCompilerSemantics();
        acceptsCompiledAnnotationProcessorOutputSettings();
        rejectsUnsupportedCompilerPluginConfiguration();
        mapsExternalModuleOwnershipConfiguration();
        skipsSourceEmptyModuleWithReactorSources();
        skipsUnannotatedSources();
        enforcesStrictIncompleteCoverageAfterWritingArtifacts();
        createsSafeDeterministicSlugs();
    }

    private static void mapsExternalModuleOwnershipConfiguration() throws Exception {
        Path root = Files.createTempDirectory("fachtracing-owned-source-config-");
        Path descriptor = Files.writeString(root.resolve("module-info.java"), "module owned.rules { }");
        Path binary = Files.write(root.resolve("owned.rules.jar"), new byte[]{1});
        var origin = new ApplicationSourceBoundary.SourceOrigin(
                ApplicationSourceBoundary.OriginKind.MAVEN_SOURCE,
                "maven:example:owned-rules:1.0", "a".repeat(64));

        var named = new ExternalModuleOwnershipConfiguration();
        named.setIdentity(origin.identity());
        named.setKind("named");
        named.setModuleName("owned.rules");
        named.setDescriptor(descriptor.toFile());
        named.setSourceRoot(root.toFile());
        assert named.matches(origin, root);
        var namedOwnership = named.ownership(root);
        assert namedOwnership.kind() == ApplicationSourceBoundary.ModuleOwnershipKind.NAMED : namedOwnership;
        assert namedOwnership.descriptor().orElseThrow()
                .equals(descriptor.toAbsolutePath().normalize()) : namedOwnership;

        var automatic = new ExternalModuleOwnershipConfiguration();
        automatic.setSourceRoot(root.toFile());
        automatic.setKind("automatic");
        automatic.setModuleName("owned.rules.auto");
        automatic.setBinaryPath(binary.toFile());
        var automaticOwnership = automatic.ownership(root);
        assert automaticOwnership.kind() == ApplicationSourceBoundary.ModuleOwnershipKind.AUTOMATIC
                : automaticOwnership;
        assert automaticOwnership.binaryPath().orElseThrow()
                .equals(binary.toAbsolutePath().normalize()) : automaticOwnership;
    }

    private static void readsEffectiveCompilerPluginConfiguration() throws Exception {
        MavenProject project = compilerProject();
        Path generated = Files.createDirectories(
                project.getBasedir().toPath().resolve("generated/rules"));
        Xpp3Dom shared = configuration();
        add(shared, "encoding", "UTF-8");
        add(shared, "release", "${java.release}");
        add(shared, "parameters", "true");
        add(shared, "generatedSourcesDirectory", generated.toString());
        Xpp3Dom arguments = add(shared, "compilerArgs", null);
        add(arguments, "arg", "-Xlint:none");
        Plugin plugin = compilerPlugin(shared);
        PluginExecution execution = new PluginExecution();
        execution.setId("default-compile");
        Xpp3Dom executionConfiguration = configuration();
        add(executionConfiguration, "enablePreview", "true");
        execution.setConfiguration(executionConfiguration);
        execution.addGoal("compile");
        plugin.addExecution(execution);
        project.getBuild().addPlugin(plugin);

        Path dependency = Files.createTempDirectory("fachtracing-module-dependency");
        var effective = MavenCompilerModelResolver.resolve(project, List.of(
                Path.of(project.getBuild().getOutputDirectory()), dependency), true);

        assert effective.compilerModel().release().equals("21") : effective;
        assert effective.compilerModel().languageVersionMode()
                == ApplicationSourceBoundary.LanguageVersionMode.RELEASE : effective;
        assert effective.compilerModel().languageOptions().equals(List.of("--release", "21")) : effective;
        assert effective.compilerModel().charset().equals(StandardCharsets.UTF_8) : effective;
        assert effective.compilerModel().compilerArguments()
                .equals(List.of("-Xlint:none", "--enable-preview", "-parameters")) : effective;
        assert effective.compilerModel().modulePath().equals(List.of(dependency.toAbsolutePath().normalize()))
                : effective;
        assert effective.compileSourceRoots().contains(generated.toString()) : effective;
        assert MavenCompilerModelResolver.generatedSourceRoots(project).equals(List.of(generated))
                : MavenCompilerModelResolver.generatedSourceRoots(project);
    }

    private static void preservesSourceTargetCompilerSemantics() throws Exception {
        MavenProject project = compilerProject();
        Xpp3Dom configuration = configuration();
        add(configuration, "source", "1.8");
        add(configuration, "target", "1.8");
        project.getBuild().addPlugin(compilerPlugin(configuration));

        var effective = MavenCompilerModelResolver.resolve(project, List.of(), false);

        assert effective.compilerModel().release().equals("8") : effective;
        assert effective.compilerModel().languageVersionMode()
                == ApplicationSourceBoundary.LanguageVersionMode.SOURCE_TARGET : effective;
        assert effective.compilerModel().languageOptions()
                .equals(List.of("-source", "8", "-target", "8")) : effective;
    }

    private static void rejectsUnsupportedCompilerPluginConfiguration() throws Exception {
        MavenProject project = compilerProject();
        Xpp3Dom configuration = configuration();
        add(configuration, "fork", "true");
        project.getBuild().addPlugin(compilerPlugin(configuration));
        try {
            MavenCompilerModelResolver.resolve(project, List.of(), false);
            throw new AssertionError("forked compiler configuration was accepted");
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("forked compiler executables") : expected;
            assert expected.getMessage().contains("example:compiler-fixture") : expected;
        }
    }

    private static void acceptsCompiledAnnotationProcessorOutputSettings() throws Exception {
        MavenProject project = compilerProject();
        Xpp3Dom configuration = configuration();
        add(configuration, "proc", "full");
        Xpp3Dom processorPaths = add(configuration, "annotationProcessorPaths", null);
        add(processorPaths, "path", "example-processor-path");
        Xpp3Dom processors = add(configuration, "annotationProcessors", null);
        add(processors, "annotationProcessor", "example.DecisionProcessor");
        Xpp3Dom arguments = add(configuration, "compilerArgs", null);
        add(arguments, "arg", "-Aexample.option=value");
        add(arguments, "arg", "-processor");
        add(arguments, "arg", "example.DecisionProcessor");
        add(arguments, "arg", "--processor-path=target/processors");
        add(arguments, "arg", "--processor-module-path");
        add(arguments, "arg", "target/processor-modules");
        add(arguments, "arg", "--default-module-for-created-files");
        add(arguments, "arg", "example.generated");
        add(arguments, "arg", "--default-module-for-created-files=example.generated");
        add(arguments, "arg", "-proc:only");
        add(arguments, "arg", "-XprintRounds");
        add(arguments, "arg", "-proc");
        add(arguments, "arg", "-Xlint:none");
        add(arguments, "arg", "-Xlint:none");
        project.getBuild().addPlugin(compilerPlugin(configuration));

        var effective = MavenCompilerModelResolver.resolve(project, List.of(), false);

        assert effective.compilerModel().compilerArguments().equals(List.of("-Xlint:none")) : effective;
    }

    private static MavenProject compilerProject() throws Exception {
        Path base = Files.createTempDirectory("fachtracing-compiler-model");
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("example");
        model.setArtifactId("compiler-fixture");
        model.setVersion("1.0.0");
        Build build = new Build();
        build.setDirectory(base.resolve("target").toString());
        build.setOutputDirectory(base.resolve("target/classes").toString());
        model.setBuild(build);
        MavenProject project = new MavenProject(model);
        project.setFile(base.resolve("pom.xml").toFile());
        project.getProperties().setProperty("java.release", "21");
        project.addCompileSourceRoot(base.resolve("src/main/java").toString());
        return project;
    }

    private static Plugin compilerPlugin(Xpp3Dom configuration) {
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setConfiguration(configuration);
        return plugin;
    }

    private static Xpp3Dom configuration() {
        return new Xpp3Dom("configuration");
    }

    private static Xpp3Dom add(Xpp3Dom parent, String name, String value) {
        Xpp3Dom child = new Xpp3Dom(name);
        child.setValue(value);
        parent.addChild(child);
        return child;
    }

    private static void generatesBothFormatsAndIndexWithoutDeletingUnrelatedFiles() throws Exception {
        Path output = Files.createTempDirectory("fachtracing-plugin-output");
        Files.writeString(output.resolve("old-structure.mmd"), "stale");
        Files.writeString(output.resolve("keep.txt"), "application-owned");
        var result = new ProjectGraphGenerator().generate(
                List.of(FIXTURES.resolve("eligibility/EligibilityPolicy.java")), CLASSPATH,
                StandardCharsets.UTF_8, output, false, Optional.empty());
        assert result.graphCount() == 1 && !result.skipped() : result;
        assert Files.exists(output.resolve("customer-eligibility-structure.mmd"));
        assert Files.exists(output.resolve("customer-eligibility-structure.puml"));
        assert Files.readString(output.resolve("index.md")).contains("customer eligibility");
        assert !Files.exists(output.resolve("old-structure.mmd"));
        assert Files.readString(output.resolve("keep.txt")).equals("application-owned");
    }

    private static void generatesParsedDeveloperJsonFromCleanRevision() throws Exception {
        Path repository = Files.createTempDirectory("fachtracing-plugin-json-source");
        Path source = repository.resolve("src/Policy.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                import at.gepardec.fachtracing.api.FachTracing;
                public final class Policy {
                    @FachTracing("guarded approval")
                    public boolean decide(String value) {
                        try {
                            return Integer.parseInt(value) > 10;
                        } catch (NumberFormatException ignored) {
                            return false;
                        }
                    }
                }
                """, StandardCharsets.UTF_8);
        initializeGitRepository(repository);
        Path output = repository.resolve("target/fachtracing");
        var developerOutput = new ProjectGraphGenerator.DeveloperOutput(
                repository,
                "https://example.invalid/rules",
                "https://example.invalid/rules/blob/{commit}/{path}#L{line}");

        var result = new ProjectGraphGenerator().generate(
                List.of(source), CLASSPATH, StandardCharsets.UTF_8, output, false,
                Optional.of(developerOutput));

        Path jsonFile = output.resolve("guarded-approval-developer.json");
        byte[] jsonBytes = Files.readAllBytes(jsonFile);
        String json = StandardCharsets.UTF_8.newDecoder().decode(
                java.nio.ByteBuffer.wrap(jsonBytes)).toString();
        Map<String, Object> document = object(new JsonParser(json).parse());
        assert document.get("schema").equals("fachtracing-developer-graph/v1") : document;
        Map<String, Object> revision = object(document.get("sourceRevision"));
        String commit = string(revision.get("commit"));
        assert commit.matches("[0-9a-f]{40,64}") : commit;
        Map<String, Object> graph = object(document.get("graph"));
        assert !array(graph.get("nodes")).isEmpty() : graph;
        assert !array(graph.get("edges")).isEmpty() : graph;
        assert !array(graph.get("coverageGaps")).isEmpty() : graph;
        Map<String, Object> sourceNode = array(graph.get("nodes")).stream()
                .map(AnalyzeMojoTest::object)
                .filter(node -> node.containsKey("source"))
                .findFirst().orElseThrow();
        String sourceUrl = string(object(sourceNode.get("source")).get("url"));
        assert sourceUrl.contains("/blob/" + commit + "/src/Policy.java#L") : sourceUrl;
        assert Files.readString(output.resolve("index.md"))
                .contains("[Developer JSON](guarded-approval-developer.json)");
        assert result.graphCount() == 1 && result.incompleteCount() == 1 : result;

        Files.writeString(output.resolve("keep.txt"), "application-owned");
        new ProjectGraphGenerator().generate(
                List.of(source), CLASSPATH, StandardCharsets.UTF_8, output, false, Optional.empty());
        assert !Files.exists(jsonFile) : "stale developer JSON was not removed";
        assert Files.readString(output.resolve("keep.txt")).equals("application-owned");

        Files.writeString(source, Files.readString(source) + "\n", StandardCharsets.UTF_8);
        Path failedOutput = repository.resolve("failed-output");
        try {
            new ProjectGraphGenerator().generate(
                    List.of(source), CLASSPATH, StandardCharsets.UTF_8, failedOutput, false,
                    Optional.of(developerOutput));
            throw new AssertionError("dirty Git state produced developer JSON");
        } catch (IllegalStateException expected) {
            assert expected.getMessage().contains("uncommitted") : expected;
            assert !Files.exists(failedOutput) : "output changed before Git validation";
        }
    }

    private static void validatesDeveloperOutputSettings() {
        Path root = Path.of(".");
        assert ProjectGraphGenerator.developerOutput(root, null, null).isEmpty();
        try {
            ProjectGraphGenerator.developerOutput(root, "https://example.invalid/rules", null);
            throw new AssertionError("partial developer output settings were accepted");
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("fachtracing.repositoryUrl") : expected;
            assert expected.getMessage().contains("fachtracing.sourceUrlTemplate") : expected;
        }
    }

    private static void skipsUnannotatedSources() throws Exception {
        Path source = Files.createTempFile("UnannotatedPolicy", ".java");
        Files.writeString(source, "package example; final class UnannotatedPolicy { boolean decide() { return true; } }");
        Path output = Files.createTempDirectory("fachtracing-plugin-skip");
        Files.writeString(output.resolve("old-structure.mmd"), "stale");
        Files.writeString(output.resolve("keep.txt"), "application-owned");
        var result = new ProjectGraphGenerator().generate(
                List.of(source), CLASSPATH, StandardCharsets.UTF_8, output, false, Optional.empty());
        assert result.skipped() && result.graphCount() == 0 : result;
        assert !Files.exists(output.resolve("old-structure.mmd"));
        assert Files.readString(output.resolve("keep.txt")).equals("application-owned");
    }

    private static void resolvesReactorSourcesWithoutGeneratingSiblingEntries() throws Exception {
        Path entry = FIXTURES.resolve("reactor/DecisionEntry.java");
        List<Path> reactorSources = List.of(
                entry,
                FIXTURES.resolve("reactor/LocalDecisionRule.java"),
                FIXTURES.resolve("reactor/RegionalDecisionRule.java"));
        Path output = Files.createTempDirectory("fachtracing-plugin-reactor");
        var result = new ProjectGraphGenerator().generate(
                List.of(entry), reactorSources, CLASSPATH, StandardCharsets.UTF_8, output, false);
        assert result.graphCount() == 1 && !result.skipped() : result;
        String index = Files.readString(output.resolve("index.md"));
        String diagram = Files.readString(output.resolve("reactor-approval-structure.mmd"));
        assert index.contains("reactor approval") && !index.contains("sibling entry") : index;
        assert diagram.contains("local decision rule") && diagram.contains("regional decision rule") : diagram;
        assert !diagram.matches("(?s).*candidate [0-9]+.*") : diagram;
    }

    private static void preventsAggregateOutputCollisions() throws Exception {
        Path sources = Files.createTempDirectory("fachtracing-collision-sources");
        Path first = sources.resolve("FirstPolicy.java");
        Path second = sources.resolve("SecondPolicy.java");
        Files.writeString(first, """
                import at.gepardec.fachtracing.api.FachTracing;
                final class FirstPolicy {
                    @FachTracing("same decision") boolean decide(int age) { return age >= 18; }
                }
                """);
        Files.writeString(second, """
                import at.gepardec.fachtracing.api.FachTracing;
                final class SecondPolicy {
                    @FachTracing("same decision") boolean decide(int score) { return score >= 10; }
                }
                """);
        Path output = Files.createTempDirectory("fachtracing-collision-output");
        var result = new ProjectGraphGenerator().generate(
                List.of(first, second), CLASSPATH, StandardCharsets.UTF_8,
                output, false, Optional.empty());
        try (var files = Files.list(output)) {
            long diagrams = files.filter(path -> path.getFileName().toString()
                    .matches("same-decision-[0-9a-f]{8}-structure\\.mmd")).count();
            assert diagrams == 2 : output;
        }
        assert result.graphCount() == 2 : result;
    }

    private static void skipsSourceEmptyModuleWithReactorSources() throws Exception {
        Path output = Files.createTempDirectory("fachtracing-plugin-empty-reactor-module");
        Files.writeString(output.resolve("old-structure.mmd"), "stale");
        var result = new ProjectGraphGenerator().generate(
                List.of(), List.of(FIXTURES.resolve("reactor/DecisionEntry.java")), CLASSPATH,
                StandardCharsets.UTF_8, output, false);
        assert result.skipped() && result.graphCount() == 0 : result;
        assert !Files.exists(output.resolve("old-structure.mmd"));
    }

    private static void extractsBoundedSourceArtifactsSafely() throws Exception {
        var limits = new SourceInputResolver.ArchiveLimits(4, 128, 256);
        Path safeArchive = sourceArchive(Map.of(
                "example/Policy.java", "package example; final class Policy {}",
                "META-INF/notice.txt", "notice"));
        Path safeOutput = Files.createTempDirectory("fachtracing-safe-source-archive");
        List<Path> sources = SourceInputResolver.extract(safeArchive, safeOutput, limits);
        assert sources.size() == 1 : sources;
        assert sources.getFirst().startsWith(safeOutput) : sources;
        assert Files.readString(sources.getFirst()).contains("class Policy");

        for (String unsafe : List.of("../escape.java", "/absolute.java", "folder\\escape.java")) {
            Path archive = sourceArchive(Map.of(unsafe, "final class Escape {}"));
            try {
                SourceInputResolver.extract(archive,
                        Files.createTempDirectory("fachtracing-unsafe-source-archive"), limits);
                throw new AssertionError("unsafe archive entry was accepted: " + unsafe);
            } catch (IllegalArgumentException expected) {
                assert expected.getMessage().contains("archive entry") : expected.getMessage();
            }
        }

        Path tooLarge = sourceArchive(Map.of("Large.java", "x".repeat(129)));
        try {
            SourceInputResolver.extract(tooLarge,
                    Files.createTempDirectory("fachtracing-large-source-archive"), limits);
            throw new AssertionError("oversized archive entry was accepted");
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("too large")
                    || expected.getMessage().contains("configured size") : expected.getMessage();
        }
    }

    private static Path sourceArchive(Map<String, String> entries) throws IOException {
        Path archive = Files.createTempFile("fachtracing-source-input", ".jar");
        try (var output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return archive;
    }

    private static void enforcesStrictIncompleteCoverageAfterWritingArtifacts() throws Exception {
        Path output = Files.createTempDirectory("fachtracing-plugin-strict");
        try {
            new ProjectGraphGenerator().generate(
                    List.of(FIXTURES.resolve("gaps/UnsupportedPolicy.java")), CLASSPATH,
                    StandardCharsets.UTF_8, output, true, Optional.empty());
            throw new AssertionError("strict mode accepted incomplete graph");
        } catch (ProjectGraphGenerator.IncompleteGraphException expected) {
            assert expected.decisions().equals(List.of("guarded approval")) : expected.decisions();
            assert Files.exists(output.resolve("guarded-approval-structure.mmd"));
            assert Files.exists(output.resolve("index.md"));
        }
    }

    private static void createsSafeDeterministicSlugs() {
        assert ProjectGraphGenerator.slug("Customer eligibility!").equals("customer-eligibility");
        assert ProjectGraphGenerator.slug("***").equals("decision");
        assert ProjectGraphGenerator.slug("Prüfung Österreich").equals("prüfung-österreich");
    }

    private static void initializeGitRepository(Path repository) throws IOException {
        git(repository, "init", "-q");
        git(repository, "config", "user.name", "Fachtracing Test");
        git(repository, "config", "user.email", "fachtracing@example.invalid");
        git(repository, "config", "commit.gpgsign", "false");
        git(repository, "add", ".");
        git(repository, "commit", "-q", "-m", "fixture");
    }

    private static void git(Path repository, String... arguments) throws IOException {
        var command = new ArrayList<String>();
        command.add("git");
        command.add("-C");
        command.add(repository.toString());
        command.addAll(List.of(arguments));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) throw new IOException("git fixture command failed: " + output);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("git fixture command interrupted", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value) {
        return (List<Object>) value;
    }

    private static String string(Object value) {
        return (String) value;
    }

    /** A separate test-only parser for the complete public JSON document. */
    private static final class JsonParser {
        private final String input;
        private int index;

        private JsonParser(String input) {
            this.input = input;
        }

        Object parse() {
            Object value = value();
            whitespace();
            if (index != input.length()) throw error("unexpected trailing content");
            return value;
        }

        private Object value() {
            whitespace();
            if (index >= input.length()) throw error("expected value");
            return switch (input.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            expect('{');
            var values = new LinkedHashMap<String, Object>();
            whitespace();
            if (take('}')) return values;
            do {
                whitespace();
                String name = string();
                whitespace();
                expect(':');
                values.put(name, value());
                whitespace();
            } while (take(','));
            expect('}');
            return values;
        }

        private List<Object> array() {
            expect('[');
            var values = new ArrayList<>();
            whitespace();
            if (take(']')) return values;
            do {
                values.add(value());
                whitespace();
            } while (take(','));
            expect(']');
            return values;
        }

        private String string() {
            expect('"');
            var value = new StringBuilder();
            while (index < input.length()) {
                char character = input.charAt(index++);
                if (character == '"') return value.toString();
                if (character != '\\') {
                    if (character < 0x20) throw error("unescaped control character");
                    value.append(character);
                    continue;
                }
                if (index >= input.length()) throw error("incomplete escape");
                char escape = input.charAt(index++);
                switch (escape) {
                    case '"', '\\', '/' -> value.append(escape);
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append((char) Integer.parseInt(take(4), 16));
                    default -> throw error("invalid escape");
                }
            }
            throw error("unterminated string");
        }

        private Object number() {
            int start = index;
            if (take('-')) { /* optional sign */ }
            while (index < input.length() && Character.isDigit(input.charAt(index))) index++;
            if (start == index) throw error("expected number");
            return Long.parseLong(input.substring(start, index));
        }

        private Object literal(String text, Object value) {
            if (!input.startsWith(text, index)) throw error("invalid literal");
            index += text.length();
            return value;
        }

        private String take(int count) {
            if (index + count > input.length()) throw error("incomplete escape");
            String value = input.substring(index, index + count);
            index += count;
            return value;
        }

        private boolean take(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw error("expected '" + expected + "'");
        }

        private void whitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at JSON offset " + index);
        }
    }
}
