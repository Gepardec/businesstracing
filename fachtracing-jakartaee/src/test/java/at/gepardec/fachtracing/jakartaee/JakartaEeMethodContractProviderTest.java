package at.gepardec.fachtracing.jakartaee;

import at.gepardec.fachtracing.analysis.ExternalMethodContract;
import at.gepardec.fachtracing.analysis.ExternalMethodContractProviders;
import at.gepardec.fachtracing.analysis.ExternalMethodReference;
import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.analysis.SourceSemanticProviders;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Executable signature contracts for the optional Jakarta EE adapter. */
public final class JakartaEeMethodContractProviderTest {
    private JakartaEeMethodContractProviderTest() { }

    public static void main(String[] args) {
        var providers = ExternalMethodContractProviders.load(
                JakartaEeMethodContractProviderTest.class.getClassLoader());
        assert providers.stream().map(provider -> provider.providerId()).toList()
                .contains("jakartaee:platform") : providers;
        for (ExternalMethodContract contract : new JakartaEeMethodContractProvider().contracts()) {
            assert methodExists(contract.method()) : contract.method();
        }
        assert new JakartaEeMethodContractProvider().contracts().stream().anyMatch(contract ->
                contract.method().ownerBinaryName().equals("jakarta.xml.ws.Service"));
        assert new JakartaEeMethodContractProvider().contracts().stream().anyMatch(contract ->
                contract.method().ownerBinaryName().equals("io.grpc.ManagedChannel"));
        assert SourceSemanticProviders.load(JakartaEeMethodContractProviderTest.class.getClassLoader())
                .stream().map(provider -> provider.providerId()).toList()
                .contains("jakartaee:source-semantics");
        selectsOnlyScopedNonAlternativeCdiBeans();
        appliesImplicitDefaultQualifier();
        resolvesConstructorInjectionAndQualifierValues();
        resolvesCustomScopesStereotypesAndPriorityAlternatives();
        exposesUnprovedCdiResolution();
        exposesContainerAndContractGaps();
    }

    private static void selectsOnlyScopedNonAlternativeCdiBeans() {
        var result = analyzeCdi("apply CDI rule");
        assert result.manifest().dispatchTargets().stream().map(AnalysisManifest.DispatchTarget::ownerHint)
                .toList().equals(List.of("fixtures.jakartaee.ScopedRule"))
                : result.manifest().dispatchTargets();
        assert result.manifest().analysisDecisions().stream().anyMatch(decision ->
                decision.reason() == AnalysisManifest.AnalysisReason.FRAMEWORK_EXCLUDED_IMPLEMENTATION
                        && decision.subject().equals("fixtures.jakartaee.PlainRule"));
        assert result.manifest().analysisDecisions().stream().anyMatch(decision ->
                decision.reason() == AnalysisManifest.AnalysisReason.FRAMEWORK_EXCLUDED_IMPLEMENTATION
                        && decision.subject().equals("fixtures.jakartaee.AlternativeRule"));
        assert result.manifest().analysisDecisions().stream().anyMatch(decision ->
                decision.reason() == AnalysisManifest.AnalysisReason.FRAMEWORK_EXCLUDED_IMPLEMENTATION
                        && decision.subject().equals("fixtures.jakartaee.OtherScopedRule"));
    }

    private static void appliesImplicitDefaultQualifier() {
        var result = analyzeCdi("apply default CDI rule");
        assert result.manifest().dispatchTargets().stream().map(AnalysisManifest.DispatchTarget::ownerHint)
                .toList().equals(List.of("fixtures.jakartaee.OtherScopedRule"))
                : result.manifest().dispatchTargets();
    }

    private static void resolvesConstructorInjectionAndQualifierValues() {
        var result = analyzeCdi("apply constructor CDI rule");
        assert result.manifest().dispatchTargets().stream().map(AnalysisManifest.DispatchTarget::ownerHint)
                .toList().equals(List.of("fixtures.jakartaee.EuropeanRule"))
                : result.manifest().dispatchTargets();
        assert result.manifest().analysisDecisions().stream().anyMatch(decision ->
                decision.reason() == AnalysisManifest.AnalysisReason.FRAMEWORK_EXCLUDED_IMPLEMENTATION
                        && decision.subject().equals("fixtures.jakartaee.AmericanRule"));
    }

    private static void resolvesCustomScopesStereotypesAndPriorityAlternatives() {
        assert selectedOwners("apply custom-scope CDI rule")
                .equals(List.of("fixtures.jakartaee.CustomScopedRule"));
        assert selectedOwners("apply stereotype CDI rule")
                .equals(List.of("fixtures.jakartaee.StereotypeRule"));
        assert selectedOwners("apply priority alternative CDI rule")
                .equals(List.of("fixtures.jakartaee.PriorityAlternativeRule"));
    }

    private static void exposesUnprovedCdiResolution() {
        for (String label : List.of(
                "apply XML alternative CDI rule",
                "apply dynamic CDI rule",
                "apply dynamic CDI provider rule")) {
            var result = analyzeCdi(label);
            assert result.graph().coverageGaps().stream().anyMatch(gap ->
                    gap.description().contains("framework dispatch selection cannot be proved"))
                    : result.graph().coverageGaps();
        }
        assert selectedOwners("apply XML alternative CDI rule").isEmpty();
        List<String> dynamicOwners = selectedOwners("apply dynamic CDI rule");
        assert dynamicOwners.contains("fixtures.jakartaee.EuropeanRule") : dynamicOwners;
        assert dynamicOwners.contains("fixtures.jakartaee.AmericanRule") : dynamicOwners;
        assert selectedOwners("apply dynamic CDI provider rule").equals(dynamicOwners);
    }

    private static void exposesContainerAndContractGaps() {
        Path fixture = Path.of("fachtracing-jakartaee/src/test/resources/fixtures/ContainerWorkflow.java");
        var result = new StaticDecisionAnalyzer().analyze(
                AnalysisRequest.of(List.of(fixture), classpath())
                        .withExternalMethodContractProviders(List.of(new JakartaEeMethodContractProvider()))
                        .withSourceSemanticProviders(List.of(new JakartaEeSourceSemanticProvider())));
        List<String> gaps = result.graph().coverageGaps().stream()
                .map(gap -> gap.description()).toList();
        assert gaps.stream().anyMatch(gap -> gap.contains("transaction interceptor")) : gaps;
        assert gaps.stream().anyMatch(gap -> gap.contains("container security")) : gaps;
        assert gaps.stream().anyMatch(gap -> gap.contains("Bean Validation constraint")) : gaps;
        assert gaps.stream().anyMatch(gap -> gap.contains("JPA lifecycle callbacks")) : gaps;
        assert gaps.stream().anyMatch(gap -> gap.contains("interceptor binding")) : gaps;
        assert gaps.stream().anyMatch(gap -> gap.contains("EJB timer")) : gaps;
        assert gaps.stream().anyMatch(gap -> gap.contains("CDI event delivery")) : gaps;
        assert gaps.stream().anyMatch(gap -> gap.contains("JAX-RS filters")) : gaps;
        assert result.graph().nodes().stream()
                .anyMatch(node -> node.businessLabel().equals("store entity"));
    }

    private static List<String> selectedOwners(String label) {
        return analyzeCdi(label).manifest().dispatchTargets().stream()
                .map(AnalysisManifest.DispatchTarget::ownerHint).toList();
    }

    private static AnalysisManifest.AnalysisResult analyzeCdi(String label) {
        Path fixture = Path.of("fachtracing-jakartaee/src/test/resources/fixtures/CdiWorkflow.java");
        return new StaticDecisionAnalyzer().analyzeAll(AnalysisRequest.of(List.of(fixture), classpath())
                        .withDynamicDispatchTargetSelectors(List.of(new CdiDispatchTargetSelector())))
                .stream().filter(candidate -> candidate.graph().decisionLabel().equals(label))
                .findFirst().orElseThrow();
    }

    private static List<Path> classpath() {
        return Arrays.stream(System.getProperty("java.class.path")
                .split(java.io.File.pathSeparator)).map(Path::of).toList();
    }

    private static boolean methodExists(ExternalMethodReference reference) {
        try {
            Class<?> owner = Class.forName(reference.ownerBinaryName());
            for (Method method : owner.getMethods()) {
                if (method.getName().equals(reference.methodName())
                        && descriptor(method).equals(reference.descriptor())) return true;
            }
            return false;
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("contract owner is unavailable: " + reference.ownerBinaryName(), exception);
        }
    }

    private static String descriptor(Method method) {
        var value = new StringBuilder("(");
        for (Class<?> parameter : method.getParameterTypes()) value.append(typeDescriptor(parameter));
        return value.append(')').append(typeDescriptor(method.getReturnType())).toString();
    }

    private static String typeDescriptor(Class<?> type) {
        if (type.isArray()) return type.getName().replace('.', '/');
        if (!type.isPrimitive()) return "L" + type.getName().replace('.', '/') + ";";
        return switch (type.getName()) {
            case "void" -> "V"; case "boolean" -> "Z"; case "byte" -> "B"; case "char" -> "C";
            case "short" -> "S"; case "int" -> "I"; case "long" -> "J"; case "float" -> "F";
            case "double" -> "D"; default -> throw new IllegalArgumentException(type.getName());
        };
    }
}
