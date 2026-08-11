package at.gepardec.fachtracing.conformance;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.BusinessArtifactGuard;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.business.BusinessGraphJsonExporter;
import at.gepardec.fachtracing.business.BusinessGraphJsonSchema;
import at.gepardec.fachtracing.business.BusinessGraphProjector;
import at.gepardec.fachtracing.business.BusinessLogicArtifactGuard;
import at.gepardec.fachtracing.business.BusinessMermaidRenderer;
import at.gepardec.fachtracing.business.BusinessPlantUmlRenderer;
import at.gepardec.fachtracing.spring.SpringMethodContractProvider;
import at.gepardec.fachtracing.mermaid.MermaidRenderer;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessLogicGraph;
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
            "owner search",
            "visit booking",
            "pet registration");

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
                .analyzeAll(AnalysisRequest.of(sources, classpath)
                        .withExternalMethodContractProviders(List.of(new SpringMethodContractProvider())));
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        Map<String, AnalysisManifest.AnalysisResult> analyses = new LinkedHashMap<>();
        results.stream().filter(result -> !result.graph().coverageGaps().isEmpty()).forEach(result -> {
            System.err.println(result.graph().decisionLabel() + " unresolved analysis:");
            result.graph().coverageGaps().forEach(gap -> System.err.println("  " + gap.description()
                    + " at " + result.manifest().sourceMappings().get(gap.nodeId())));
        });
        results.stream()
                .sorted(Comparator.comparing(result -> result.graph().decisionLabel()))
                .forEach(result -> analyses.put(result.graph().decisionLabel(), result));

        assert analyses.keySet().equals(EXPECTED_DECISIONS) : analyses.keySet();
        Files.createDirectories(generated);
        var plantUml = new PlantUmlRenderer();
        var mermaid = new MermaidRenderer();
        var projector = new BusinessGraphProjector();
        var businessPlantUml = new BusinessPlantUmlRenderer();
        var businessMermaid = new BusinessMermaidRenderer();
        var businessJson = new BusinessGraphJsonExporter();
        String businessJsonSchema = new BusinessGraphJsonSchema().generate();
        Files.writeString(generated.resolve("fachtracing-business-graph-v1.schema.json"),
                businessJsonSchema);
        var oracleMismatches = new ArrayList<String>();
        var index = new StringBuilder("# Spring PetClinic business graphs\n\n");
        for (var entry : analyses.entrySet()) {
            String decision = entry.getKey();
            AnalysisManifest.AnalysisResult analysis = entry.getValue();
            BusinessDecisionGraph graph = analysis.graph();
            String slug = slug(decision);
            String semantic = semanticTopology(graph);
            String plantUmlStructure = plantUml.structure(graph);
            String mermaidStructure = mermaid.structure(graph);
            BusinessLogicGraph business = projector.project(analysis);
            String businessPlantUmlContent = businessPlantUml.render(business);
            String businessMermaidContent = businessMermaid.render(business);
            String businessJsonContent = businessJson.export(business);

            assert new BusinessArtifactGuard().violations(graph).isEmpty()
                    : decision + " technical graph output: " + new BusinessArtifactGuard().violations(graph);
            assertBusinessTerminals(decision, graph);
            assertCompleteness(decision, graph);
            assert business.completeness() == BusinessLogicGraph.Completeness.COMPLETE : business;
            assert business.nodes().stream().noneMatch(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP)
                    : business.nodes();
            new BusinessLogicArtifactGuard().requireClean(business);
            assertBusinessArtifact(decision + " business PlantUML", businessPlantUmlContent);
            assertBusinessArtifact(decision + " business Mermaid", businessMermaidContent);
            assertBusinessArtifact(decision + " business JSON", businessJsonContent);
            assertBusinessResults(decision, business);
            BusinessJsonSchemaConformance.validate(businessJsonContent, businessJsonSchema);

            Files.writeString(generated.resolve(slug + "-business.puml"), businessPlantUmlContent);
            Files.writeString(generated.resolve(slug + "-business.mmd"), businessMermaidContent);
            Files.writeString(generated.resolve(slug + "-business.json"), businessJsonContent);
            Files.writeString(generated.resolve(slug + "-structure.puml"), plantUmlStructure);
            Files.writeString(generated.resolve(slug + "-structure.mmd"), mermaidStructure);
            Files.writeString(generated.resolve(slug + "-semantic.txt"), semantic);
            Path oracle = oracles.resolve(slug + "-business.json");
            if (!Files.exists(oracle) || !Files.readString(oracle).equals(businessJsonContent)) {
                oracleMismatches.add(decision);
            }
            index.append("- **").append(decision).append("** — COMPLETE — ")
                    .append("[Business Mermaid](").append(slug).append("-business.mmd) · ")
                    .append("[Business PlantUML](").append(slug).append("-business.puml) · ")
                    .append("[Business JSON](").append(slug).append("-business.json)\n")
                    .append("  - Technical developer artifacts: [structure Mermaid](")
                    .append(slug).append("-structure.mmd) · [structure PlantUML](")
                    .append(slug).append("-structure.puml)\n");

            System.out.println(decision + ": " + graph.nodes().size() + " nodes, "
                    + graph.edges().size() + " exact edges; " + business.nodes().size()
                    + " business nodes, " + business.completeness());
        }
        Files.writeString(generated.resolve("index.md"), index.toString());
        assert oracleMismatches.isEmpty() : "business JSON differs for " + oracleMismatches;
        System.out.println("analyzed " + sources.size() + " source files and " + analyses.size()
                + " decisions in " + elapsedMillis + " ms");
    }

    private static void assertCompleteness(String decision, BusinessDecisionGraph graph) {
        assert graph.completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                : decision + " gaps: " + graph.coverageGaps();
        assert graph.coverageGaps().isEmpty() : graph.coverageGaps();
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
                "petcontroller", "bindingresult", "bytecode", "stack frame", "redirect:",
                "for each", "derive", "evaluate", "decision result path",
                "alternative result", "unresolved")) {
            assert !lower.contains(forbidden) : artifact + " exposes technical content: " + forbidden;
        }
    }

    private static void assertBusinessResults(String decision, BusinessLogicGraph graph) {
        Set<String> results = graph.nodes().stream()
                .filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT)
                .map(BusinessLogicGraph.Node::label)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> expected = switch (decision) {
            case "owner search" -> Set.of(
                    "no matching records", "one matching record", "multiple matching records");
            case "visit booking" -> Set.of("correction required", "visit booking completed");
            case "pet registration" -> Set.of(
                    "correction required", "operation failed", "pet registration completed");
            default -> throw new IllegalArgumentException("unexpected decision " + decision);
        };
        assert results.equals(expected) : decision + " results: " + results;
        Set<String> terminalNodeIds = graph.nodes().stream()
                .filter(node -> graph.edges().stream().noneMatch(edge -> edge.fromNodeId().equals(node.nodeId())))
                .map(BusinessLogicGraph.Node::nodeId).collect(java.util.stream.Collectors.toSet());
        assert terminalNodeIds.stream().allMatch(nodeId ->
                graph.node(nodeId).kind() == BusinessLogicGraph.NodeKind.RESULT)
                : decision + " non-result terminals: " + terminalNodeIds;
    }

    private static String slug(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("^-|-$", "").toLowerCase();
    }
}
