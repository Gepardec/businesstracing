package at.gepardec.fachtracing.jakartaee;

import at.gepardec.fachtracing.analysis.ExternalMethodContract;
import at.gepardec.fachtracing.analysis.ExternalMethodContractProviders;
import at.gepardec.fachtracing.analysis.ExternalMethodReference;

import java.lang.reflect.Method;

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
