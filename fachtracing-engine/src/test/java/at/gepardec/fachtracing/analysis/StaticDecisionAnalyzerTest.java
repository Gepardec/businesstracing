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
        bindsCompleteBooleanPredicatesToExactEdges();
        excludesResultIndependentWork();
        followsDirectCallsAcrossDomains();
        representsDynamicDispatchWithoutGuessing();
        resolvesImplementationsFromSourcesOutsideTheGraphRootScope();
        resolvesImplementationsAcrossProjectAwareSourceRoles();
        isolatesDuplicateTypesAndCompilerModelsByProject();
        rejectsInvalidJpmsContextBeforeGraphExtraction();
        reportsTheSearchedBoundaryWhenImplementationsAreMissing();
        rejectsInvalidApplicationBoundaries();
        rejectsGraphRootsOutsideTheSourceUniverse();
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
        exportsMultiOriginDeveloperProvenanceWithoutFalseGitLinks();
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

    private static void bindsCompleteBooleanPredicatesToExactEdges() {
        var result = analyze("eligibility/EligibilityPolicy.java");
        assert !result.manifest().branchTargets().isEmpty() : result.manifest();
        for (AnalysisManifest.BranchTarget target : result.manifest().branchTargets()) {
            var trueEdge = result.graph().edges().stream()
                    .filter(edge -> edge.edgeId().equals(target.trueEdgeId())).findFirst().orElseThrow();
            var falseEdge = result.graph().edges().stream()
                    .filter(edge -> edge.edgeId().equals(target.falseEdgeId())).findFirst().orElseThrow();
            assert trueEdge.fromNodeId().equals(target.nodeId()) : target;
            assert falseEdge.fromNodeId().equals(target.nodeId()) : target;
            assert trueEdge.outcome().equals("true") || trueEdge.outcome().startsWith("true;") : trueEdge;
            assert falseEdge.outcome().equals("false") || falseEdge.outcome().startsWith("false;") : falseEdge;
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

    private static void resolvesImplementationsFromSourcesOutsideTheGraphRootScope() {
        Path entry = FIXTURES.resolve("reactor/DecisionEntry.java");
        List<Path> sourceUniverse = List.of(
                entry,
                FIXTURES.resolve("reactor/LocalDecisionRule.java"),
                FIXTURES.resolve("reactor/RegionalDecisionRule.java"));
        var results = new StaticDecisionAnalyzer().analyzeAll(
                new AnalysisRequest(sourceUniverse, CLASSPATH, StandardCharsets.UTF_8, List.of(entry)));
        assert results.size() == 1 : results;
        var graph = results.getFirst().graph();
        assert graph.decisionLabel().equals("reactor approval") : graph.decisionLabel();
        long candidates = graph.edges().stream()
                .filter(edge -> edge.outcome().startsWith("candidate ")).count();
        assert candidates == 2 : graph.edges();
        assert graph.nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE).count() == 2
                : graph.nodes();
    }

    private static void resolvesImplementationsAcrossProjectAwareSourceRoles() {
        Path entry = FIXTURES.resolve("reactor/DecisionEntry.java");
        Path local = FIXTURES.resolve("reactor/LocalDecisionRule.java");
        Path regional = FIXTURES.resolve("reactor/RegionalDecisionRule.java");
        var compiler = ApplicationSourceBoundary.CompilerModel.java21();
        var boundary = new ApplicationSourceBoundary(List.of(
                new ApplicationSourceBoundary.ProjectSources(
                        "entry", List.of(entry), List.of(entry), CLASSPATH, compiler,
                        List.of("implementations")),
                new ApplicationSourceBoundary.ProjectSources(
                        "implementations", List.of(), List.of(local, regional), CLASSPATH, compiler,
                        List.of())), List.of());

        var results = new StaticDecisionAnalyzer().analyzeAll(boundary);
        assert results.size() == 1 : results;
        long candidates = results.getFirst().graph().edges().stream()
                .filter(edge -> edge.outcome().startsWith("candidate ")).count();
        assert candidates == 2 : results.getFirst().graph().edges();
        assert boundary.entrySourceFiles().equals(List.of(entry.toAbsolutePath().normalize()));
        assert boundary.resolutionSourceFiles().size() == 3 : boundary.resolutionSourceFiles();
        assert boundary.fingerprint().length() == 64 : boundary.fingerprint();
        assert boundary.fingerprint().equals(boundary.fingerprint());
    }

    private static void reportsTheSearchedBoundaryWhenImplementationsAreMissing() {
        Path entry = FIXTURES.resolve("reactor/DecisionEntry.java");
        var boundary = new ApplicationSourceBoundary(List.of(
                new ApplicationSourceBoundary.ProjectSources(
                        "entry", List.of(entry), List.of(entry), CLASSPATH,
                        ApplicationSourceBoundary.CompilerModel.java21(), List.of())), List.of());
        var result = new StaticDecisionAnalyzer().analyze(boundary);
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : result.graph();
        assert result.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.message().contains("searched projects [entry]")
                        && diagnostic.message().contains(boundary.fingerprint())) : result.diagnostics();
    }

    private static void isolatesDuplicateTypesAndCompilerModelsByProject() {
        Path firstRoot = null;
        Path secondRoot = null;
        try {
            firstRoot = Files.createTempDirectory("fachtracing-project-java17-");
            secondRoot = Files.createTempDirectory("fachtracing-project-java21-");
            Path first = firstRoot.resolve("same/Policy.java");
            Path second = secondRoot.resolve("same/Policy.java");
            Files.createDirectories(first.getParent());
            Files.createDirectories(second.getParent());
            Files.writeString(first, """
                    package same;
                    import at.gepardec.fachtracing.api.FachTracing;
                    final class Policy {
                        @FachTracing("java 17 decision") boolean decide(int age) { return age >= 17; }
                    }
                    """);
            Files.writeString(second, """
                    package same;
                    import at.gepardec.fachtracing.api.FachTracing;
                    final class Policy {
                        @FachTracing("java 21 decision") boolean decide(int age) { return age >= 21; }
                    }
                    """);
            Path moduleDescriptor = secondRoot.resolve("module-info.java");
            Files.writeString(moduleDescriptor, "module same.policy { requires at.gepardec.fachtracing.api; }");
            Path apiModule = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
            var boundary = new ApplicationSourceBoundary(List.of(
                    new ApplicationSourceBoundary.ProjectSources(
                            "java17", List.of(first), List.of(first), CLASSPATH,
                            new ApplicationSourceBoundary.CompilerModel(
                                    StandardCharsets.UTF_8, "17", List.of()), List.of()),
                    new ApplicationSourceBoundary.ProjectSources(
                            "java21", List.of(second), List.of(second), CLASSPATH,
                            new ApplicationSourceBoundary.CompilerModel(
                                    StandardCharsets.UTF_8, "21", List.of(), List.of(apiModule)), List.of(),
                            java.util.Optional.of(moduleDescriptor))), List.of());
            var results = new StaticDecisionAnalyzer().analyzeAll(boundary);
            assert results.stream().map(result -> result.graph().decisionLabel()).sorted().toList()
                    .equals(List.of("java 17 decision", "java 21 decision")) : results;
            assert boundary.projects().get(1).moduleDescriptor().orElseThrow().equals(
                    moduleDescriptor.toAbsolutePath().normalize());
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (secondRoot != null) deleteTree(secondRoot);
            if (firstRoot != null) deleteTree(firstRoot);
        }
    }

    private static void rejectsInvalidApplicationBoundaries() {
        Path entry = FIXTURES.resolve("reactor/DecisionEntry.java");
        try {
            new ApplicationSourceBoundary(List.of(new ApplicationSourceBoundary.ProjectSources(
                    "entry", List.of(entry), List.of(entry), CLASSPATH,
                    ApplicationSourceBoundary.CompilerModel.java21(), List.of("missing"))), List.of());
            throw new AssertionError("boundary accepted an unknown project dependency");
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("unknown project dependency") : expected.getMessage();
        }

        var java17 = new ApplicationSourceBoundary.CompilerModel(StandardCharsets.UTF_8, "17", List.of());
        var incompatible = new ApplicationSourceBoundary(List.of(
                new ApplicationSourceBoundary.ProjectSources(
                        "entry", List.of(entry), List.of(entry), CLASSPATH,
                        ApplicationSourceBoundary.CompilerModel.java21(), List.of("other")),
                new ApplicationSourceBoundary.ProjectSources(
                        "other", List.of(), List.of(), CLASSPATH, java17, List.of())), List.of());
        try {
            incompatible.toAnalysisRequest();
            throw new AssertionError("flat analysis accepted incompatible compiler models");
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("compiler models differ") : expected.getMessage();
        }
    }

    private static void rejectsInvalidJpmsContextBeforeGraphExtraction() {
        Path root = null;
        try {
            root = Files.createTempDirectory("fachtracing-invalid-jpms-");
            Path source = root.resolve("example/Policy.java");
            Files.createDirectories(source.getParent());
            Files.writeString(source, """
                    package example;
                    import at.gepardec.fachtracing.api.FachTracing;
                    final class Policy {
                        @FachTracing("invalid module decision") boolean decide() { return true; }
                    }
                    """);
            Path descriptor = root.resolve("module-info.java");
            Files.writeString(descriptor, "module invalid.policy { requires missing.business.rules; }");
            var model = new ApplicationSourceBoundary.CompilerModel(
                    StandardCharsets.UTF_8, "21", List.of(), CLASSPATH);
            var boundary = new ApplicationSourceBoundary(List.of(
                    new ApplicationSourceBoundary.ProjectSources(
                            "invalid", List.of(source), List.of(source), CLASSPATH,
                            model, List.of(), java.util.Optional.of(descriptor))), List.of());
            try {
                new StaticDecisionAnalyzer().analyzeAll(boundary);
                throw new AssertionError("invalid JPMS context was accepted");
            } catch (IllegalArgumentException expected) {
                assert expected.getMessage().contains("Effective compiler context failed") : expected;
                assert expected.getMessage().contains("module not found") : expected;
            }
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (root != null) deleteTree(root);
        }
    }

    private static void rejectsGraphRootsOutsideTheSourceUniverse() {
        Path source = FIXTURES.resolve("eligibility/EligibilityPolicy.java");
        try {
            new AnalysisRequest(List.of(source), CLASSPATH, StandardCharsets.UTF_8,
                    List.of(FIXTURES.resolve("pricing/PricingPolicy.java")));
            throw new AssertionError("request accepted a graph root outside its source universe");
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("root source files") : expected.getMessage();
        }
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

    private static void exportsMultiOriginDeveloperProvenanceWithoutFalseGitLinks() {
        Path repository = null;
        Path external = null;
        try {
            repository = Files.createTempDirectory("fachtracing-export-origin-git-");
            external = Files.createTempDirectory("fachtracing-export-origin-maven-");
            Path entry = repository.resolve("example/Policy.java");
            Path implementation = external.resolve("example/AdultRule.java");
            Files.createDirectories(entry.getParent());
            Files.createDirectories(implementation.getParent());
            Files.writeString(entry, """
                    package example;
                    import at.gepardec.fachtracing.api.FachTracing;
                    interface Rule { boolean decide(int age); }
                    final class Policy {
                        @FachTracing("origin decision")
                        boolean decide(Rule rule, int age) { return rule.decide(age); }
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(implementation, """
                    package example;
                    final class AdultRule implements Rule {
                        public boolean decide(int age) { return age >= 18; }
                    }
                    """, StandardCharsets.UTF_8);
            initializeGitRepository(repository);
            var analysis = new StaticDecisionAnalyzer().analyze(
                    AnalysisRequest.of(List.of(entry, implementation), CLASSPATH));
            var revision = DeveloperGraphExporter.SourceRevision.captureGit(
                    repository, "https://example.invalid/rules",
                    "https://example.invalid/rules/blob/{commit}/{path}#L{line}");
            var catalog = new DeveloperGraphExporter.SourceCatalog(List.of(
                    DeveloperGraphExporter.SourceOrigin.git("git", revision),
                    DeveloperGraphExporter.SourceOrigin.external(
                            "rules-source", DeveloperGraphExporter.OriginKind.MAVEN_SOURCE,
                            external, "example:rules:1.0", "archive-sha256")));

            String json = new DeveloperGraphExporter().export(analysis, catalog);
            assert json.contains("\"schema\":\"fachtracing-developer-graph/v2\"") : json;
            assert json.contains("\"kind\":\"MAVEN_SOURCE\"") : json;
            assert json.contains("\"identity\":\"example:rules:1.0\"") : json;
            int urls = json.split("\\\"url\\\"", -1).length - 1;
            Path gitRoot = repository;
            long gitMappedNodes = analysis.manifest().sourceMappings().values().stream()
                    .filter(mapping -> mapping.source().toAbsolutePath().normalize().startsWith(gitRoot)).count();
            assert urls == gitMappedNodes : "external source received a Git URL: " + json;
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (external != null) deleteTree(external);
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
