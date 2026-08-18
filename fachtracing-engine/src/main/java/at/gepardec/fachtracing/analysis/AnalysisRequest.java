package at.gepardec.fachtracing.analysis;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Source, graph-root, and classpath inputs for one framework-neutral analysis run. */
public record AnalysisRequest(
        List<Path> sourceFiles,
        List<Path> compilationClasspath,
        Charset charset,
        List<Path> rootSourceFiles,
        List<ExternalMethodContractProvider> externalMethodContractProviders,
        List<BusinessEntryPoint> businessEntryPoints,
        List<DynamicDispatchTargetSelector> dynamicDispatchTargetSelectors,
        List<SourceSemanticProvider> sourceSemanticProviders) {
    /** Keeps the provider-aware contract without configured business entry points. */
    public AnalysisRequest(
            List<Path> sourceFiles,
            List<Path> compilationClasspath,
            Charset charset,
            List<Path> rootSourceFiles,
            List<ExternalMethodContractProvider> externalMethodContractProviders) {
        this(sourceFiles, compilationClasspath, charset, rootSourceFiles,
                externalMethodContractProviders, List.of(), List.of(), List.of());
    }

    /** Keeps the business-entry-point contract without framework dispatch selectors. */
    public AnalysisRequest(
            List<Path> sourceFiles,
            List<Path> compilationClasspath,
            Charset charset,
            List<Path> rootSourceFiles,
            List<ExternalMethodContractProvider> externalMethodContractProviders,
            List<BusinessEntryPoint> businessEntryPoints) {
        this(sourceFiles, compilationClasspath, charset, rootSourceFiles,
                externalMethodContractProviders, businessEntryPoints, List.of(), List.of());
    }

    /** Keeps the project-aware contract without external method providers. */
    public AnalysisRequest(
            List<Path> sourceFiles,
            List<Path> compilationClasspath,
            Charset charset,
            List<Path> rootSourceFiles) {
        this(sourceFiles, compilationClasspath, charset, rootSourceFiles,
                List.of(), List.of(), List.of(), List.of());
    }

    /** Keeps the original contract: all supplied sources can contain graph roots. */
    public AnalysisRequest(List<Path> sourceFiles, List<Path> compilationClasspath, Charset charset) {
        this(sourceFiles, compilationClasspath, charset, sourceFiles,
                List.of(), List.of(), List.of(), List.of());
    }

    /** Creates a defensive request. Root sources must be part of the complete Java source set. */
    public AnalysisRequest {
        sourceFiles = List.copyOf(sourceFiles);
        compilationClasspath = List.copyOf(compilationClasspath);
        charset = Objects.requireNonNull(charset, "charset");
        rootSourceFiles = List.copyOf(rootSourceFiles);
        externalMethodContractProviders = List.copyOf(externalMethodContractProviders);
        businessEntryPoints = List.copyOf(businessEntryPoints);
        dynamicDispatchTargetSelectors = List.copyOf(dynamicDispatchTargetSelectors);
        sourceSemanticProviders = List.copyOf(sourceSemanticProviders);
        if (sourceFiles.isEmpty()) throw new IllegalArgumentException("at least one source file is required");
        if (rootSourceFiles.isEmpty()) throw new IllegalArgumentException("at least one root source file is required");
        if (sourceFiles.stream().anyMatch(path -> !isJava(path))
                || rootSourceFiles.stream().anyMatch(path -> !isJava(path))) {
            throw new IllegalArgumentException("all source files must end in .java");
        }
        var sourceKeys = sourceFiles.stream().map(AnalysisRequest::key).collect(java.util.stream.Collectors.toSet());
        if (rootSourceFiles.stream().map(AnalysisRequest::key).anyMatch(root -> !sourceKeys.contains(root))) {
            throw new IllegalArgumentException("all root source files must be part of sourceFiles");
        }
    }

    /** Creates a UTF-8 request with an explicit compilation classpath. */
    public static AnalysisRequest of(List<Path> sourceFiles, List<Path> compilationClasspath) {
        return new AnalysisRequest(sourceFiles, compilationClasspath, StandardCharsets.UTF_8);
    }

    /** Returns the same source request with explicit trusted method-level semantic providers. */
    public AnalysisRequest withExternalMethodContractProviders(
            List<? extends ExternalMethodContractProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        return new AnalysisRequest(sourceFiles, compilationClasspath, charset, rootSourceFiles,
                List.copyOf(providers), businessEntryPoints, dynamicDispatchTargetSelectors,
                sourceSemanticProviders);
    }

    /** Returns the same source request with exact configured business graph roots. */
    public AnalysisRequest withBusinessEntryPoints(List<? extends BusinessEntryPoint> entryPoints) {
        Objects.requireNonNull(entryPoints, "entryPoints");
        return new AnalysisRequest(sourceFiles, compilationClasspath, charset, rootSourceFiles,
                externalMethodContractProviders, List.copyOf(entryPoints), dynamicDispatchTargetSelectors,
                sourceSemanticProviders);
    }

    /** Returns the same request with optional framework dispatch target selectors. */
    public AnalysisRequest withDynamicDispatchTargetSelectors(
            List<? extends DynamicDispatchTargetSelector> selectors) {
        Objects.requireNonNull(selectors, "selectors");
        return new AnalysisRequest(sourceFiles, compilationClasspath, charset, rootSourceFiles,
                externalMethodContractProviders, businessEntryPoints, List.copyOf(selectors),
                sourceSemanticProviders);
    }

    /** Returns the same request with framework-managed source semantic providers. */
    public AnalysisRequest withSourceSemanticProviders(
            List<? extends SourceSemanticProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        return new AnalysisRequest(sourceFiles, compilationClasspath, charset, rootSourceFiles,
                externalMethodContractProviders, businessEntryPoints, dynamicDispatchTargetSelectors,
                List.copyOf(providers));
    }

    private static boolean isJava(Path path) {
        return path.toString().endsWith(".java");
    }

    private static Path key(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
