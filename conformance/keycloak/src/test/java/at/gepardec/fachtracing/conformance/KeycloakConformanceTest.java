package at.gepardec.fachtracing.conformance;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.BusinessArtifactGuard;
import at.gepardec.fachtracing.analysis.BusinessEntryPoint;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.business.BusinessGraphProjector;
import at.gepardec.fachtracing.business.BusinessMermaidRenderer;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;
import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
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

        var fullBusinessGraph = new BusinessGraphProjector().project(analysis);
        new at.gepardec.fachtracing.business.BusinessLogicArtifactGuard().requireClean(fullBusinessGraph);
        List<String> exactLabels = analysis.graph().nodes().stream()
                .map(node -> node.businessLabel().toLowerCase(java.util.Locale.ROOT)).toList();
        for (String required : List.of(
                "search query is absent", "search exists", "prefix exists", "last exists",
                "first exists", "email exists", "username exists", "created after exists",
                "created before exists", "admin permissions disabled for realm")) {
            assert exactLabels.contains(required) : "reviewed flow anchor is absent: " + required;
        }
        assert exactLabels.stream().noneMatch(label -> label.contains("schema")) : exactLabels;
        for (String required : List.of("search query is absent", "search exists", "prefix exists")) {
            String nodeId = analysis.graph().nodes().stream()
                    .filter(node -> node.businessLabel().equalsIgnoreCase(required))
                    .map(node -> node.nodeId())
                    .findFirst().orElseThrow();
            assert analysis.manifest().branchTargets().stream().anyMatch(target -> target.nodeId().equals(nodeId))
                    : "runtime branch binding is absent for " + required;
        }
        String businessDiagram = new BusinessMermaidRenderer().render(reviewedOverview());
        assert businessDiagram.startsWith("flowchart LR\n") : businessDiagram;
        assert !businessDiagram.contains("org.keycloak") : businessDiagram;
        assert !businessDiagram.contains("UsersResource") : businessDiagram;
        assert !businessDiagram.contains(".java") : businessDiagram;

        Files.createDirectories(output);
        Files.writeString(output.resolve("search-users-business.mmd"), businessDiagram);
        var bundle = new RuntimeActivationBundle(
                "keycloak-eba869ee597b933efc8fa2c84713db9e6c0983cf",
                "-javaagent:fachtracing-agent-0.1.0-rc.1.jar",
                classFingerprints(analysis.manifest(), classpath),
                List.of(new RuntimeActivationBundle.DecisionDefinition(
                        analysis.graph(), analysis.manifest())));
        Files.write(output.resolve("activation.json"), bundle.toJson());
        System.out.println("KEYCLOAK_BUSINESS_TRACE_READY " + output);
        System.out.println("search users: " + analysis.graph().nodes().size() + " exact nodes, "
                + analysis.graph().completeness());
    }

    private static BusinessLogicGraph reviewedOverview() {
        var nodes = List.of(
                overviewNode("permission", BusinessLogicGraph.NodeKind.RULE,
                        "the caller can query users"),
                overviewNode("general", BusinessLogicGraph.NodeKind.RULE,
                        "a general search was supplied"),
                overviewNode("prefix", BusinessLogicGraph.NodeKind.RULE,
                        "the general search uses prefix terms"),
                overviewNode("details", BusinessLogicGraph.NodeKind.RULE,
                        "one or more detailed filters were supplied"),
                overviewNode("term-search", BusinessLogicGraph.NodeKind.ACTION,
                        "search matching prefix terms"),
                overviewNode("filter-search", BusinessLogicGraph.NodeKind.ACTION,
                        "search by supplied filters"),
                overviewNode("list", BusinessLogicGraph.NodeKind.ACTION,
                        "list users"),
                overviewNode("visible", BusinessLogicGraph.NodeKind.RULE,
                        "each returned user is visible to the caller"),
                overviewNode("permitted", BusinessLogicGraph.NodeKind.RESULT,
                        "permitted users"),
                overviewNode("forbidden", BusinessLogicGraph.NodeKind.RESULT,
                        "request forbidden"),
                overviewNode("gap", BusinessLogicGraph.NodeKind.GAP,
                        "some authorization and storage rules are outside this source"));
        var edges = List.of(
                overviewEdge("permission", "general", "yes"),
                overviewEdge("permission", "forbidden", "no"),
                overviewEdge("general", "prefix", "yes"),
                overviewEdge("general", "details", "no"),
                overviewEdge("prefix", "term-search", "yes"),
                overviewEdge("prefix", "filter-search", "no"),
                overviewEdge("details", "filter-search", "yes"),
                overviewEdge("details", "list", "no"),
                overviewEdge("term-search", "visible", ""),
                overviewEdge("filter-search", "visible", ""),
                overviewEdge("list", "visible", ""),
                overviewEdge("visible", "permitted", "yes"),
                overviewEdge("visible", "gap", "no"));
        return new BusinessLogicGraph(
                "keycloak-user-search-overview", 1, "search users", List.of("permission"),
                nodes, edges, BusinessLogicGraph.Completeness.INCOMPLETE);
    }

    private static BusinessLogicGraph.Node overviewNode(
            String id, BusinessLogicGraph.NodeKind kind, String label) {
        return new BusinessLogicGraph.Node(id, kind, label);
    }

    private static BusinessLogicGraph.Edge overviewEdge(
            String from, String to, String outcome) {
        return new BusinessLogicGraph.Edge(from + "-" + to + "-" + outcome, from, to, outcome);
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
