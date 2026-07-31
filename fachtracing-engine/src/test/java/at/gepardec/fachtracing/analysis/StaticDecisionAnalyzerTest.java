package at.gepardec.fachtracing.analysis;

import at.gepardec.fachtracing.developer.DeveloperGraphExporter;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Executable dependency-free contract tests for the static analyzer. */
public final class StaticDecisionAnalyzerTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path FIXTURES = ROOT.resolve("fachtracing-engine/src/test/resources/fixtures");
    private static final List<Path> CLASSPATH = List.of(ROOT.resolve("fachtracing-api/target/classes"));

    private StaticDecisionAnalyzerTest() { }

    public static void main(String[] args) {
        supportedConstructsAcrossDomains();
        excludesResultIndependentWork();
        followsDirectCallsAcrossDomains();
        representsDynamicDispatchWithoutGuessing();
        exposesRelevantCoverageGaps();
        sourceUnavailableDecisionLogicIsNeverReportedComplete();
        analyzesEveryAnnotatedEntry();
        treatsPlatformValueOperationsAsDecisionFacts();
        supportsCollectionFactsAndRecordEquality();
        followsStrategiesThatMutateReturnedCollectionsInsideLambdas();
        streamPredicatesStayBusinessFacing();
        usesOneBusinessStartAndStopWithExplicitReturns();
        removesIdentifierAndNullImplementationVocabulary();
        exportsDeveloperGraphWithRevisionPinnedSourceLinks();
        capturesOnlyCleanGitRevisions();
        rejectsSourceMissingFromCapturedCommit();
    }

    private static void supportedConstructsAcrossDomains() {
        for (var fixture : List.of("eligibility/EligibilityPolicy.java", "pricing/PricingPolicy.java")) {
            var graph = analyze(fixture).graph();
            assert graph.completeness() == BusinessDecisionGraph.Completeness.COMPLETE : fixture;
            assert hasKind(graph, BusinessDecisionGraph.NodeKind.ENTRY) : fixture;
            assert hasKind(graph, BusinessDecisionGraph.NodeKind.PREDICATE) : fixture;
            assert hasKind(graph, BusinessDecisionGraph.NodeKind.OUTCOME) : fixture;
        }
    }

    private static void excludesResultIndependentWork() {
        var result = analyze("eligibility/EligibilityPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE;
        assert hasKind(result.graph(), BusinessDecisionGraph.NodeKind.PREDICATE);
        assert hasKind(result.graph(), BusinessDecisionGraph.NodeKind.OUTCOME);
        var predicate = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .findFirst().orElseThrow();
        var branchOutcomes = result.graph().edges().stream()
                .filter(edge -> edge.fromNodeId().equals(predicate.nodeId()))
                .map(BusinessDecisionGraph.DecisionEdge::outcome).toList();
        assert branchOutcomes.stream().anyMatch(value -> value.startsWith("true; returns "))
                && branchOutcomes.stream().anyMatch(value -> value.startsWith("false; returns "))
                : result.graph().edges();
        assert result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME)
                .noneMatch(outcome -> result.graph().edges().stream()
                        .anyMatch(edge -> edge.fromNodeId().equals(outcome.nodeId()))) : result.graph().edges();
        String labels = result.graph().nodes().toString();
        assert !labels.contains("diagnosticOnly") : labels;
        assert !labels.contains("System.getProperty") : labels;
        result.graph().nodes().forEach(node -> {
            assert !node.businessLabel().contains("(") : node;
            assert !node.businessLabel().contains(")") : node;
            assert !node.businessLabel().contains(".equals") : node;
        });
    }

    private static void followsDirectCallsAcrossDomains() {
        var result = analyze("pricing/PricingPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE;
        long predicates = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .count();
        assert predicates >= 1 : result.graph().nodes();
        assert result.graph().nodes().stream()
                .anyMatch(node -> node.businessLabel().contains("preferred")) : result.graph().nodes();
        assert result.graph().nodes().stream()
                .anyMatch(node -> node.businessLabel().equals("evaluate fixed adjustment"))
                : "single-return arithmetic helper was mistaken for a projection: " + result.graph().nodes();
        var outcome = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME)
                .findFirst().orElseThrow();
        var resultPaths = result.graph().edges().stream()
                .filter(edge -> edge.toNodeId().equals(outcome.nodeId()))
                .map(BusinessDecisionGraph.DecisionEdge::outcome).toList();
        assert resultPaths.size() == 2
                && resultPaths.stream().anyMatch(value -> value.startsWith("true; returns "))
                && resultPaths.stream().anyMatch(value -> value.startsWith("false; returns "))
                : result.graph().edges();
    }

    private static void representsDynamicDispatchWithoutGuessing() {
        var result = analyze("strategy/StrategyDecisionService.java");
        var dispatch = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.DISPATCH)
                .findFirst().orElseThrow();
        long alternatives = result.graph().edges().stream()
                .filter(edge -> edge.fromNodeId().equals(dispatch.nodeId()))
                .filter(edge -> edge.outcome().startsWith("candidate "))
                .count();
        assert alternatives == 2 : result.graph().edges();
        long implementationPredicates = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .count();
        assert implementationPredicates == 2 : result.graph().nodes();
    }

    private static void exposesRelevantCoverageGaps() {
        var loopResult = analyze("gaps/AggregatingPolicy.java");
        assert loopResult.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                : loopResult.diagnostics();
        assert hasKind(loopResult.graph(), BusinessDecisionGraph.NodeKind.CHOICE) : loopResult.graph().nodes();

        var result = analyze("gaps/UnsupportedPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE;
        assert hasKind(result.graph(), BusinessDecisionGraph.NodeKind.COVERAGE_GAP);
        assert !result.diagnostics().isEmpty();
        assert result.diagnostics().getFirst().line() > 0;
    }

    private static void sourceUnavailableDecisionLogicIsNeverReportedComplete() {
        var result = analyze("gaps/ExternalDecisionPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE;
        assert result.graph().coverageGaps().stream()
                .anyMatch(gap -> gap.description().contains("implementations are unavailable"));
    }

    private static void analyzesEveryAnnotatedEntry() {
        var results = new StaticDecisionAnalyzer().analyzeAll(AnalysisRequest.of(
                List.of(
                        FIXTURES.resolve("eligibility/EligibilityPolicy.java"),
                        FIXTURES.resolve("pricing/PricingPolicy.java")),
                CLASSPATH));
        assert results.size() == 2 : results;
        assert results.stream().map(result -> result.graph().decisionLabel()).distinct().count() == 2 : results;
        assert results.stream().allMatch(result ->
                result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE) : results;
    }

    private static void treatsPlatformValueOperationsAsDecisionFacts() {
        var result = analyze("calendar/CalendarPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .count() >= 1 : result.graph().nodes();
    }

    private static void supportsCollectionFactsAndRecordEquality() {
        var result = analyze("authorization/RecordAuthorizationPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert result.graph().nodes().stream()
                .anyMatch(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                        && node.businessLabel().contains("approvers")) : result.graph().nodes();
        assert result.graph().nodes().stream()
                .noneMatch(node -> node.kind() == BusinessDecisionGraph.NodeKind.DISPATCH) : result.graph().nodes();
    }

    private static void followsStrategiesThatMutateReturnedCollectionsInsideLambdas() {
        var result = analyze("aggregation/StrategyAggregationPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        long alternatives = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.DISPATCH).count();
        assert alternatives >= 1 : result.graph().nodes();
        long candidates = result.graph().edges().stream()
                .filter(edge -> edge.outcome().startsWith("candidate ")).count();
        assert candidates == 2 : result.graph().edges();
        assert result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE).count() >= 2
                : result.graph().nodes();
    }

    private static void streamPredicatesStayBusinessFacing() {
        var result = analyze("streams/StreamSelectionPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert result.graph().nodes().stream()
                .anyMatch(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                        && node.businessLabel().contains("label length is above 3")) : result.graph().nodes();
        result.graph().nodes().forEach(node -> {
            String label = node.businessLabel();
            assert !label.contains("->") && !label.contains("::") && !label.contains(".stream")
                    && !label.contains("instanceof") : node;
        });
    }

    private static void usesOneBusinessStartAndStopWithExplicitReturns() {
        var result = analyze("eligibility/EligibilityPolicy.java");
        var entries = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.ENTRY).toList();
        var outcomes = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME).toList();
        assert entries.size() == 1 && entries.getFirst().businessLabel().equals("Start") : entries;
        assert outcomes.size() == 1 && outcomes.getFirst().businessLabel().equals("Stop") : outcomes;
        var terminalNodes = result.graph().nodes().stream()
                .filter(node -> result.graph().edges().stream()
                        .noneMatch(edge -> edge.fromNodeId().equals(node.nodeId())))
                .map(BusinessDecisionGraph.DecisionNode::nodeId)
                .collect(java.util.stream.Collectors.toSet());
        assert terminalNodes.equals(java.util.Set.of(outcomes.getFirst().nodeId())) : terminalNodes;
        assert result.graph().edges().stream().filter(edge -> edge.toNodeId().equals(outcomes.getFirst().nodeId()))
                .allMatch(edge -> edge.outcome().contains("returns")) : result.graph().edges();
        var returnProbes = result.manifest().probeSites().stream()
                .filter(probe -> probe.kind() == AnalysisManifest.ProbeKind.OUTCOME).toList();
        assert returnProbes.size() == 2 : returnProbes;
        assert returnProbes.stream().map(AnalysisManifest.ProbeSite::nodeId).distinct().count() == 1 : returnProbes;
    }

    private static void removesIdentifierAndNullImplementationVocabulary() {
        var graph = analyze("authorization/RecordAuthorizationPolicy.java").graph();
        String businessText = graph.nodes().stream().map(BusinessDecisionGraph.DecisionNode::businessLabel)
                .collect(java.util.stream.Collectors.joining(" ")) + " "
                + graph.edges().stream().map(BusinessDecisionGraph.DecisionEdge::outcome)
                .collect(java.util.stream.Collectors.joining(" "));
        assert !businessText.matches("(?is).*\\bids?\\b.*") : businessText;
        assert !businessText.matches("(?is).*\\bnull\\b.*") : businessText;
        assert businessText.contains("creator exists") : businessText;
        assert businessText.contains("employee") : businessText;
    }

    private static void exportsDeveloperGraphWithRevisionPinnedSourceLinks() {
        Path repository = null;
        try {
            repository = Files.createTempDirectory("fachtracing-export-source-");
            Path source = repository.resolve("src/Policy.java");
            Files.createDirectories(source.getParent());
            Files.writeString(source, """
                    import at.gepardec.fachtracing.api.FachTracing;
                    final class Policy {
                        @FachTracing boolean decide(int age) { return age >= 18; }
                    }
                    """, StandardCharsets.UTF_8);
            initializeGitRepository(repository);
            var result = new StaticDecisionAnalyzer().analyze(
                    AnalysisRequest.of(List.of(source), CLASSPATH));
            var revision = DeveloperGraphExporter.SourceRevision.captureGit(
                    repository,
                    "https://example.invalid/rules",
                    "https://example.invalid/rules/blob/{commit}/{path}#L{line}");
            var exporter = new DeveloperGraphExporter();
            String json = exporter.export(result, revision);

            assert json.equals(exporter.export(result, revision)) : "developer export must be deterministic";
            assert json.contains("\"schema\":\"fachtracing-developer-graph/v1\"") : json;
            assert json.contains("\"commit\":\"" + revision.commit() + "\"") : json;
            assert json.contains("src/Policy.java") : json;
            assert json.contains("/blob/" + revision.commit() + "/src/Policy.java#L") : json;
            assert json.contains("\"sha256\":") : json;
            assert !json.contains(repository.toString()) : "absolute workspace path leaked into export";
            result.graph().nodes().forEach(node -> {
                assert json.contains("\"id\":\"" + node.nodeId() + "\"") : node;
            });
            result.graph().edges().forEach(edge -> {
                assert json.contains("\"id\":\"" + edge.edgeId() + "\"") : edge;
            });

            var escapedGraph = new BusinessDecisionGraph(
                    "escape-graph", 1, "quoted \"decision\"\nnext", "synthetic",
                    List.of(new BusinessDecisionGraph.DecisionNode(
                            "synthetic", BusinessDecisionGraph.NodeKind.COMPUTATION,
                            "line\n\"quoted\"", java.util.Map.of("key", "slash\\value"))),
                    List.of(), BusinessDecisionGraph.Completeness.COMPLETE, List.of());
            var escapedManifest = new AnalysisManifest(
                    "escape-graph", 1, java.util.Map.of(), List.of(), List.of(), java.util.Map.of());
            String escaped = exporter.export(
                    new AnalysisManifest.AnalysisResult(escapedGraph, escapedManifest, List.of()), revision);
            assert escaped.contains("quoted \\\"decision\\\"\\nnext") : escaped;
            assert escaped.contains("line\\n\\\"quoted\\\"") : escaped;
            assert !escaped.contains("\"source\"") : "synthetic node received fabricated source";

            try {
                exporter.export(analyze("eligibility/EligibilityPolicy.java"), revision);
                throw new AssertionError("out-of-root source path was exported");
            } catch (IllegalArgumentException expected) {
                assert expected.getMessage().contains("outside") : expected;
            }

            var staleManifest = new AnalysisManifest(
                    result.manifest().graphId(), result.manifest().graphVersion(),
                    result.manifest().sourceMappings(), result.manifest().probeSites(),
                    result.manifest().dispatchTargets(), java.util.Map.of(source.toString(), "0".repeat(64)));
            try {
                exporter.export(new AnalysisManifest.AnalysisResult(
                        result.graph(), staleManifest, result.diagnostics()), revision);
                throw new AssertionError("stale source fingerprint was accepted");
            } catch (IllegalStateException expected) {
                assert expected.getMessage().contains("does not match") : expected;
            }

            try {
                DeveloperGraphExporter.SourceRevision.captureGit(
                        repository, "https://example.invalid/rules", "https://example.invalid/{path}");
                throw new AssertionError("source template without commit was accepted");
            } catch (IllegalArgumentException expected) {
                assert expected.getMessage().contains("{commit}") : expected;
            }
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (repository != null) deleteTree(repository);
        }
    }

    private static void capturesOnlyCleanGitRevisions() {
        Path repository = null;
        try {
            repository = Files.createTempDirectory("fachtracing-export-git-");
            Path source = repository.resolve("Policy.java");
            Files.writeString(source, "final class Policy {}\n", StandardCharsets.UTF_8);
            initializeGitRepository(repository);

            var revision = DeveloperGraphExporter.SourceRevision.captureGit(
                    repository,
                    "https://example.invalid/rules",
                    "https://example.invalid/rules/blob/{commit}/{path}#L{line}");
            assert revision.commit().matches("[0-9a-f]{40,64}") : revision;
            assert revision.committedAt().contains("T") : revision;
            assert revision.relativePath(source).equals("Policy.java") : revision;

            Files.writeString(source, "final class Policy { int changed; }\n", StandardCharsets.UTF_8);
            try {
                DeveloperGraphExporter.SourceRevision.captureGit(
                        repository,
                        "https://example.invalid/rules",
                        "https://example.invalid/rules/blob/{commit}/{path}#L{line}");
                throw new AssertionError("dirty Git revision was accepted");
            } catch (IllegalStateException expected) {
                assert expected.getMessage().contains("uncommitted") : expected;
            }
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (repository != null) deleteTree(repository);
        }
    }

    private static void rejectsSourceMissingFromCapturedCommit() {
        Path repository = null;
        try {
            repository = Files.createTempDirectory("fachtracing-export-ignored-source-");
            Files.writeString(repository.resolve(".gitignore"), "generated/\n", StandardCharsets.UTF_8);
            Path source = repository.resolve("generated/Policy.java");
            Files.createDirectories(source.getParent());
            Files.writeString(source, """
                    import at.gepardec.fachtracing.api.FachTracing;
                    final class Policy {
                        @FachTracing boolean decide(int age) { return age >= 18; }
                    }
                    """, StandardCharsets.UTF_8);
            initializeGitRepository(repository);
            var result = new StaticDecisionAnalyzer().analyze(
                    AnalysisRequest.of(List.of(source), CLASSPATH));
            var revision = DeveloperGraphExporter.SourceRevision.captureGit(
                    repository,
                    "https://example.invalid/rules",
                    "https://example.invalid/rules/blob/{commit}/{path}#L{line}");

            try {
                new DeveloperGraphExporter().export(result, revision);
                throw new AssertionError("source missing from commit was exported");
            } catch (IllegalStateException expected) {
                assert expected.getMessage().contains("not present") : expected;
                assert expected.getMessage().contains("generated/Policy.java") : expected;
            }
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (repository != null) deleteTree(repository);
        }
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
        var command = new java.util.ArrayList<String>();
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

    private static void deleteTree(Path root) {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    private static AnalysisManifest.AnalysisResult analyze(String relativeFixture) {
        return new StaticDecisionAnalyzer().analyze(AnalysisRequest.of(
                List.of(FIXTURES.resolve(relativeFixture)), CLASSPATH));
    }

    private static boolean hasKind(BusinessDecisionGraph graph, BusinessDecisionGraph.NodeKind kind) {
        return graph.nodes().stream().anyMatch(node -> node.kind() == kind);
    }
}
