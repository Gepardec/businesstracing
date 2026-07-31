package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.developer.DeveloperGraphExporter;
import at.gepardec.fachtracing.mermaid.MermaidRenderer;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.plantuml.PlantUmlRenderer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Maven-independent generation pipeline used by the Mojo and executable contracts. */
final class ProjectGraphGenerator {
    private final StaticDecisionAnalyzer analyzer = new StaticDecisionAnalyzer();
    private final DeveloperGraphExporter developerJson = new DeveloperGraphExporter();
    private final MermaidRenderer mermaid = new MermaidRenderer();
    private final PlantUmlRenderer plantUml = new PlantUmlRenderer();

    GenerationResult generate(
            List<Path> sourceFiles,
            List<Path> classpath,
            Charset charset,
            Path outputDirectory,
            boolean failOnIncomplete,
            Optional<DeveloperOutput> developerOutput) throws IOException, IncompleteGraphException {
        return generate(sourceFiles, sourceFiles, classpath, charset, outputDirectory,
                failOnIncomplete, developerOutput);
    }

    GenerationResult generate(
            List<Path> sourceFiles,
            List<Path> classpath,
            Charset charset,
            Path outputDirectory,
            boolean failOnIncomplete) throws IOException, IncompleteGraphException {
        return generate(sourceFiles, sourceFiles, classpath, charset, outputDirectory, failOnIncomplete);
    }

    GenerationResult generate(
            List<Path> rootSourceFiles,
            List<Path> sourceFiles,
            List<Path> classpath,
            Charset charset,
            Path outputDirectory,
            boolean failOnIncomplete) throws IOException, IncompleteGraphException {
        return generate(rootSourceFiles, sourceFiles, classpath, charset, outputDirectory,
                failOnIncomplete, Optional.empty());
    }

    GenerationResult generate(
            List<Path> rootSourceFiles,
            List<Path> sourceFiles,
            List<Path> classpath,
            Charset charset,
            Path outputDirectory,
            boolean failOnIncomplete,
            Optional<DeveloperOutput> developerOutput) throws IOException, IncompleteGraphException {
        Objects.requireNonNull(developerOutput, "developerOutput");
        if (rootSourceFiles.isEmpty() || sourceFiles.isEmpty()) {
            removePriorArtifacts(outputDirectory);
            return new GenerationResult(0, 0, true);
        }

        var analyses = analyzer.analyzeAll(
                new AnalysisRequest(sourceFiles, classpath, charset, rootSourceFiles));
        if (analyses.isEmpty()) {
            removePriorArtifacts(outputDirectory);
            return new GenerationResult(0, 0, true);
        }

        DeveloperGraphExporter.SourceRevision revision = developerOutput
                .map(DeveloperOutput::capture).orElse(null);
        DeveloperGraphExporter.SourceCatalog sourceCatalog = developerOutput
                .filter(output -> !output.externalOrigins().isEmpty())
                .map(output -> output.catalog(revision)).orElse(null);

        Files.createDirectories(outputDirectory);
        removePriorArtifacts(outputDirectory);

        Map<String, Long> slugCounts = analyses.stream()
                .collect(Collectors.groupingBy(
                        result -> slug(result.graph().decisionLabel()), HashMap::new, Collectors.counting()));
        var index = new StringBuilder("# Fachtracing decision graphs\n\n");
        var incomplete = new ArrayList<String>();

        for (var analysis : analyses) {
            BusinessDecisionGraph graph = analysis.graph();
            String base = slug(graph.decisionLabel());
            if (slugCounts.get(base) > 1) base += "-" + graph.graphId().substring(0, 8);
            String mermaidName = base + "-structure.mmd";
            String plantUmlName = base + "-structure.puml";
            String developerJsonName = base + "-developer.json";
            Files.writeString(outputDirectory.resolve(mermaidName), mermaid.structure(graph), charset);
            Files.writeString(outputDirectory.resolve(plantUmlName), plantUml.structure(graph), charset);
            index.append("- **").append(markdown(graph.decisionLabel())).append("** — ")
                    .append(graph.completeness()).append(" — ")
                    .append("[Mermaid](").append(mermaidName).append(") · ")
                    .append("[PlantUML](").append(plantUmlName).append(')');
            if (revision != null) {
                Files.writeString(outputDirectory.resolve(developerJsonName),
                        sourceCatalog == null
                                ? developerJson.export(analysis, revision)
                                : developerJson.export(analysis, sourceCatalog),
                        StandardCharsets.UTF_8);
                index.append(" · [Developer JSON](").append(developerJsonName).append(')');
            }
            index.append('\n');
            if (graph.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE) {
                incomplete.add(graph.decisionLabel());
            }
        }
        Files.writeString(outputDirectory.resolve("index.md"), index.toString(), charset);

        if (failOnIncomplete && !incomplete.isEmpty()) throw new IncompleteGraphException(incomplete);
        return new GenerationResult(analyses.size(), incomplete.size(), false);
    }

    private static void removePriorArtifacts(Path outputDirectory) throws IOException {
        if (!Files.isDirectory(outputDirectory)) return;
        try (var files = Files.list(outputDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (name.equals("index.md") || name.endsWith("-structure.mmd")
                        || name.endsWith("-structure.puml") || name.endsWith("-developer.json")) {
                    Files.delete(file);
                }
            }
        }
    }

    static String slug(String label) {
        String slug = label.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? "decision" : slug;
    }

    static Optional<DeveloperOutput> developerOutput(
            Path repositoryRoot,
            String repositoryUrl,
            String sourceUrlTemplate) {
        boolean hasRepository = repositoryUrl != null && !repositoryUrl.isBlank();
        boolean hasTemplate = sourceUrlTemplate != null && !sourceUrlTemplate.isBlank();
        if (hasRepository != hasTemplate) {
            throw new IllegalArgumentException(
                    "fachtracing.repositoryUrl and fachtracing.sourceUrlTemplate must be set together");
        }
        if (!hasRepository) return Optional.empty();
        return Optional.of(new DeveloperOutput(repositoryRoot, repositoryUrl, sourceUrlTemplate));
    }

    private static String markdown(String value) {
        return value.replace("\\", "\\\\").replace("*", "\\*")
                .replace("[", "\\[").replace("]", "\\]");
    }

    record GenerationResult(int graphCount, int incompleteCount, boolean skipped) { }

    record DeveloperOutput(
            Path repositoryRoot,
            String repositoryUrl,
            String sourceUrlTemplate,
            List<DeveloperGraphExporter.SourceOrigin> externalOrigins) {
        DeveloperOutput(Path repositoryRoot, String repositoryUrl, String sourceUrlTemplate) {
            this(repositoryRoot, repositoryUrl, sourceUrlTemplate, List.of());
        }

        DeveloperOutput {
            Objects.requireNonNull(repositoryRoot, "repositoryRoot");
            requireText(repositoryUrl, "repositoryUrl");
            requireText(sourceUrlTemplate, "sourceUrlTemplate");
            externalOrigins = List.copyOf(Objects.requireNonNull(externalOrigins, "externalOrigins"));
        }

        DeveloperGraphExporter.SourceRevision capture() {
            return DeveloperGraphExporter.SourceRevision.captureGit(
                    repositoryRoot, repositoryUrl, sourceUrlTemplate);
        }

        DeveloperGraphExporter.SourceCatalog catalog(DeveloperGraphExporter.SourceRevision revision) {
            var origins = new ArrayList<DeveloperGraphExporter.SourceOrigin>();
            origins.add(DeveloperGraphExporter.SourceOrigin.git("git", revision));
            origins.addAll(externalOrigins);
            return new DeveloperGraphExporter.SourceCatalog(origins);
        }

        private static void requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    static final class IncompleteGraphException extends Exception {
        private final List<String> decisions;

        private IncompleteGraphException(List<String> decisions) {
            super("Incomplete business-decision graphs: " + String.join(", ", decisions));
            this.decisions = List.copyOf(decisions);
        }

        List<String> decisions() { return decisions; }
    }
}
