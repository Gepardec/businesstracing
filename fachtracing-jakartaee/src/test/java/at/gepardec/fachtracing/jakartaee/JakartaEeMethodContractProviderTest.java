package at.gepardec.fachtracing.jakartaee;

import at.gepardec.fachtracing.analysis.ExternalMethodContract;
import at.gepardec.fachtracing.analysis.ExternalMethodContractProviders;
import at.gepardec.fachtracing.analysis.ExternalMethodReference;
import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;

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
        selectsOnlyScopedNonAlternativeCdiBeans();
    }

    private static void selectsOnlyScopedNonAlternativeCdiBeans() {
        Path fixture = Path.of("fachtracing-jakartaee/src/test/resources/fixtures/CdiWorkflow.java");
        List<Path> classpath = Arrays.stream(System.getProperty("java.class.path")
                        .split(java.io.File.pathSeparator)).map(Path::of).toList();
        var result = new StaticDecisionAnalyzer().analyzeAll(AnalysisRequest.of(List.of(fixture), classpath)
                        .withDynamicDispatchTargetSelectors(List.of(new CdiDispatchTargetSelector())))
                .stream().filter(candidate -> candidate.graph().decisionLabel().equals("apply CDI rule"))
                .findFirst().orElseThrow();
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
