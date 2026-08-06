package at.gepardec.fachtracing.analysis;

import at.gepardec.fachtracing.developer.DeveloperGraphExporter;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;

/** Executable dependency-free contract tests for the static analyzer. */
public final class StaticDecisionAnalyzerTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path FIXTURES = ROOT.resolve("fachtracing-engine/src/test/resources/fixtures");
    private static final List<Path> CLASSPATH = List.of(ROOT.resolve("fachtracing-api/target/classes"));

    private StaticDecisionAnalyzerTest() { }

    public static void main(String[] args) {
        removesJavaConstructionVocabularyGenerically();
        scansMethodReceiversForEvidence();
        supportedConstructsAcrossDomains();
        bindsCompleteBooleanPredicatesToExactEdges();
        excludesResultIndependentWork();
        followsDirectCallsAcrossDomains();
        representsDynamicDispatchWithoutGuessing();
        resolvesImplementationsFromSourcesOutsideTheGraphRootScope();
        resolvesImplementationsAcrossProjectAwareSourceRoles();
        isolatesDuplicateTypesAndCompilerModelsByProject();
        supportsConnectedMixedModularAndNonModularProjects();
        supportsOwnedExternalNamedModuleSources();
        supportsOwnedExternalAutomaticModuleSources();
        rejectsInvalidJpmsContextBeforeGraphExtraction();
        rejectsIncompatibleOrUnownedJpmsSourcesBeforeExtraction();
        reportsTheSearchedBoundaryWhenImplementationsAreMissing();
        rejectsInvalidApplicationBoundaries();
        rejectsGraphRootsOutsideTheSourceUniverse();
        exposesRelevantCoverageGaps();
        sourceUnavailableDecisionLogicIsNeverReportedComplete();
        usesControlledBytecodeFallbackAndRejectsUnsafeBinary();
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
        supportsTryWithResourcesIndependently();
        reportsResultRelevantResourceFailureIndependently();
        supportsBusinessLogicInsideSynchronizedBlocks();
        supportsPatternMatchingIndependently();
        supportsSealedTypesIndependently();
        supportsNestedClassesIndependently();
        supportsMethodReferencesIndependently();
        lowersIndexedLoopsToBusinessIteration();
    }

    private static void removesJavaConstructionVocabularyGenerically() {
        assert BusinessLabelNormalizer.normalize("initialize new approval validator")
                .equals("approval validator")
                : BusinessLabelNormalizer.normalize("initialize new approval validator");
        assert BusinessLabelNormalizer.normalize("ticket validator is available")
                .equals("ticket validator is available");
        assert BusinessLabelNormalizer.normalize("evaluate create warning with enum type")
                .equals("create warning")
                : BusinessLabelNormalizer.normalize("evaluate create warning with enum type");
    }

    private static void scansMethodReceiversForEvidence() {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("fachtracing-receiver-evidence");
            Path source = directory.resolve("ReceiverPolicy.java");
            Files.writeString(source, """
                    import at.gepardec.fachtracing.api.FachTracing;
                    final class ReceiverPolicy {
                        @FachTracing("direct receiver")
                        boolean direct(String city) { return city.equals("Vienna"); }
                        @FachTracing("unsupported receiver")
                        boolean unsupported(String city) { return city.trim().equals("Vienna"); }
                    }
                    """);
            var results = new StaticDecisionAnalyzer().analyzeAll(
                    AnalysisRequest.of(List.of(source), CLASSPATH));
            var direct = results.stream().filter(item ->
                    item.graph().decisionLabel().equals("direct receiver")).findFirst().orElseThrow();
            assert direct.manifest().evidenceTargets().stream().anyMatch(target ->
                    target.argumentIndex() == 0 && target.evidenceLabel().equals("city"))
                    : direct.graph().nodes() + " / " + direct.manifest();
            var unsupported = results.stream().filter(item ->
                    item.graph().decisionLabel().equals("unsupported receiver")).findFirst().orElseThrow();
            assert unsupported.manifest().evidenceTargets().stream().anyMatch(target ->
                    target.argumentIndex() == -1 && target.sourceLine() > 0)
                    : unsupported.manifest().evidenceTargets();
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        } finally {
            if (directory != null) deleteTree(directory);
        }
    }

    private static void supportsTryWithResourcesIndependently() {
        var result = construct("try resource decision");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.graph();
        assert result.graph().nodes().stream().noneMatch(node ->
                node.businessLabel().contains("resource") || node.businessLabel().contains("close"))
                : result.graph().nodes();
    }

    private static void reportsResultRelevantResourceFailureIndependently() {
        var result = analyze("controlflow/ResourceFailurePolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : result;
        assert result.graph().coverageGaps().stream().anyMatch(gap ->
                gap.description().contains("close logic can change the decision")) : result.graph().coverageGaps();
        assert result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.line() > 0) : result.diagnostics();
    }

    private static void supportsBusinessLogicInsideSynchronizedBlocks() {
        var result = analyze("controlflow/SynchronizedPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                : result.diagnostics();
        assert result.graph().nodes().stream().anyMatch(node ->
                node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                        && node.businessLabel().contains("age is below 24")) : result.graph().nodes();
        String nodeLabels = result.graph().nodes().stream()
                .map(BusinessDecisionGraph.DecisionNode::businessLabel).toList().toString();
        assert !nodeLabels.contains("monitor") : nodeLabels;
        assert !hasKind(result.graph(), BusinessDecisionGraph.NodeKind.COVERAGE_GAP) : result.graph();
    }

    private static void supportsPatternMatchingIndependently() {
        var result = construct("pattern decision");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert hasKind(result.graph(), BusinessDecisionGraph.NodeKind.PREDICATE);
    }

    private static void supportsSealedTypesIndependently() {
        var result = construct("sealed decision");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert result.graph().edges().stream().filter(edge -> edge.outcome().equals("selected rule")).count() == 2
                : result.graph().edges();
    }

    private static void supportsNestedClassesIndependently() {
        var result = construct("nested decision");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert result.graph().nodes().stream().anyMatch(node -> node.businessLabel().contains("age is at least 24"))
                : result.graph().nodes();
    }

    private static void supportsMethodReferencesIndependently() {
        var result = construct("method reference decision");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert result.graph().nodes().stream().anyMatch(node -> node.businessLabel().contains("amount is below 100"))
                : result.graph().nodes();
    }

    private static void lowersIndexedLoopsToBusinessIteration() {
        var result = analyze("loops/IndexedEntryPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                : result.diagnostics();
        assert result.graph().nodes().stream().anyMatch(node ->
                node.businessLabel().equals("a following entry exists")) : result.graph().nodes();
        assert result.graph().nodes().stream().anyMatch(node ->
                node.businessLabel().contains("age is below 24")) : result.graph().nodes();
        assert new BusinessArtifactGuard().violations(result.graph()).isEmpty()
                : new BusinessArtifactGuard().violations(result.graph());
        String business = result.graph().nodes().stream()
                .map(BusinessDecisionGraph.DecisionNode::businessLabel)
                .collect(java.util.stream.Collectors.joining(" | "));
        assert !business.matches("(?i).*\\b(?:index|idx|size)\\b.*") : business;
    }

    private static AnalysisManifest.AnalysisResult construct(String label) {
        return new StaticDecisionAnalyzer().analyzeAll(AnalysisRequest.of(
                        List.of(FIXTURES.resolve("constructs/JavaConstructPolicy.java")), CLASSPATH)).stream()
                .filter(result -> result.graph().decisionLabel().equals(label)).findFirst().orElseThrow();
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
            assert !target.descriptorHint().isBlank() : target;
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
        var predicateIds = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .map(BusinessDecisionGraph.DecisionNode::nodeId).toList();
        var branchOutcomes = result.graph().edges().stream()
                .filter(edge -> predicateIds.contains(edge.fromNodeId()))
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
                .filter(edge -> edge.outcome().equals("selected rule"))
                .count();
        assert alternatives == 2 : result.graph().edges();
        assert result.manifest().dispatchTargets().size() == 2 : result.manifest();
        result.manifest().dispatchTargets().forEach(target -> {
            assert !target.descriptorHint().isBlank() : target;
        });
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
                .filter(edge -> edge.outcome().equals("selected rule")).count();
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
                .filter(edge -> edge.outcome().equals("selected rule")).count();
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

    private static void supportsConnectedMixedModularAndNonModularProjects() {
        Path root = null;
        try {
            root = Files.createTempDirectory("fachtracing-mixed-jpms-");
            Path libraryRoot = root.resolve("library");
            Path consumerRoot = root.resolve("consumer");
            Path library = libraryRoot.resolve("mixed/library/LibraryPolicy.java");
            Path consumer = consumerRoot.resolve("mixed/consumer/ConsumerPolicy.java");
            Path crossMode = consumerRoot.resolve("mixed/consumer/CrossModePolicy.java");
            Files.createDirectories(library.getParent());
            Files.createDirectories(consumer.getParent());
            Files.writeString(library, """
                    package mixed.library;
                    import at.gepardec.fachtracing.api.FachTracing;
                    public final class LibraryPolicy {
                        @FachTracing("mixed library decision")
                        public boolean decide(int age) { return age < 24; }
                        public static boolean young(int age) { return age < 24; }
                    }
                    """);
            Files.writeString(consumer, """
                    package mixed.consumer;
                    import at.gepardec.fachtracing.api.FachTracing;
                    public final class ConsumerPolicy {
                        @FachTracing("mixed module decision")
                        public boolean decide(int amount) { return amount >= 100; }
                    }
                    """);
            Files.writeString(crossMode, """
                    package mixed.consumer;
                    import at.gepardec.fachtracing.api.FachTracing;
                    import mixed.library.LibraryPolicy;
                    public final class CrossModePolicy {
                        @FachTracing("mixed unavailable source decision")
                        public boolean decide(int age) { return LibraryPolicy.young(age); }
                    }
                    """);
            Path binaryClasses = root.resolve("binary-classes");
            Files.createDirectories(binaryClasses);
            int compilation = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "--release", "21", "-classpath", CLASSPATH.getFirst().toString(),
                    "-d", binaryClasses.toString(), library.toString());
            assert compilation == 0 : "could not compile the non-modular test library";
            Path libraryJar = root.resolve("mixed.library.jar");
            createJar(binaryClasses, libraryJar);
            Path descriptor = consumerRoot.resolve("module-info.java");
            Files.writeString(descriptor, """
                    module mixed.consumer {
                        requires at.gepardec.fachtracing.api;
                        requires mixed.library;
                    }
                    """);
            Path apiModule = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
            List<Path> mixedClasspath = List.of(apiModule, libraryJar);
            var modularModel = new ApplicationSourceBoundary.CompilerModel(
                    StandardCharsets.UTF_8, "21", List.of(), mixedClasspath);
            var boundary = new ApplicationSourceBoundary(List.of(
                    new ApplicationSourceBoundary.ProjectSources(
                            "library", List.of(library), List.of(library), CLASSPATH,
                            ApplicationSourceBoundary.CompilerModel.java21(), List.of()),
                    new ApplicationSourceBoundary.ProjectSources(
                            "consumer", List.of(consumer, crossMode), List.of(consumer, crossMode), mixedClasspath,
                            modularModel, List.of("library"), java.util.Optional.of(descriptor))), List.of());

            var results = new StaticDecisionAnalyzer().analyzeAll(boundary);
            assert results.stream().map(result -> result.graph().decisionLabel()).sorted().toList()
                    .equals(List.of("mixed library decision", "mixed module decision",
                            "mixed unavailable source decision")) : results;
            assert results.stream().allMatch(result -> result.graph().completeness()
                    == BusinessDecisionGraph.Completeness.COMPLETE) : results;
            var unavailable = results.stream().filter(result -> result.graph().decisionLabel()
                    .equals("mixed unavailable source decision")).findFirst().orElseThrow();
            assert unavailable.graph().nodes().stream().anyMatch(node ->
                    node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                            && node.businessLabel().contains("input 1 is below 24")) : unavailable;
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (root != null) deleteTree(root);
        }
    }

    private static void supportsOwnedExternalNamedModuleSources() {
        Path root = null;
        try {
            root = Files.createTempDirectory("fachtracing-owned-jpms-");
            Path entryRoot = root.resolve("entry");
            Path rulesRoot = root.resolve("rules");
            Path entry = entryRoot.resolve("owned/entry/Policy.java");
            Path rule = rulesRoot.resolve("owned/rules/AgeRule.java");
            Files.createDirectories(entry.getParent());
            Files.createDirectories(rule.getParent());
            Files.writeString(entry, """
                    package owned.entry;
                    import at.gepardec.fachtracing.api.FachTracing;
                    import owned.rules.AgeRule;
                    public final class Policy {
                        @FachTracing("owned module decision")
                        public boolean decide(int age) { return AgeRule.accepts(age); }
                    }
                    """);
            Files.writeString(rule, """
                    package owned.rules;
                    public final class AgeRule {
                        public static boolean accepts(int age) { return age >= 24; }
                    }
                    """);
            Path entryModule = entryRoot.resolve("module-info.java");
            Path rulesModule = rulesRoot.resolve("module-info.java");
            Files.writeString(entryModule, """
                    module owned.entry {
                        requires at.gepardec.fachtracing.api;
                        requires owned.rules;
                    }
                    """);
            Files.writeString(rulesModule, "module owned.rules { exports owned.rules; }");
            var compiler = new ApplicationSourceBoundary.CompilerModel(
                    StandardCharsets.UTF_8, "21", List.of(), CLASSPATH);
            var ownership = ApplicationSourceBoundary.ModuleOwnership.named(
                    "owned.rules", rulesModule, rulesRoot);
            var external = new ApplicationSourceBoundary.ResolutionSource(rule,
                    new ApplicationSourceBoundary.SourceOrigin(
                            ApplicationSourceBoundary.OriginKind.MAVEN_SOURCE,
                            "example:owned-rules:1.0", "a".repeat(64)), ownership);
            var boundary = new ApplicationSourceBoundary(List.of(
                    new ApplicationSourceBoundary.ProjectSources(
                            "entry", List.of(entry), List.of(entry), CLASSPATH,
                            compiler, List.of(), java.util.Optional.of(entryModule))), List.of(external));

            var result = new StaticDecisionAnalyzer().analyze(boundary);
            assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                    : result.diagnostics();
            assert result.graph().nodes().stream().anyMatch(node ->
                    node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                            && node.businessLabel().contains("age is at least 24"))
                    : result.graph().nodes();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (root != null) deleteTree(root);
        }
    }

    private static void supportsOwnedExternalAutomaticModuleSources() {
        Path root = null;
        try {
            root = Files.createTempDirectory("fachtracing-owned-auto-jpms-");
            Path entryRoot = root.resolve("entry");
            Path rulesRoot = root.resolve("rules-source");
            Path entry = entryRoot.resolve("owned/entry/AutomaticPolicy.java");
            Path rule = rulesRoot.resolve("owned/automatic/AmountRule.java");
            Files.createDirectories(entry.getParent());
            Files.createDirectories(rule.getParent());
            Files.writeString(entry, """
                    package owned.entry;
                    import at.gepardec.fachtracing.api.FachTracing;
                    import owned.automatic.AmountRule;
                    public final class AutomaticPolicy {
                        @FachTracing("automatic module decision")
                        public boolean decide(int amount) { return AmountRule.accepts(amount); }
                    }
                    """);
            Files.writeString(rule, """
                    package owned.automatic;
                    public final class AmountRule {
                        public static boolean accepts(int amount) { return amount >= 100; }
                    }
                    """);
            Path binaryClasses = root.resolve("rules-classes");
            Files.createDirectories(binaryClasses);
            int compilation = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "--release", "21", "-d", binaryClasses.toString(), rule.toString());
            assert compilation == 0 : "could not compile the automatic-module test rule";
            Path binary = root.resolve("owned.rules.auto.jar");
            createJar(binaryClasses, binary);
            Path entryModule = entryRoot.resolve("module-info.java");
            Files.writeString(entryModule, """
                    module owned.entry.auto {
                        requires at.gepardec.fachtracing.api;
                        requires owned.rules.auto;
                    }
                    """);
            var modulePath = List.of(apiClasses(), binary);
            var compiler = new ApplicationSourceBoundary.CompilerModel(
                    StandardCharsets.UTF_8, "21", List.of(), modulePath);
            var ownership = ApplicationSourceBoundary.ModuleOwnership.automatic(
                    "owned.rules.auto", binary, rulesRoot);
            var external = new ApplicationSourceBoundary.ResolutionSource(rule,
                    new ApplicationSourceBoundary.SourceOrigin(
                            ApplicationSourceBoundary.OriginKind.MAVEN_SOURCE,
                            "example:owned-rules-auto:1.0", "b".repeat(64)), ownership);
            var boundary = new ApplicationSourceBoundary(List.of(
                    new ApplicationSourceBoundary.ProjectSources(
                            "entry", List.of(entry), List.of(entry), modulePath,
                            compiler, List.of(), java.util.Optional.of(entryModule))), List.of(external));

            var result = new StaticDecisionAnalyzer().analyze(boundary);
            assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                    : result.diagnostics();
            assert result.graph().nodes().stream().anyMatch(node ->
                    node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                            && node.businessLabel().contains("amount is at least 100"))
                    : result.graph().nodes();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (root != null) deleteTree(root);
        }
    }

    private static Path apiClasses() {
        return Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
    }

    private static void createJar(Path classes, Path target) throws IOException {
        try (var output = new JarOutputStream(Files.newOutputStream(target));
                var paths = Files.walk(classes)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = classes.relativize(path).toString().replace(java.io.File.separatorChar, '/');
                output.putNextEntry(new JarEntry(name));
                Files.copy(path, output);
                output.closeEntry();
            }
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

    private static void rejectsIncompatibleOrUnownedJpmsSourcesBeforeExtraction() {
        Path root = null;
        try {
            root = Files.createTempDirectory("fachtracing-jpms-boundary-");
            Path oneRoot = root.resolve("one");
            Path twoRoot = root.resolve("two");
            Path one = oneRoot.resolve("one/Policy.java");
            Path two = twoRoot.resolve("two/Rule.java");
            Files.createDirectories(one.getParent());
            Files.createDirectories(two.getParent());
            Files.writeString(one, """
                    package one;
                    import at.gepardec.fachtracing.api.FachTracing;
                    public final class Policy {
                        @FachTracing("JPMS boundary") public boolean decide() { return true; }
                    }
                    """);
            Files.writeString(two, "package two; public final class Rule { }");
            Path oneModule = oneRoot.resolve("module-info.java");
            Path twoModule = twoRoot.resolve("module-info.java");
            Files.writeString(oneModule, "module test.one { requires at.gepardec.fachtracing.api; requires test.two; }");
            Files.writeString(twoModule, "module test.two { exports two; }");
            var java21 = new ApplicationSourceBoundary.CompilerModel(
                    StandardCharsets.UTF_8, "21", List.of(), CLASSPATH);
            var java17 = new ApplicationSourceBoundary.CompilerModel(
                    StandardCharsets.UTF_8, "17", List.of(), CLASSPATH);
            var incompatible = new ApplicationSourceBoundary(List.of(
                    new ApplicationSourceBoundary.ProjectSources("one", List.of(one), List.of(one), CLASSPATH,
                            java21, List.of("two"), java.util.Optional.of(oneModule)),
                    new ApplicationSourceBoundary.ProjectSources("two", List.of(), List.of(two), CLASSPATH,
                            java17, List.of(), java.util.Optional.of(twoModule))), List.of());
            try {
                new StaticDecisionAnalyzer().analyzeAll(incompatible);
                throw new AssertionError("incompatible JPMS compiler settings were accepted");
            } catch (IllegalArgumentException expected) {
                assert expected.getMessage().contains("incompatible compiler settings") : expected;
            }

            var external = new ApplicationSourceBoundary.ResolutionSource(two,
                    new ApplicationSourceBoundary.SourceOrigin(
                            ApplicationSourceBoundary.OriginKind.LOCAL, "unowned", ""));
            var unowned = new ApplicationSourceBoundary(List.of(
                    new ApplicationSourceBoundary.ProjectSources("one", List.of(one), List.of(one), CLASSPATH,
                            java21, List.of(), java.util.Optional.of(oneModule))), List.of(external));
            try {
                new StaticDecisionAnalyzer().analyzeAll(unowned);
                throw new AssertionError("unowned external JPMS source was accepted");
            } catch (IllegalArgumentException expected) {
                assert expected.getMessage().contains("cannot assign external sources") : expected;
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

    private static void usesControlledBytecodeFallbackAndRejectsUnsafeBinary() {
        Path root = null;
        try {
            root = Files.createTempDirectory("fachtracing-bytecode-fallback-");
            Path binarySourceRoot = root.resolve("binary-source");
            Path binaryClasses = root.resolve("binary-classes");
            Path entryRoot = root.resolve("entry-source");
            Path safeRule = binarySourceRoot.resolve("binaryrules/BinaryAgeRule.java");
            Path calculatedRule = binarySourceRoot.resolve("binaryrules/CalculatedBinaryRule.java");
            Path unsafeRule = binarySourceRoot.resolve("binaryrules/UnsafeBinaryRule.java");
            Path entry = entryRoot.resolve("entry/BinaryPolicy.java");
            Files.createDirectories(safeRule.getParent());
            Files.createDirectories(entry.getParent());
            Files.createDirectories(binaryClasses);
            Files.writeString(safeRule, """
                    package binaryrules;
                    public final class BinaryAgeRule {
                        public static boolean accepts(int age) { return age >= 24; }
                    }
                    """);
            Files.writeString(unsafeRule, """
                    package binaryrules;
                    public final class UnsafeBinaryRule {
                        public static boolean accepts(int age) { return Integer.toString(age).length() > 1; }
                    }
                    """);
            Files.writeString(calculatedRule, """
                    package binaryrules;
                    public final class CalculatedBinaryRule {
                        private final int threshold;
                        public CalculatedBinaryRule() { threshold = 30; }
                        public boolean accepts(int age, int credit) { return age + credit >= threshold; }
                    }
                    """);
            int compilation = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "--release", "21", "-d", binaryClasses.toString(),
                    safeRule.toString(), calculatedRule.toString(), unsafeRule.toString());
            assert compilation == 0 : "could not compile bytecode fallback rules";
            Files.writeString(entry, """
                    package entry;
                    import at.gepardec.fachtracing.api.FachTracing;
                    import binaryrules.BinaryAgeRule;
                    import binaryrules.CalculatedBinaryRule;
                    import binaryrules.UnsafeBinaryRule;
                    public final class BinaryPolicy {
                        @FachTracing("safe binary decision")
                        public boolean safe(int age) { return BinaryAgeRule.accepts(age); }
                        @FachTracing("calculated binary decision")
                        public boolean calculated(int age, int credit) {
                            return new CalculatedBinaryRule().accepts(age, credit);
                        }
                        @FachTracing("unsafe binary decision")
                        public boolean unsafe(int age) { return UnsafeBinaryRule.accepts(age); }
                    }
                    """);
            var results = new StaticDecisionAnalyzer().analyzeAll(AnalysisRequest.of(
                    List.of(entry), List.of(CLASSPATH.getFirst(), binaryClasses)));
            var safe = results.stream().filter(item -> item.graph().decisionLabel()
                    .equals("safe binary decision")).findFirst().orElseThrow();
            assert safe.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : safe;
            assert safe.graph().nodes().stream().anyMatch(node ->
                    node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                            && node.businessLabel().equals("input 1 is at least 24")) : safe.graph().nodes();
            assert safe.manifest().probeSites().stream().anyMatch(site ->
                    site.ownerHint().equals("binaryrules.BinaryAgeRule")
                            && site.descriptorHint().equals("(I)Z")) : safe.manifest();

            var calculated = results.stream().filter(item -> item.graph().decisionLabel()
                    .equals("calculated binary decision")).findFirst().orElseThrow();
            assert calculated.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : calculated;
            assert calculated.graph().nodes().stream().anyMatch(node ->
                    node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                            && node.businessLabel().equals(
                            "input 1 plus input 2 is at least configured value 1"))
                    : calculated.graph().nodes();

            var unsafe = results.stream().filter(item -> item.graph().decisionLabel()
                    .equals("unsafe binary decision")).findFirst().orElseThrow();
            assert unsafe.graph().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : unsafe;
            assert unsafe.graph().coverageGaps().stream().anyMatch(gap ->
                    gap.description().contains("unsupported call")) : unsafe.graph().coverageGaps();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            if (root != null) deleteTree(root);
        }
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
                .filter(edge -> edge.outcome().equals("selected rule")).count();
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
