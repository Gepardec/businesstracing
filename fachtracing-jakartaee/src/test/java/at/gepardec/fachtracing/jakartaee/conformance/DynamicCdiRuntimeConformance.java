package at.gepardec.fachtracing.jakartaee.conformance;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.api.FachTracing;
import at.gepardec.fachtracing.jakartaee.CdiDispatchTargetSelector;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.jboss.weld.environment.se.Weld;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds and runs the real-container proof for dynamic CDI dispatch. */
public final class DynamicCdiRuntimeConformance {
    private static final String DECISION = "apply selected regional rule";

    private DynamicCdiRuntimeConformance() { }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 4 && arguments[0].equals("prepare")) {
            prepare(Path.of(arguments[1]), Path.of(arguments[2]), Path.of(arguments[3]));
            return;
        }
        if (arguments.length == 1 && arguments[0].equals("run")) {
            runContainer();
            return;
        }
        throw new IllegalArgumentException("use prepare <source> <classes> <activation> or run");
    }

    private static void prepare(Path source, Path classes, Path activation) throws Exception {
        AnalysisManifest.AnalysisResult result = new StaticDecisionAnalyzer()
                .analyzeAll(AnalysisRequest.of(List.of(source), classpath())
                        .withDynamicDispatchTargetSelectors(List.of(new CdiDispatchTargetSelector())))
                .stream().filter(candidate -> candidate.graph().decisionLabel().equals(DECISION))
                .findFirst().orElseThrow();
        Set<String> targets = result.manifest().dispatchTargets().stream()
                .map(AnalysisManifest.DispatchTarget::ownerHint)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> expected = Set.of(EuropeanRule.class.getName(), AmericanRule.class.getName());
        if (!targets.equals(expected)) {
            throw new AssertionError("dynamic CDI targets differ: " + targets);
        }
        if (result.graph().coverageGaps().stream().noneMatch(gap ->
                gap.description().contains("framework dispatch selection cannot be proved"))) {
            throw new AssertionError("dynamic CDI static gap is absent");
        }
        var bundle = new RuntimeActivationBundle(
                "dynamic-cdi-runtime-conformance",
                "-javaagent:fachtracing-agent.jar",
                fingerprints(classes, result.manifest()),
                List.of(new RuntimeActivationBundle.DecisionDefinition(result.graph(), result.manifest())));
        Files.createDirectories(activation.getParent());
        Files.write(activation, bundle.toJson());
        System.out.println("DYNAMIC_CDI_ACTIVATION_OK: " + targets);
    }

    private static void runContainer() {
        try (var container = new Weld().disableDiscovery().addBeanClasses(
                DynamicCdiWorkflow.class, Region.class, RegionLiteral.class,
                EuropeanRule.class, AmericanRule.class).initialize()) {
            DynamicCdiWorkflow workflow = container.select(DynamicCdiWorkflow.class).get();
            if (!workflow.apply("EU", 15)) throw new AssertionError("EU bean was not selected");
            if (workflow.apply("US", 15)) throw new AssertionError("US bean was not selected");
        }
        System.out.println("DYNAMIC_CDI_CONTAINER_OK");
    }

    private static List<Path> classpath() {
        return Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator))
                .map(Path::of).toList();
    }

    private static java.util.Map<String, String> fingerprints(
            Path classes, AnalysisManifest manifest) throws Exception {
        var owners = new LinkedHashSet<String>();
        manifest.probeSites().forEach(site -> owners.add(site.ownerHint()));
        manifest.dispatchTargets().forEach(target -> owners.add(target.ownerHint()));
        manifest.branchTargets().forEach(target -> owners.add(target.ownerHint()));
        manifest.controlTargets().forEach(target -> owners.add(target.ownerHint()));
        manifest.evidenceTargets().forEach(target -> owners.add(target.ownerHint()));
        var result = new LinkedHashMap<String, String>();
        for (String owner : owners) {
            String internalName = owner.replace('.', '/');
            Path bytecode = classes.resolve(internalName + ".class");
            result.put(internalName, java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(bytecode))));
        }
        return result;
    }
}

@Dependent
class DynamicCdiWorkflow {
    @Inject @Any Instance<RegionalRule> rules;

    @FachTracing("apply selected regional rule")
    boolean apply(String region, int value) {
        return rules.select(new RegionLiteral(region)).get().accepts(value);
    }
}

interface RegionalRule {
    boolean accepts(int value);
}

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@interface Region {
    String value();
}

final class RegionLiteral extends AnnotationLiteral<Region> implements Region {
    private final String value;

    RegionLiteral(String value) { this.value = value; }

    @Override public String value() { return value; }
}

@ApplicationScoped
@Region("EU")
class EuropeanRule implements RegionalRule {
    public boolean accepts(int value) { return value > 10; }
}

@ApplicationScoped
@Region("US")
class AmericanRule implements RegionalRule {
    public boolean accepts(int value) { return value > 20; }
}
