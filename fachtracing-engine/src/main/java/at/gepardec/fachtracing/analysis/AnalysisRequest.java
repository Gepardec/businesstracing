package at.gepardec.fachtracing.analysis;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Source and classpath inputs for one framework-neutral analysis run. */
public record AnalysisRequest(List<Path> sourceFiles, List<Path> compilationClasspath, Charset charset) {
    /** Creates a defensive request. Source files must be explicit regular Java source paths. */
    public AnalysisRequest {
        sourceFiles = List.copyOf(sourceFiles);
        compilationClasspath = List.copyOf(compilationClasspath);
        charset = Objects.requireNonNull(charset, "charset");
        if (sourceFiles.isEmpty()) throw new IllegalArgumentException("at least one source file is required");
        if (sourceFiles.stream().anyMatch(path -> !path.toString().endsWith(".java"))) {
            throw new IllegalArgumentException("all source files must end in .java");
        }
    }

    /** Creates a UTF-8 request with an explicit compilation classpath. */
    public static AnalysisRequest of(List<Path> sourceFiles, List<Path> compilationClasspath) {
        return new AnalysisRequest(sourceFiles, compilationClasspath, StandardCharsets.UTF_8);
    }
}
