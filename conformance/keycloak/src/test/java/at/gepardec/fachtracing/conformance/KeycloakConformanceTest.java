package at.gepardec.fachtracing.conformance;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.BusinessArtifactGuard;
import at.gepardec.fachtracing.analysis.BusinessEntryPoint;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.business.BusinessGraphProjector;
import at.gepardec.fachtracing.business.BusinessExecutionGraphProjector;
import at.gepardec.fachtracing.business.BusinessLogicArtifactGuard;
import at.gepardec.fachtracing.business.BusinessMermaidRenderer;
import at.gepardec.fachtracing.developer.DecisionAuditMermaidRenderer;
import at.gepardec.fachtracing.model.BusinessLogicGraph;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/** Generates the pinned Keycloak user-search business graph and runtime activation. */
public final class KeycloakConformanceTest {
    private static final String OWNER = "org.keycloak.services.resources.admin.UsersResource";

    private KeycloakConformanceTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: <users-resource-source> <services-classes> <classpath-file> <output-directory>");
        }
        Path source = Path.of(args[0]).toAbsolutePath().normalize();
        Path servicesClasses = Path.of(args[1]).toAbsolutePath().normalize();
        Path output = Path.of(args[3]).toAbsolutePath().normalize();
        List<Path> classpath = new ArrayList<>();
        classpath.add(servicesClasses);
        for (String item : Files.readString(Path.of(args[2])).split(java.io.File.pathSeparator)) {
            if (!item.isBlank()) classpath.add(Path.of(item).toAbsolutePath().normalize());
        }

        String sourceText = Files.readString(source);
        assert sourceText.contains("Stream<UserRepresentation> getUsers(") : "pinned endpoint method changed";
        assert !sourceText.contains("at.gepardec.fachtracing") : "Keycloak source was modified for tracing";

        var request = AnalysisRequest.of(List.of(source), classpath)
                .withBusinessEntryPoints(List.of(BusinessEntryPoint.of(OWNER, "getUsers", "search users")));
        AnalysisManifest.AnalysisResult analysis = new StaticDecisionAnalyzer().analyze(request);
        assert analysis.graph().decisionLabel().equals("search users") : analysis.graph();
        assert analysis.manifest().probeSites().stream().anyMatch(site ->
                site.kind() == AnalysisManifest.ProbeKind.ENTRY
                        && site.ownerHint().equals(OWNER)
                        && site.memberHint().equals("getUsers")) : analysis.manifest().probeSites();
        assert new BusinessArtifactGuard().violations(analysis.graph()).isEmpty()
                : new BusinessArtifactGuard().violations(analysis.graph());

        var businessProjector = new BusinessGraphProjector();
        var projectionAudit = businessProjector.projectWithAudit(analysis);
        var fullBusinessGraph = projectionAudit.graph();
        new BusinessLogicArtifactGuard().requireClean(fullBusinessGraph);
        List<String> exactLabels = analysis.graph().nodes().stream()
                .map(node -> node.businessLabel().toLowerCase(java.util.Locale.ROOT)).toList();
        for (String required : List.of(
                "search query is absent", "search exists", "prefix exists", "last exists",
                "first exists", "email exists", "username exists", "created after exists",
                "created before exists", "admin permissions disabled for realm")) {
            assert exactLabels.contains(required) : "reviewed flow anchor is absent: " + required;
        }
        assert exactLabels.stream().noneMatch(label -> label.contains("schema")) : exactLabels;
        List<String> businessLabels = fullBusinessGraph.nodes().stream()
                .map(node -> node.label().toLowerCase(java.util.Locale.ROOT)).toList();
        for (String required : List.of("search query is absent", "search exists", "prefix exists")) {
            assert businessLabels.contains(required) : "projected business anchor is absent: " + required;
        }
        assert fullBusinessGraph.nodes().stream()
                .anyMatch(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP)
                : "projected business graph must identify incomplete coverage";
        assert fullBusinessGraph.nodes().size() < analysis.graph().nodes().size()
                : "the generated overview did not reduce the exact graph";
        for (String required : List.of("search query is absent", "search exists", "prefix exists")) {
            String nodeId = analysis.graph().nodes().stream()
                    .filter(node -> node.businessLabel().equalsIgnoreCase(required))
                    .map(node -> node.nodeId())
                    .findFirst().orElseThrow();
            assert analysis.manifest().branchTargets().stream().anyMatch(target -> target.nodeId().equals(nodeId))
                    : "runtime branch binding is absent for " + required;
        }
        String businessDiagram = new BusinessMermaidRenderer().render(fullBusinessGraph);
        assert businessDiagram.startsWith("flowchart LR\n") : businessDiagram;
        assert !businessDiagram.contains("org.keycloak") : businessDiagram;
        assert !businessDiagram.contains("UsersResource") : businessDiagram;
        assert !businessDiagram.contains(".java") : businessDiagram;
        var auditRenderer = new DecisionAuditMermaidRenderer();
        String analysisAudit = auditRenderer.analysis(analysis);
        String projectionAuditDiagram = auditRenderer.projection(projectionAudit);
        assert analysisAudit.equals(auditRenderer.analysis(analysis)) : analysisAudit;
        assert projectionAuditDiagram.equals(auditRenderer.projection(projectionAudit))
                : projectionAuditDiagram;
        assert analysisAudit.contains("UsersResource.java") : analysisAudit;
        assert analysisAudit.contains("INCLUDED /") : analysisAudit;
        assert analysisAudit.contains("Exact result-relevant graph") : analysisAudit;
        assert analysisAudit.contains("PREDICATE") : analysisAudit;
        for (String required : List.of(
                "REMOVED / STRUCTURAL_ENTRY",
                "REMOVED / TECHNICAL_CALCULATION",
                "KEPT / BUSINESS_RULE",
                "KEPT / COVERAGE_GAP",
                "REPLACED / TERMINAL_RESULT")) {
            assert projectionAuditDiagram.contains(required)
                    : "projection audit category is absent: " + required + "\n" + projectionAuditDiagram;
        }

        DecisionExecution evaluatedExecution = successfulExecution(analysis.graph());
        BusinessLogicGraph evaluatedFlow = new BusinessExecutionGraphProjector()
                .project(analysis.graph(), evaluatedExecution);
        new BusinessLogicArtifactGuard().requireClean(evaluatedFlow);
        assert evaluatedFlow.nodes().size() < fullBusinessGraph.nodes().size() : evaluatedFlow;
        assert evaluatedFlow.nodes().size() <= 15 : "evaluated example is not concise: " + evaluatedFlow;
        assert evaluatedFlow.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT)
                .count() == 1 : evaluatedFlow;
        String evaluatedDiagram = new BusinessMermaidRenderer().render(evaluatedFlow);
        assert evaluatedDiagram.contains("search users") : evaluatedDiagram;
        assert !evaluatedDiagram.contains("org.keycloak") : evaluatedDiagram;
        assert !evaluatedDiagram.contains("UsersResource") : evaluatedDiagram;
        assert !evaluatedDiagram.contains(".java") : evaluatedDiagram;

        Files.createDirectories(output);
        Files.writeString(output.resolve("search-users-business.mmd"), businessDiagram);
        Files.writeString(output.resolve("search-users-evaluated-example.mmd"), evaluatedDiagram);
        Files.writeString(output.resolve("search-users-analysis-audit.mmd"), analysisAudit);
        Files.writeString(output.resolve("search-users-projection-audit.mmd"), projectionAuditDiagram);
        var bundle = new RuntimeActivationBundle(
                "keycloak-eba869ee597b933efc8fa2c84713db9e6c0983cf",
                "-javaagent:fachtracing-agent-0.1.0-rc.1.jar",
                classFingerprints(analysis.manifest(), classpath),
                List.of(new RuntimeActivationBundle.DecisionDefinition(
                        analysis.graph(), analysis.manifest())));
        Files.write(output.resolve("activation.json"), bundle.toJson());
        System.out.println("KEYCLOAK_BUSINESS_TRACE_READY " + output);
        System.out.println("search users: " + analysis.graph().nodes().size() + " exact nodes, "
                + fullBusinessGraph.nodes().size() + " overview nodes, "
                + evaluatedFlow.nodes().size() + " evaluated nodes, "
                + analysis.graph().completeness());
    }

    private static DecisionExecution successfulExecution(BusinessDecisionGraph graph) {
        List<BusinessDecisionGraph.DecisionEdge> path = shortestSuccessfulPath(graph);
        var observations = new ArrayList<DecisionExecution.NodeObservation>();
        long sequence = 0;
        for (BusinessDecisionGraph.DecisionEdge edge : path) {
            observations.add(new DecisionExecution.NodeObservation(
                    sequence++, edge.fromNodeId(), edge.outcome(), Map.of(), edge.edgeId()));
        }
        observations.add(new DecisionExecution.NodeObservation(
                sequence, path.getLast().toNodeId(), "completed", Map.of(), null));
        return new DecisionExecution(
                "generated-keycloak-example", graph.graphId(), graph.version(),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), observations,
                new DecisionExecution.DecisionValue("status", "COMPLETED", "Completed"),
                graph.completeness(),
                graph.coverageGaps().stream().map(BusinessDecisionGraph.CoverageGap::description).toList());
    }

    private static List<BusinessDecisionGraph.DecisionEdge> shortestSuccessfulPath(
            BusinessDecisionGraph graph) {
        var nodes = new HashMap<String, BusinessDecisionGraph.DecisionNode>();
        graph.nodes().forEach(node -> nodes.put(node.nodeId(), node));
        var queue = new ArrayDeque<String>();
        var seen = new java.util.HashSet<String>();
        var predecessor = new HashMap<String, BusinessDecisionGraph.DecisionEdge>();
        queue.add(graph.entryNodeId());
        seen.add(graph.entryNodeId());
        String terminal = null;
        while (!queue.isEmpty() && terminal == null) {
            String current = queue.removeFirst();
            for (BusinessDecisionGraph.DecisionEdge edge : graph.edges().stream()
                    .filter(candidate -> candidate.fromNodeId().equals(current)).toList()) {
                BusinessDecisionGraph.DecisionNode target = nodes.get(edge.toNodeId());
                if (target != null && target.kind() == BusinessDecisionGraph.NodeKind.OUTCOME
                        && !edge.outcome().toLowerCase(java.util.Locale.ROOT).contains("fails")) {
                    predecessor.put(edge.toNodeId(), edge);
                    terminal = edge.toNodeId();
                    break;
                }
                if (target != null && target.kind() == BusinessDecisionGraph.NodeKind.OUTCOME) continue;
                if (seen.add(edge.toNodeId())) {
                    predecessor.put(edge.toNodeId(), edge);
                    queue.addLast(edge.toNodeId());
                }
            }
        }
        if (terminal == null) throw new AssertionError("generated graph has no successful terminal path");
        var reversed = new ArrayList<BusinessDecisionGraph.DecisionEdge>();
        for (String nodeId = terminal; !nodeId.equals(graph.entryNodeId()); ) {
            BusinessDecisionGraph.DecisionEdge edge = predecessor.get(nodeId);
            if (edge == null) throw new AssertionError("generated terminal path is disconnected");
            reversed.add(edge);
            nodeId = edge.fromNodeId();
        }
        Collections.reverse(reversed);
        return List.copyOf(reversed);
    }

    private static Map<String, String> classFingerprints(
            AnalysisManifest manifest,
            List<Path> classpath) throws Exception {
        var owners = new LinkedHashSet<String>();
        manifest.probeSites().forEach(site -> owners.add(site.ownerHint()));
        manifest.dispatchTargets().forEach(target -> owners.add(target.ownerHint()));
        manifest.branchTargets().forEach(target -> owners.add(target.ownerHint()));
        manifest.controlTargets().forEach(target -> owners.add(target.ownerHint()));
        manifest.evidenceTargets().forEach(target -> owners.add(target.ownerHint()));
        var fingerprints = new java.util.LinkedHashMap<String, String>();
        for (String owner : owners) {
            byte[] bytes = classBytes(owner, classpath);
            fingerprints.put(owner.replace('.', '/'), sha256(bytes));
        }
        return Map.copyOf(fingerprints);
    }

    private static byte[] classBytes(String owner, List<Path> classpath) throws IOException {
        String resource = owner.replace('.', '/') + ".class";
        for (Path entry : classpath) {
            if (Files.isDirectory(entry)) {
                Path file = entry.resolve(resource);
                if (Files.isRegularFile(file)) return Files.readAllBytes(file);
            } else if (Files.isRegularFile(entry) && entry.toString().endsWith(".jar")) {
                try (var archive = new JarFile(entry.toFile())) {
                    var item = archive.getJarEntry(resource);
                    if (item != null) {
                        try (var input = archive.getInputStream(item)) {
                            return input.readAllBytes();
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("compiled class not found for selected owner: " + owner);
    }

    private static String sha256(byte[] value) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }
}
