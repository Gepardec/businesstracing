package at.gepardec.fachtracing.analysis;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Exact Java method selection and business label for one graph root. */
public record BusinessEntryPoint(
        String owner,
        String method,
        List<String> parameterTypes,
        String label) {
    private static final Pattern OWNER = Pattern.compile(
            "[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)+");
    private static final Pattern METHOD = Pattern.compile("[A-Za-z_$][\\w$]*");
    private static final Pattern PARAMETER_TYPE = Pattern.compile(
            "(?:boolean|byte|short|int|long|char|float|double|"
                    + "[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)(?:\\[\\])*");

    /** Validates and copies the configured method selection. */
    public BusinessEntryPoint {
        owner = required(owner, "owner");
        method = required(method, "method");
        parameterTypes = List.copyOf(Objects.requireNonNull(parameterTypes, "parameterTypes"));
        label = required(label, "label");
        if (!OWNER.matcher(owner).matches()) {
            throw new IllegalArgumentException("owner must be a qualified Java type name: " + owner);
        }
        if (!METHOD.matcher(method).matches()) {
            throw new IllegalArgumentException("method must be a Java method name: " + method);
        }
        for (String parameterType : parameterTypes) {
            if (parameterType == null || !PARAMETER_TYPE.matcher(parameterType).matches()) {
                throw new IllegalArgumentException(
                        "parameter type must be an erased Java type name: " + parameterType);
            }
        }
    }

    /** Selects a method when its owner and name are unique. */
    public static BusinessEntryPoint of(String owner, String method, String label) {
        return new BusinessEntryPoint(owner, method, List.of(), label);
    }

    /** Gives the configured method identity for diagnostics. */
    public String identity() {
        String parameters = parameterTypes.isEmpty() ? "" : String.join(",", parameterTypes);
        return owner + "#" + method + "(" + parameters + ")";
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
