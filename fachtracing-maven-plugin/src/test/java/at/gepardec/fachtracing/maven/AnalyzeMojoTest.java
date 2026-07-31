package at.gepardec.fachtracing.maven;

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
        skipsUnannotatedSources();
        enforcesStrictIncompleteCoverageAfterWritingArtifacts();
        createsSafeDeterministicSlugs();
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
