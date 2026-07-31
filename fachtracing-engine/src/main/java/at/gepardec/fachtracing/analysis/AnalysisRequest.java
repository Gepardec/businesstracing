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
        List<Path> rootSourceFiles) {
    /** Keeps the original contract: all supplied sources can contain graph roots. */
    public AnalysisRequest(List<Path> sourceFiles, List<Path> compilationClasspath, Charset charset) {
        this(sourceFiles, compilationClasspath, charset, sourceFiles);
    }

    /** Creates a defensive request. Root sources must be part of the complete Java source set. */
    public AnalysisRequest {
        sourceFiles = List.copyOf(sourceFiles);
        compilationClasspath = List.copyOf(compilationClasspath);
        charset = Objects.requireNonNull(charset, "charset");
        rootSourceFiles = List.copyOf(rootSourceFiles);
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

    private static boolean isJava(Path path) {
        return path.toString().endsWith(".java");
    }

    private static Path key(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
