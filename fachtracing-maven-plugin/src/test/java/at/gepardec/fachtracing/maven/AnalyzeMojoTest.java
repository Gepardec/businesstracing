package at.gepardec.fachtracing.maven;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Dependency-free executable contracts for project graph generation. */
public final class AnalyzeMojoTest {
    private static final Path FIXTURES = Path.of("fachtracing-engine/src/test/resources/fixtures");
    private static final List<Path> CLASSPATH = Arrays.stream(System.getProperty("java.class.path")
                    .split(java.io.File.pathSeparator))
            .map(Path::of).toList();

    private AnalyzeMojoTest() { }

    public static void main(String[] args) throws Exception {
        generatesBothFormatsAndIndexWithoutDeletingUnrelatedFiles();
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
                StandardCharsets.UTF_8, output, false);
        assert result.graphCount() == 1 && !result.skipped() : result;
        assert Files.exists(output.resolve("customer-eligibility-structure.mmd"));
        assert Files.exists(output.resolve("customer-eligibility-structure.puml"));
        assert Files.readString(output.resolve("index.md")).contains("customer eligibility");
        assert !Files.exists(output.resolve("old-structure.mmd"));
        assert Files.readString(output.resolve("keep.txt")).equals("application-owned");
    }

    private static void skipsUnannotatedSources() throws Exception {
        Path source = Files.createTempFile("UnannotatedPolicy", ".java");
        Files.writeString(source, "package example; final class UnannotatedPolicy { boolean decide() { return true; } }");
        Path output = Files.createTempDirectory("fachtracing-plugin-skip");
        Files.writeString(output.resolve("old-structure.mmd"), "stale");
        Files.writeString(output.resolve("keep.txt"), "application-owned");
        var result = new ProjectGraphGenerator().generate(
                List.of(source), CLASSPATH, StandardCharsets.UTF_8, output, false);
        assert result.skipped() && result.graphCount() == 0 : result;
        assert !Files.exists(output.resolve("old-structure.mmd"));
        assert Files.readString(output.resolve("keep.txt")).equals("application-owned");
    }

    private static void enforcesStrictIncompleteCoverageAfterWritingArtifacts() throws Exception {
        Path output = Files.createTempDirectory("fachtracing-plugin-strict");
        try {
            new ProjectGraphGenerator().generate(
                    List.of(FIXTURES.resolve("gaps/UnsupportedPolicy.java")), CLASSPATH,
                    StandardCharsets.UTF_8, output, true);
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
}
