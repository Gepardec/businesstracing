package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.analysis.ApplicationSourceBoundary;

import java.io.File;
import java.nio.file.Path;

/** Maven configuration for explicit JPMS ownership of external source inputs. */
public final class ExternalModuleOwnershipConfiguration {
    private String identity;
    private File sourceRoot;
    private String kind;
    private String moduleName;
    private File descriptor;
    private File binaryPath;

    /** Sets the source origin identity, such as one Maven coordinate. */
    public void setIdentity(String identity) { this.identity = identity; }
    /** Sets the external source root. */
    public void setSourceRoot(File sourceRoot) { this.sourceRoot = sourceRoot; }
    /** Sets `named` or `automatic`. */
    public void setKind(String kind) { this.kind = kind; }
    /** Sets the JPMS module name. */
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    /** Sets the named-module descriptor. */
    public void setDescriptor(File descriptor) { this.descriptor = descriptor; }
    /** Sets the automatic-module binary. */
    public void setBinaryPath(File binaryPath) { this.binaryPath = binaryPath; }

    boolean matches(ApplicationSourceBoundary.SourceOrigin origin, Path defaultRoot) {
        boolean identityMatch = identity != null && !identity.isBlank() && identity.equals(origin.identity());
        boolean rootMatch = sourceRoot != null && defaultRoot.toAbsolutePath().normalize()
                .equals(sourceRoot.toPath().toAbsolutePath().normalize());
        return identityMatch || rootMatch;
    }

    ApplicationSourceBoundary.ModuleOwnership ownership(Path defaultRoot) {
        Path root = sourceRoot == null ? defaultRoot : sourceRoot.toPath();
        if (kind == null || moduleName == null) {
            throw new IllegalArgumentException("external module ownership needs kind and moduleName");
        }
        return switch (kind.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "named" -> ApplicationSourceBoundary.ModuleOwnership.named(
                    moduleName,
                    descriptor == null ? root.resolve("module-info.java") : descriptor.toPath(),
                    root);
            case "automatic" -> {
                if (binaryPath == null) {
                    throw new IllegalArgumentException("automatic module ownership needs binaryPath");
                }
                yield ApplicationSourceBoundary.ModuleOwnership.automatic(
                        moduleName, binaryPath.toPath(), root);
            }
            default -> throw new IllegalArgumentException(
                    "external module ownership kind must be named or automatic");
        };
    }
}
