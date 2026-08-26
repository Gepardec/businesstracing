package at.gepardec.fachtracing.conformance;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.BusinessEntryPoint;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.jakartaee.CdiDispatchTargetSelector;
import at.gepardec.fachtracing.jakartaee.JakartaEeMethodContractProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Executable CDI conformance test for the pinned Jakarta EE REST source corpus. */
public final class JakartaEeRestConformanceTest {
    private static final String REPOSITORY =
            "com.example.infrastructure.persistence.jpa.JpaTaskRepository";

    private JakartaEeRestConformanceTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException(
                "usage: <repository-root> <sample-root> <dependency-classpath-file>");
        Path sample = Path.of(args[1]).toAbsolutePath().normalize();
        List<Path> sources;
        try (var paths = Files.walk(sample.resolve("src/main/java"))) {
            sources = paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        assert sources.size() >= 25 : "expected Jakarta EE source corpus, found " + sources.size();
        var classpath = new ArrayList<Path>();
        classpath.add(Path.of(args[0]).resolve("fachtracing-api/target/classes"));
        classpath.add(sample.resolve("target/classes"));
        for (String item : Files.readString(Path.of(args[2])).split(java.io.File.pathSeparator)) {
            if (!item.isBlank()) classpath.add(Path.of(item));
        }
        List<AnalysisManifest.AnalysisResult> analyses = new StaticDecisionAnalyzer().analyzeAll(
                AnalysisRequest.of(sources, classpath)
                        .withBusinessEntryPoints(List.of(BusinessEntryPoint.of(
                                "com.example.interfaces.task.TaskResources", "allTasks", "list tasks")))
                        .withExternalMethodContractProviders(List.of(new JakartaEeMethodContractProvider()))
                        .withDynamicDispatchTargetSelectors(List.of(new CdiDispatchTargetSelector())));
        var analysis = analyses.stream().filter(result -> result.graph().decisionLabel().equals("list tasks"))
                .findFirst().orElseThrow(() -> new AssertionError("configured endpoint was not analyzed"));
        assert analysis.manifest().dispatchTargets().stream().anyMatch(target ->
                target.ownerHint().equals(REPOSITORY)) : analysis.manifest().dispatchTargets();
        assert analysis.manifest().analysisDecisions().stream().anyMatch(decision ->
                decision.action() == AnalysisManifest.AnalysisAction.INCLUDED
                        && decision.subject().equals(REPOSITORY)) : analysis.manifest().analysisDecisions();
        System.out.println("JAKARTA_EE_REST_CONFORMANCE_OK: " + sources.size() + " source files");
    }
}
