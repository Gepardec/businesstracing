package at.gepardec.fachtracing.conformance;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.BusinessArtifactGuard;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.mermaid.MermaidRenderer;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.plantuml.PlantUmlRenderer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executable conformance test for an annotation-overlaid Spring PetClinic source tree. */
public final class SpringPetClinicConformanceTest {
    private static final Set<String> EXPECTED_DECISIONS = Set.of(
            "determine whether an entity is new",
            "find an eligible pet by name",
            "register a new pet");
    private static final Set<String> EXPECTED_COMPLETE = Set.of(
            "determine whether an entity is new",
            "find an eligible pet by name");

    private SpringPetClinicConformanceTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "usage: <repository-root> <patched-petclinic-root> <dependency-classpath-file> <generated-dir> <oracle-dir>");
        }
        Path reference = Path.of(args[1]).toAbsolutePath().normalize();
        Path generated = Path.of(args[3]).toAbsolutePath().normalize();
        Path oracles = Path.of(args[4]).toAbsolutePath().normalize();

        List<Path> sources;
        try (var paths = Files.walk(reference.resolve("src/main/java"))) {
            sources = paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        assert sources.size() >= 30 : "expected the full application source corpus, found " + sources.size();

        List<Path> classpath = new ArrayList<>();
        classpath.add(Path.of(args[0]).resolve("fachtracing-api/target/classes"));
        classpath.add(reference.resolve("target/classes"));
        for (String item : Files.readString(Path.of(args[2])).split(java.io.File.pathSeparator)) {
            if (!item.isBlank()) classpath.add(Path.of(item));
        }

        long started = System.nanoTime();
        List<AnalysisManifest.AnalysisResult> results = new StaticDecisionAnalyzer()
                .analyzeAll(AnalysisRequest.of(sources, classpath));
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        Map<String, BusinessDecisionGraph> graphs = new LinkedHashMap<>();
        results.stream()
                .sorted(Comparator.comparing(result -> result.graph().decisionLabel()))
                .forEach(result -> graphs.put(result.graph().decisionLabel(), result.graph()));

        assert graphs.keySet().equals(EXPECTED_DECISIONS) : graphs.keySet();
        Files.createDirectories(generated);
        var plantUml = new PlantUmlRenderer();
        var mermaid = new MermaidRenderer();
        var topologyMismatches = new ArrayList<String>();
        for (var entry : graphs.entrySet()) {
            String decision = entry.getKey();
            BusinessDecisionGraph graph = entry.getValue();
            String slug = slug(decision);
            String semantic = semanticTopology(graph);
            String plantUmlStructure = plantUml.structure(graph);
            String mermaidStructure = mermaid.structure(graph);

            assert new BusinessArtifactGuard().violations(graph).isEmpty()
                    : decision + " technical graph output: " + new BusinessArtifactGuard().violations(graph);
            assertBusinessTerminals(decision, graph);
            assertBusinessArtifact(decision, plantUmlStructure);
            assertBusinessArtifact(decision + " Mermaid", mermaidStructure);
            assertCompleteness(decision, graph);

            Files.writeString(generated.resolve(slug + "-structure.puml"), plantUmlStructure);
            Files.writeString(generated.resolve(slug + "-structure.mmd"), mermaidStructure);
            Files.writeString(generated.resolve(slug + "-semantic.txt"), semantic);
            Path oracle = oracles.resolve(slug + ".txt");
            assert Files.exists(oracle) : "missing reviewed oracle " + oracle;
            if (!Files.readString(oracle).equals(semantic)) topologyMismatches.add(decision);

            System.out.println(decision + ": " + graph.nodes().size() + " nodes, "
                    + graph.edges().size() + " edges, " + graph.completeness());
        }
        assert topologyMismatches.isEmpty() : "semantic topology differs for " + topologyMismatches;
        System.out.println("analyzed " + sources.size() + " source files and " + graphs.size()
                + " decisions in " + elapsedMillis + " ms");
    }

    private static void assertCompleteness(String decision, BusinessDecisionGraph graph) {
        if (EXPECTED_COMPLETE.contains(decision)) {
            assert graph.completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                    : decision + " gaps: " + graph.coverageGaps();
            assert graph.coverageGaps().isEmpty() : graph.coverageGaps();
            return;
        }
        assert graph.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : graph;
        assert graph.coverageGaps().size() == 5 : graph.coverageGaps();
        assert graph.coverageGaps().stream().allMatch(gap -> gap.description().contains("cannot be reconstructed")
                || gap.description().contains("unsupported call")) : graph.coverageGaps();
        assert graph.nodes().stream().filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.COVERAGE_GAP)
                .count() == 5 : graph.nodes();
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
        Set<String> terminalNodes = graph.nodes().stream()
                .filter(node -> graph.edges().stream().noneMatch(edge -> edge.fromNodeId().equals(node.nodeId())))
                .map(BusinessDecisionGraph.DecisionNode::nodeId)
                .collect(java.util.stream.Collectors.toSet());
        assert terminalNodes.equals(Set.of(stopId))
                : decision + " has paths that do not converge on Stop: " + terminalNodes;
    }

    private static void assertBusinessArtifact(String artifact, String content) {
        String lower = content.toLowerCase();
        for (String forbidden : List.of("org.springframework.samples.petclinic", ".java", "baseentity",
                "petcontroller", "bindingresult", "bytecode", "stack frame")) {
            assert !lower.contains(forbidden) : artifact + " exposes technical content: " + forbidden;
        }
        assert !lower.matches("(?s).*\\b(?:id|ids|null)\\b.*")
                : artifact + " exposes identifier or null implementation vocabulary";
    }

    private static String slug(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("^-|-$", "").toLowerCase();
    }
}
