package at.gepardec.fachtracing.conformance;

import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.analysis.BusinessArtifactGuard;
import at.gepardec.fachtracing.analysis.BusinessEntryPoint;
import at.gepardec.fachtracing.agent.FachtracingTransformer;
import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.business.BusinessExecutionGraphProjector;
import at.gepardec.fachtracing.business.BusinessExecutionTextRenderer;
import at.gepardec.fachtracing.business.BusinessGraphProjector;
import at.gepardec.fachtracing.business.BusinessLogicArtifactGuard;
import at.gepardec.fachtracing.business.BusinessMermaidRenderer;
import at.gepardec.fachtracing.explain.DecisionExplanationProjector;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.mermaid.MermaidRenderer;
import at.gepardec.fachtracing.plantuml.PlantUmlRenderer;
import at.gepardec.fachtracing.runtime.RuntimeCollector;
import at.gepardec.fachtracing.runtime.TraceRuntime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executable conformance test against an externally supplied, unchanged source tree. */
public final class MegaBackendConformanceTest {
    private static final List<BusinessEntryPoint> ENTRY_POINTS = List.of(
            new BusinessEntryPoint(
                    "com.gepardec.mega.domain.calculation.journey.JourneyDirectionValidator",
                    "validate",
                    List.of(
                            "com.gepardec.mega.domain.model.monthlyreport.JourneyDirection",
                            "com.gepardec.mega.domain.model.monthlyreport.JourneyDirection"),
                    "validate journey direction"),
            new BusinessEntryPoint(
                    "com.gepardec.mega.domain.calculation.time.TimeOverlapCalculator",
                    "isOverlapping",
                    List.of(
                            "com.gepardec.mega.domain.model.monthlyreport.ProjectEntry",
                            "com.gepardec.mega.domain.model.monthlyreport.ProjectEntry"),
                    "detect overlapping time entries"),
            new BusinessEntryPoint(
                    "com.gepardec.mega.hexagon.monthend.domain.model.MonthEndClarification",
                    "canBeResolvedBy",
                    List.of("com.gepardec.mega.hexagon.shared.domain.model.UserId"),
                    "authorize clarification resolution"),
            new BusinessEntryPoint(
                    "com.gepardec.mega.hexagon.project.domain.model.Project",
                    "isActiveIn",
                    List.of("java.time.YearMonth"),
                    "determine project activity in month"),
            new BusinessEntryPoint(
                    "com.gepardec.mega.service.helper.WarningCalculatorsManager",
                    "determineJourneyWarnings",
                    List.of("java.util.List"),
                    "determine journey warnings"));
    private static final Set<String> EXPECTED_DECISIONS = Set.of(
            "authorize clarification resolution",
            "detect overlapping time entries",
            "determine journey warnings",
            "determine project activity in month",
            "validate journey direction");
    private static final Set<String> REQUIRED_COMPLETE = Set.of(
            "authorize clarification resolution",
            "detect overlapping time entries",
            "determine journey warnings",
            "determine project activity in month",
            "validate journey direction");

    private MegaBackendConformanceTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            throw new IllegalArgumentException(
                    "usage: <repository-root> <mega-root> <dependency-classpath-file> <generated-dir> <oracle-dir>");
        }
        Path repository = Path.of(args[0]).toAbsolutePath().normalize();
        Path reference = Path.of(args[1]).toAbsolutePath().normalize();
        Path generated = Path.of(args[3]).toAbsolutePath().normalize();
        Path oracles = Path.of(args[4]).toAbsolutePath().normalize();
        if (args.length != 5) throw new IllegalArgumentException("reviewed oracles are immutable during verification");

        List<Path> sources;
        try (var paths = Files.walk(reference.resolve("src/main/java"))) {
            sources = paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        assert sources.size() >= 400 : "expected realistic source corpus, found " + sources.size();

        List<Path> classpath = new ArrayList<>();
        classpath.add(repository.resolve("fachtracing-api/target/classes"));
        classpath.add(reference.resolve("target/classes"));
        for (String item : Files.readString(Path.of(args[2])).split(java.io.File.pathSeparator)) {
            if (!item.isBlank()) classpath.add(Path.of(item));
        }

        long started = System.nanoTime();
        var results = new StaticDecisionAnalyzer().analyzeAll(
                AnalysisRequest.of(sources, classpath).withBusinessEntryPoints(ENTRY_POINTS));
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        Map<String, AnalysisManifest.AnalysisResult> analyses = new LinkedHashMap<>();
        results.stream().sorted(Comparator.comparing(result -> result.graph().decisionLabel()))
                .forEach(result -> analyses.put(result.graph().decisionLabel(), result));
        Map<String, BusinessDecisionGraph> graphs = new LinkedHashMap<>();
        analyses.forEach((label, result) -> graphs.put(label, result.graph()));

        assert graphs.keySet().equals(EXPECTED_DECISIONS) : graphs.keySet();
        Files.createDirectories(generated);
        Files.createDirectories(oracles);
        var renderer = new PlantUmlRenderer();
        var mermaid = new MermaidRenderer();
        var businessProjector = new BusinessGraphProjector();
        var businessMermaid = new BusinessMermaidRenderer();
        var topologyMismatches = new ArrayList<String>();
        for (var entry : graphs.entrySet()) {
            String name = slug(entry.getKey());
            BusinessDecisionGraph graph = entry.getValue();
            assert new BusinessArtifactGuard().violations(graph).isEmpty()
                    : entry.getKey() + " technical graph output: "
                    + new BusinessArtifactGuard().violations(graph);
            String semantic = semanticTopology(graph);
            String structure = renderer.structure(graph);
            String mermaidStructure = mermaid.structure(graph);
            Files.writeString(generated.resolve(name + "-structure.puml"), structure);
            Files.writeString(generated.resolve(name + "-structure.mmd"), mermaidStructure);
            Files.writeString(generated.resolve(name + "-semantic.txt"), semantic);
            var businessGraph = businessProjector.project(analyses.get(entry.getKey()));
            String businessDiagram = businessMermaid.render(businessGraph);
            new BusinessLogicArtifactGuard().requireClean(businessGraph);
            assert businessGraph.nodes().size() <= graph.nodes().size()
                    : entry.getKey() + " business overview grew: " + businessGraph;
            assert businessGraph.nodes().stream()
                    .anyMatch(node -> node.kind() == at.gepardec.fachtracing.model.BusinessLogicGraph.NodeKind.RESULT)
                    : entry.getKey() + " business overview has no result";
            assertBusinessArtifact(entry.getKey() + " business overview", businessDiagram);
            Files.writeString(generated.resolve(name + "-business.mmd"), businessDiagram);
            Path oracle = oracles.resolve(name + ".txt");
            assert Files.exists(oracle) : "missing reviewed oracle " + oracle;
            if (!Files.readString(oracle).equals(semantic)) topologyMismatches.add(entry.getKey());
            if (REQUIRED_COMPLETE.contains(entry.getKey())) {
                assert graph.completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                        : entry.getKey() + " gaps: " + graph.coverageGaps();
            } else {
                assert graph.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : graph;
                assert graph.coverageGaps().size() == 1 : graph.coverageGaps();
                assert graph.coverageGaps().getFirst().description().contains("called decision logic is unavailable")
                        : graph.coverageGaps();
            }
            graph.nodes().forEach(node -> assertBusinessFacing(entry.getKey(), node.businessLabel()));
            assertBusinessTerminals(entry.getKey(), graph);
            assertBusinessArtifact(entry.getKey(), structure);
            assertBusinessArtifact(entry.getKey() + " Mermaid", mermaidStructure);
            System.out.println(entry.getKey() + ": " + graph.nodes().size() + " nodes, "
                    + graph.edges().size() + " edges, " + graph.completeness());
        }
        assert topologyMismatches.isEmpty() : "semantic topology differs for " + topologyMismatches;
        BusinessDecisionGraph strategyGraph = graphs.get("determine journey warnings");
        assert strategyGraph.nodes().stream().anyMatch(node -> node.kind() == BusinessDecisionGraph.NodeKind.DISPATCH)
                : "manager graph must expose warning strategy dispatch";
        executePolymorphicDecision(
                analyses.get("determine journey warnings"), reference, classpath, generated);
        System.out.println("analyzed " + sources.size() + " source files and " + graphs.size()
                + " decisions in " + elapsedMillis + " ms");
    }

    private static void executePolymorphicDecision(
            AnalysisManifest.AnalysisResult analysis,
            Path reference,
            List<Path> classpath,
            Path generated) throws Exception {
        var urls = new ArrayList<URL>();
        urls.add(reference.resolve("target/classes").toUri().toURL());
        for (Path item : classpath) urls.add(item.toUri().toURL());

        Map<String, String> fingerprints = new LinkedHashMap<>();
        try (var raw = new URLClassLoader(urls.toArray(URL[]::new), MegaBackendConformanceTest.class.getClassLoader())) {
            var owners = new java.util.LinkedHashSet<String>();
            analysis.manifest().probeSites().forEach(site -> owners.add(site.ownerHint()));
            analysis.manifest().dispatchTargets().forEach(target -> owners.add(target.ownerHint()));
            for (String owner : owners) {
                String resource = owner.replace('.', '/') + ".class";
                try (InputStream input = raw.getResourceAsStream(resource)) {
                    assert input != null : "missing runtime class " + owner;
                    fingerprints.put(owner.replace('.', '/'), sha256(input.readAllBytes()));
                }
            }
        }

        var transformer = new FachtracingTransformer(analysis.manifest(), fingerprints);
        var collector = new RuntimeCollector();
        collector.register(analysis.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);

        try (var loader = new TransformingLoader(urls.toArray(URL[]::new), transformer)) {
            Class<?> managerType = loader.loadClass("com.gepardec.mega.service.helper.WarningCalculatorsManager");
            Object manager = managerType.getConstructor().newInstance();
            Object result = managerType.getMethod("determineJourneyWarnings", List.class).invoke(manager, List.of());
            assert result instanceof List<?> list && list.isEmpty() : result;
        }

        var executions = new ArrayList<DecisionExecution>();
        collector.pollCompleted().ifPresent(executions::add);
        while (collector.completedCount() > 0) collector.pollCompleted().ifPresent(executions::add);
        assert executions.size() == 1 : executions;
        DecisionExecution execution = executions.getFirst();
        assert execution.finalResult().type().equals("collection") : execution.finalResult();
        assert execution.finalResult().canonicalValue().equals("[]") : execution.finalResult();
        assert execution.observations().stream().noneMatch(item ->
                item.nodeId().equals(analysis.graph().entryNodeId()) && !item.evidence().isEmpty())
                : execution.observations();
        long selectedEdges = execution.observations().stream()
                .filter(item -> item.selectedEdgeId() != null && item.outcome().equals("selected"))
                .count();
        assert selectedEdges == 3 : "expected all three journey-warning strategies, selected " + selectedEdges;

        var projector = new DecisionExplanationProjector();
        var explanation = projector.project(analysis.graph(), execution);
        assert explanation.completeness() == BusinessDecisionGraph.Completeness.COMPLETE : explanation;
        String text = projector.text(explanation);
        String executionDiagram = new PlantUmlRenderer().execution(analysis.graph(), execution);
        String mermaidExecution = new MermaidRenderer().execution(analysis.graph(), execution);
        var businessExecution = new BusinessExecutionGraphProjector().project(analysis.graph(), execution);
        String businessText = new BusinessExecutionTextRenderer().render(businessExecution);
        String businessDiagram = new BusinessMermaidRenderer().render(businessExecution);
        new BusinessLogicArtifactGuard().requireClean(businessExecution);
        assert businessExecution.nodes().stream()
                .filter(node -> node.kind() == at.gepardec.fachtracing.model.BusinessLogicGraph.NodeKind.RESULT)
                .count() == 1 : businessExecution;
        assert businessExecution.nodes().size() < analysis.graph().nodes().size() : businessExecution;
        assertBusinessArtifact("determine journey warnings explanation", text);
        assertBusinessArtifact("determine journey warnings execution", executionDiagram);
        assertBusinessArtifact("determine journey warnings Mermaid execution", mermaidExecution);
        assertBusinessArtifact("determine journey warnings business text", businessText);
        assertBusinessArtifact("determine journey warnings business diagram", businessDiagram);
        Files.writeString(generated.resolve("determine-journey-warnings-execution.puml"), executionDiagram);
        Files.writeString(generated.resolve("determine-journey-warnings-explanation.txt"), text);
        Files.writeString(generated.resolve("determine-journey-warnings-execution.mmd"), mermaidExecution);
        Files.writeString(generated.resolve("determine-journey-warnings-business-execution.txt"), businessText);
        Files.writeString(generated.resolve("determine-journey-warnings-business-execution.mmd"), businessDiagram);
        System.out.println("captured journey-warning execution with typed empty collection and three selected strategies");
    }

    private static String sha256(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String semanticTopology(BusinessDecisionGraph graph) {
        Map<String, Integer> indices = new LinkedHashMap<>();
        for (int index = 0; index < graph.nodes().size(); index++) {
            indices.put(graph.nodes().get(index).nodeId(), index + 1);
        }
        var text = new StringBuilder("decision: ").append(graph.decisionLabel()).append('\n')
                .append("completeness: ").append(graph.completeness()).append('\n');
        for (int index = 0; index < graph.nodes().size(); index++) {
            var node = graph.nodes().get(index);
            text.append("node ").append(index + 1).append(": ").append(node.kind()).append(" | ")
                    .append(node.businessLabel()).append('\n');
        }
        for (var edge : graph.edges()) {
            text.append("edge ").append(indices.get(edge.fromNodeId())).append(" -> ")
                    .append(indices.get(edge.toNodeId())).append(" | ").append(edge.outcome()).append('\n');
        }
        for (var gap : graph.coverageGaps()) text.append("gap: ").append(gap.description()).append('\n');
        return text.toString();
    }

    private static void assertBusinessFacing(String decision, String label) {
        String lower = label.toLowerCase();
        for (String forbidden : List.of("com.gepardec", ".java", "projectentry", "localdatetime", "gettotime",
                "getfromtime", "objects.requirenonnull", "->", "::", "instanceof", "<>")) {
            assert !lower.contains(forbidden) : decision + " exposes technical label: " + label;
        }
        assert !lower.contains(" stream ") && !lower.endsWith(" stream")
                : decision + " exposes stream plumbing: " + label;
        assert !lower.matches(".*\\b(?:id|ids|null)\\b.*")
                : decision + " exposes implementation vocabulary: " + label;
        assert !label.contains("(") && !label.contains(")") : decision + " exposes call syntax: " + label;
    }

    private static void assertBusinessTerminals(String decision, BusinessDecisionGraph graph) {
        var starts = graph.nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.ENTRY
                        && node.businessLabel().equals("Start"))
                .toList();
        var stops = graph.nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME
                        && node.businessLabel().equals("Stop"))
                .toList();
        assert starts.size() == 1 : decision + " must have exactly one Start: " + starts;
        assert stops.size() == 1 : decision + " must have exactly one Stop: " + stops;
        String stopId = stops.getFirst().nodeId();
        var terminalNodes = graph.nodes().stream()
                .filter(node -> graph.edges().stream().noneMatch(edge -> edge.fromNodeId().equals(node.nodeId())))
                .map(BusinessDecisionGraph.DecisionNode::nodeId)
                .collect(java.util.stream.Collectors.toSet());
        assert terminalNodes.equals(Set.of(stopId))
                : decision + " has paths that do not converge on Stop: " + terminalNodes;
        assert graph.edges().stream().filter(edge -> edge.toNodeId().equals(stopId))
                .allMatch(edge -> edge.outcome().contains("returns") || edge.outcome().equals("fails"))
                : decision + " has an unexplained terminal edge";
    }

    private static void assertBusinessArtifact(String artifact, String content) {
        String lower = content.toLowerCase();
        for (String forbidden : List.of("com.gepardec", ".java", "projectentry", "projecttimeentry",
                "localdatetime", "objects.requirenonnull", "lambda$", "bytecode", "stack frame")) {
            assert !lower.contains(forbidden) : artifact + " exposes technical content: " + forbidden;
        }
        assert !lower.matches("(?s).*\\b(?:id|ids|null)\\b.*")
                : artifact + " exposes identifier/null implementation vocabulary";
    }

    private static String slug(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("^-|-$", "").toLowerCase();
    }

    private static final class TransformingLoader extends URLClassLoader {
        private final FachtracingTransformer transformer;

        private TransformingLoader(URL[] urls, FachtracingTransformer transformer) {
            super(urls, MegaBackendConformanceTest.class.getClassLoader());
            this.transformer = transformer;
        }

        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("com.gepardec.mega.")) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) loaded = findClass(name);
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }

        @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
            String internal = name.replace('.', '/');
            String resource = internal + ".class";
            try (InputStream input = findResource(resource).openStream()) {
                byte[] original = input.readAllBytes();
                byte[] transformed = transformer.transform(null, this, internal, null, null, original);
                byte[] bytes = transformed == null ? original : transformed;
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException error) {
                throw new ClassNotFoundException(name, error);
            }
        }
    }
}
