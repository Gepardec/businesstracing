package at.gepardec.fachtracing.analysis;

import java.util.Objects;

/** Exact binary identity for one external JVM method. */
public record ExternalMethodReference(String ownerBinaryName, String methodName, String descriptor)
        implements Comparable<ExternalMethodReference> {
    /** Creates a validated exact method reference. */
    public ExternalMethodReference {
        ownerBinaryName = requireText(ownerBinaryName, "ownerBinaryName");
        methodName = requireText(methodName, "methodName");
        descriptor = requireText(descriptor, "descriptor");
        if (ownerBinaryName.contains("/") || ownerBinaryName.startsWith(".")
                || ownerBinaryName.endsWith(".")) {
            throw new IllegalArgumentException("ownerBinaryName must use dotted JVM binary syntax");
        }
        if (!descriptor.startsWith("(") || descriptor.indexOf(')') < 1) {
            throw new IllegalArgumentException("descriptor must be a JVM method descriptor");
        }
    }

    @Override
    public int compareTo(ExternalMethodReference other) {
        int owner = ownerBinaryName.compareTo(other.ownerBinaryName);
        if (owner != 0) return owner;
        int method = methodName.compareTo(other.methodName);
        return method != 0 ? method : descriptor.compareTo(other.descriptor);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
