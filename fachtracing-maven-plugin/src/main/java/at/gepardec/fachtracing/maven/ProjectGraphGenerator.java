package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.ApplicationSourceBoundary;
import at.gepardec.fachtracing.analysis.BusinessArtifactGuard;
import at.gepardec.fachtracing.analysis.OpaqueLibraryBoundary;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.api.FachTracing;
import at.gepardec.fachtracing.business.BusinessGraphJsonExporter;
import at.gepardec.fachtracing.business.BusinessGraphJsonSchema;
import at.gepardec.fachtracing.business.BusinessGraphProjector;
import at.gepardec.fachtracing.business.BusinessMermaidRenderer;
import at.gepardec.fachtracing.business.BusinessPlantUmlRenderer;
import at.gepardec.fachtracing.developer.DeveloperGraphExporter;
import at.gepardec.fachtracing.developer.DeveloperGraphJsonSchema;
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
    private static final String DEVELOPER_SCHEMA_V1 = "fachtracing-developer-graph-v1.schema.json";
    private static final String LEGACY_DEVELOPER_SCHEMA_V2 = "fachtracing-developer-graph-v2.schema.json";
    private static final String BUSINESS_SCHEMA_V1 = "fachtracing-business-graph-v1.schema.json";

    private final StaticDecisionAnalyzer analyzer = new StaticDecisionAnalyzer();
    private final DeveloperGraphExporter developerJson = new DeveloperGraphExporter();
    private final DeveloperGraphJsonSchema developerSchema = new DeveloperGraphJsonSchema();
    private final MermaidRenderer mermaid = new MermaidRenderer();
    private final PlantUmlRenderer plantUml = new PlantUmlRenderer();
    private final BusinessGraphProjector businessProjector = new BusinessGraphProjector();
    private final BusinessMermaidRenderer businessMermaid = new BusinessMermaidRenderer();
    private final BusinessPlantUmlRenderer businessPlantUml = new BusinessPlantUmlRenderer();
    private final BusinessGraphJsonExporter businessJson = new BusinessGraphJsonExporter();
    private final BusinessGraphJsonSchema businessSchema = new BusinessGraphJsonSchema();

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
            return new GenerationResult(0, 0, true, List.of());
        }

        var analyses = analyzer.analyzeAll(
                new AnalysisRequest(sourceFiles, classpath, charset, rootSourceFiles));
        return write(analyses, charset, outputDirectory, failOnIncomplete, developerOutput);
    }

    GenerationResult generate(
            ApplicationSourceBoundary boundary,
            Charset outputCharset,
            Path outputDirectory,
            boolean failOnIncomplete) throws IOException, IncompleteGraphException {
        return generate(boundary, OpaqueLibraryBoundary.empty(), outputCharset,
                outputDirectory, failOnIncomplete, Optional.empty());
    }

    GenerationResult generate(
            ApplicationSourceBoundary boundary,
            Charset outputCharset,
            Path outputDirectory,
            boolean failOnIncomplete,
            Optional<DeveloperOutput> developerOutput) throws IOException, IncompleteGraphException {
        return generate(boundary, OpaqueLibraryBoundary.empty(), outputCharset,
                outputDirectory, failOnIncomplete, developerOutput);
    }

    GenerationResult generate(
            ApplicationSourceBoundary boundary,
            OpaqueLibraryBoundary opaqueLibraries,
            Charset outputCharset,
            Path outputDirectory,
            boolean failOnIncomplete,
            Optional<DeveloperOutput> developerOutput) throws IOException, IncompleteGraphException {
        Objects.requireNonNull(boundary, "boundary");
        Objects.requireNonNull(opaqueLibraries, "opaqueLibraries");
        Objects.requireNonNull(developerOutput, "developerOutput");
        if (boundary.entrySourceFiles().isEmpty()) {
            removePriorArtifacts(outputDirectory);
            return new GenerationResult(0, 0, true, List.of());
        }
        return write(analyzer.analyzeAll(boundary, opaqueLibraries), outputCharset, outputDirectory,
                failOnIncomplete, developerOutput);
    }

    private GenerationResult write(
            List<at.gepardec.fachtracing.analysis.AnalysisManifest.AnalysisResult> analyses,
            Charset charset,
            Path outputDirectory,
            boolean failOnIncomplete,
            Optional<DeveloperOutput> developerOutput) throws IOException, IncompleteGraphException {
        if (analyses.isEmpty()) {
            removePriorArtifacts(outputDirectory);
            return new GenerationResult(0, 0, true, List.of());
        }

        DeveloperGraphExporter.SourceRevision revision = developerOutput
                .map(DeveloperOutput::capture).orElse(null);
        DeveloperGraphExporter.SourceCatalog sourceCatalog = developerOutput
                .map(output -> output.catalog(revision)).orElse(null);

        Files.createDirectories(outputDirectory);
        removePriorArtifacts(outputDirectory);

        Map<String, Long> slugCounts = analyses.stream()
                .collect(Collectors.groupingBy(
                        result -> slug(result.graph().decisionLabel()), HashMap::new, Collectors.counting()));
        var index = new StringBuilder("# Fachtracing decision graphs\n\n");
        Files.writeString(outputDirectory.resolve(BUSINESS_SCHEMA_V1),
                businessSchema.generate(), StandardCharsets.UTF_8);
        index.append("Business JSON Schema: [V1](")
                .append(BUSINESS_SCHEMA_V1).append(")\n\n");
        if (revision != null) {
            Files.writeString(outputDirectory.resolve(DEVELOPER_SCHEMA_V1),
                    developerSchema.generate(), StandardCharsets.UTF_8);
            index.append("Developer JSON Schema: [V1](")
                    .append(DEVELOPER_SCHEMA_V1).append(")\n\n");
        }
        var incomplete = new ArrayList<String>();

        for (var analysis : analyses) {
            BusinessDecisionGraph graph = analysis.graph();
            List<String> artifactViolations = new BusinessArtifactGuard().violations(graph);
            if (!artifactViolations.isEmpty()) {
                throw new IllegalStateException("business graph contains technical iteration output: "
                        + artifactViolations);
            }
            String base = slug(graph.decisionLabel());
            if (slugCounts.get(base) > 1) base += "-" + graph.graphId().substring(0, 8);
            var businessGraph = businessProjector.project(analysis);
            String businessMermaidName = base + "-business.mmd";
            String businessPlantUmlName = base + "-business.puml";
            String businessJsonName = base + "-business.json";
            String mermaidName = base + "-structure.mmd";
            String plantUmlName = base + "-structure.puml";
            String developerJsonName = base + "-developer.json";
            Files.writeString(outputDirectory.resolve(businessMermaidName),
                    businessMermaid.render(businessGraph), charset);
            Files.writeString(outputDirectory.resolve(businessPlantUmlName),
                    businessPlantUml.render(businessGraph), charset);
            Files.writeString(outputDirectory.resolve(businessJsonName),
                    businessJson.export(businessGraph), StandardCharsets.UTF_8);
            Files.writeString(outputDirectory.resolve(mermaidName), mermaid.structure(graph), charset);
            Files.writeString(outputDirectory.resolve(plantUmlName), plantUml.structure(graph), charset);
            index.append("- **").append(markdown(graph.decisionLabel())).append("** — ")
                    .append(graph.completeness()).append(" — ")
                    .append("[Business Mermaid](").append(businessMermaidName).append(") · ")
                    .append("[Business PlantUML](").append(businessPlantUmlName).append(") · ")
                    .append("[Business JSON](").append(businessJsonName).append(")\n")
                    .append("  - Technical developer artifacts: ")
                    .append("[structure Mermaid](").append(mermaidName).append(") · ")
                    .append("[structure PlantUML](").append(plantUmlName).append(')');
            if (revision != null) {
                Files.writeString(outputDirectory.resolve(developerJsonName),
                        developerJson.export(analysis, sourceCatalog),
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
        return new GenerationResult(analyses.size(), incomplete.size(), false, analyses);
    }

    private static void removePriorArtifacts(Path outputDirectory) throws IOException {
        if (!Files.isDirectory(outputDirectory)) return;
        try (var files = Files.list(outputDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (name.equals("index.md") || name.endsWith("-structure.mmd")
                        || name.endsWith("-structure.puml") || name.endsWith("-developer.json")
                        || name.endsWith("-business.mmd") || name.endsWith("-business.puml")
                        || name.endsWith("-business.json") || name.equals(BUSINESS_SCHEMA_V1)
                        || name.equals(DEVELOPER_SCHEMA_V1)
                        || name.equals(LEGACY_DEVELOPER_SCHEMA_V2)) {
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

    @FachTracing("enable developer graph export")
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

    record GenerationResult(
            int graphCount,
            int incompleteCount,
            boolean skipped,
            List<AnalysisManifest.AnalysisResult> analyses) {
        GenerationResult {
            analyses = List.copyOf(analyses);
        }
    }

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
