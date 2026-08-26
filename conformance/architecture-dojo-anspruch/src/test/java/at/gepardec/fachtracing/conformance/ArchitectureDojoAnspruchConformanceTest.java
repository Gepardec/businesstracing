package at.gepardec.fachtracing.conformance;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.BusinessArtifactGuard;
import at.gepardec.fachtracing.analysis.BusinessEntryPoint;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.business.BusinessGraphJsonExporter;
import at.gepardec.fachtracing.business.BusinessGraphJsonSchema;
import at.gepardec.fachtracing.business.BusinessGraphProjector;
import at.gepardec.fachtracing.business.BusinessLogicArtifactGuard;
import at.gepardec.fachtracing.business.BusinessMermaidRenderer;
import at.gepardec.fachtracing.business.BusinessPlantUmlRenderer;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Generates business graphs from the pinned Architecture Dojo Anspruch solution. */
public final class ArchitectureDojoAnspruchConformanceTest {
    private static final String ENTITLEMENT = "check benefit entitlement";
    private static final String NOTIFICATION = "submit incapacity notification";
    private static final Set<String> EXPECTED_DECISIONS = Set.of(ENTITLEMENT, NOTIFICATION);

    private ArchitectureDojoAnspruchConformanceTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "usage: <checkout-root> <dependency-classpath-file> <generated-directory>");
        }
        Path checkout = Path.of(args[0]).toAbsolutePath().normalize();
        Path generated = Path.of(args[2]).toAbsolutePath().normalize();

        List<Path> sources = new ArrayList<>();
        collectSources(checkout.resolve("dojo-external-domains/src/main/java"), sources);
        collectSources(checkout.resolve("dojo-leistung/src/main/java"), sources);
        sources.sort(Comparator.naturalOrder());
        assert sources.size() >= 35 : "expected the complete Onion source corpus: " + sources.size();

        List<Path> classpath = new ArrayList<>();
        classpath.add(checkout.resolve("dojo-external-domains/target/classes"));
        classpath.add(checkout.resolve("dojo-leistung/target/classes"));
        for (String item : Files.readString(Path.of(args[1])).split(java.io.File.pathSeparator)) {
            if (!item.isBlank()) classpath.add(Path.of(item).toAbsolutePath().normalize());
        }

        var entries = List.of(
                new BusinessEntryPoint(
                        "at.gepardec.dojo.leistung.anspruch.infrastructure.AnspruchWebCheck",
                        "pruefe", List.of("java.lang.String"), ENTITLEMENT),
                new BusinessEntryPoint(
                        "at.gepardec.dojo.leistung.au.application.ErstelleAuMeldungService",
                        "erstelleAuMeldung", List.of("java.lang.String", "java.time.LocalDate"),
                        NOTIFICATION));

        long started = System.nanoTime();
        List<AnalysisManifest.AnalysisResult> results = new StaticDecisionAnalyzer().analyzeAll(
                AnalysisRequest.of(sources, classpath).withBusinessEntryPoints(entries));
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        Map<String, AnalysisManifest.AnalysisResult> analyses = new LinkedHashMap<>();
        results.stream().sorted(Comparator.comparing(result -> result.graph().decisionLabel()))
                .forEach(result -> analyses.put(result.graph().decisionLabel(), result));
        assert analyses.keySet().equals(EXPECTED_DECISIONS) : analyses.keySet();

        Files.createDirectories(generated);
        String schema = new BusinessGraphJsonSchema().generate();
        Files.writeString(generated.resolve("fachtracing-business-graph-v1.schema.json"), schema);
        var projector = new BusinessGraphProjector();
        var jsonExporter = new BusinessGraphJsonExporter();
        var mermaidRenderer = new BusinessMermaidRenderer();
        var plantUmlRenderer = new BusinessPlantUmlRenderer();

        for (Map.Entry<String, AnalysisManifest.AnalysisResult> entry : analyses.entrySet()) {
            String decision = entry.getKey();
            AnalysisManifest.AnalysisResult analysis = entry.getValue();
            BusinessDecisionGraph exact = analysis.graph();
            if (!exact.coverageGaps().isEmpty()) {
                System.err.println(decision + " coverage gaps:");
                exact.coverageGaps().forEach(gap -> System.err.println("  " + gap.description()));
            }
            assert exact.completeness() == BusinessDecisionGraph.Completeness.COMPLETE : exact.coverageGaps();
            assert new BusinessArtifactGuard().violations(exact).isEmpty()
                    : new BusinessArtifactGuard().violations(exact);

            BusinessLogicGraph business = projector.project(analysis);
            assert business.completeness() == BusinessLogicGraph.Completeness.COMPLETE : business;
            assert business.nodes().stream().noneMatch(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP)
                    : business.nodes();
            new BusinessLogicArtifactGuard().requireClean(business);
            String json = jsonExporter.export(business);
            String mermaid = mermaidRenderer.render(business);
            String plantUml = plantUmlRenderer.render(business);
            assertNoDeveloperTerms(decision, json, mermaid, plantUml);
            BusinessJsonSchemaConformance.validate(json, schema);

            String slug = slug(decision);
            Files.writeString(generated.resolve(slug + "-business.json"), json);
            Files.writeString(generated.resolve(slug + "-business.mmd"), mermaid);
            Files.writeString(generated.resolve(slug + "-business.puml"), plantUml);
            System.out.println(decision + ": " + exact.nodes().size() + " exact nodes, "
                    + exact.edges().size() + " exact edges; " + business.nodes().size()
                    + " business nodes, " + business.edges().size() + " business edges, "
                    + business.completeness());
        }
        System.out.println("ARCHITECTURE_DOJO_ANSPRUCH_GRAPHS_READY " + generated);
        System.out.println("analyzed " + sources.size() + " source files and " + analyses.size()
                + " decisions in " + elapsedMillis + " ms");
    }

    private static void collectSources(Path root, List<Path> sources) throws Exception {
        try (var paths = Files.walk(root)) {
            sources.addAll(paths.filter(path -> path.toString().endsWith(".java")).toList());
        }
    }

    private static void assertNoDeveloperTerms(String decision, String... artifacts) {
        for (String artifact : artifacts) {
            String lower = artifact.toLowerCase(java.util.Locale.ROOT);
            for (String forbidden : List.of(
                    "at.gepardec.dojo", ".java", "anspruchwebcheck", "erstelleaumeldungservice",
                    "bytecode", "stack frame", "unresolved")) {
                assert !lower.contains(forbidden)
                        : decision + " exposes developer content: " + forbidden;
            }
        }
    }

    private static String slug(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("^-|-$", "").toLowerCase();
    }
}
