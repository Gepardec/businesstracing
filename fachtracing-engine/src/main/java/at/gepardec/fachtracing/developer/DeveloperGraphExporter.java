package at.gepardec.fachtracing.developer;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Creates deterministic JSON from a graph and its separate developer source data.
 *
 * <p>This export is intended for developer tools. It must not be substituted for the
 * business-facing graph or decision record because it contains repository and source locations.
 */
public final class DeveloperGraphExporter {
    /** Identifies the current JSON format. */
    public static final String SCHEMA = "fachtracing-developer-graph/v1";

    /** Produces one deterministic developer graph document. */
    public String export(AnalysisManifest.AnalysisResult analysis, SourceRevision revision) {
        Objects.requireNonNull(analysis, "analysis");
        Objects.requireNonNull(revision, "revision");
        if (!analysis.graph().graphId().equals(analysis.manifest().graphId())
                || analysis.graph().version() != analysis.manifest().graphVersion()) {
            throw new IllegalArgumentException("graph and developer manifest versions do not match");
        }
        verifySourceFiles(analysis.manifest(), revision);

        var output = new StringBuilder(4096).append('{');
        stringField(output, "schema", SCHEMA).append(',');
        output.append("\"graph\":");
        appendGraph(output, analysis, revision);
        output.append(',').append("\"sourceRevision\":");
        appendRevision(output, revision);
        output.append(',').append("\"sourceFiles\":");
        appendSourceFiles(output, analysis.manifest(), revision);
        return output.append('}').append('\n').toString();
    }

    private static void appendGraph(
            StringBuilder output,
            AnalysisManifest.AnalysisResult analysis,
            SourceRevision revision) {
        BusinessDecisionGraph graph = analysis.graph();
        output.append('{');
        stringField(output, "id", graph.graphId()).append(',');
        numberField(output, "version", graph.version()).append(',');
        stringField(output, "label", graph.decisionLabel()).append(',');
        stringField(output, "entryNodeId", graph.entryNodeId()).append(',');
        stringField(output, "completeness", graph.completeness().name()).append(',');
        output.append("\"nodes\":[");
        for (int index = 0; index < graph.nodes().size(); index++) {
            if (index > 0) output.append(',');
            appendNode(output, graph.nodes().get(index), analysis.manifest(), revision);
        }
        output.append("],\"edges\":[");
        for (int index = 0; index < graph.edges().size(); index++) {
            if (index > 0) output.append(',');
            appendEdge(output, graph.edges().get(index));
        }
        output.append("],\"coverageGaps\":[");
        for (int index = 0; index < graph.coverageGaps().size(); index++) {
            if (index > 0) output.append(',');
            var gap = graph.coverageGaps().get(index);
            output.append('{');
            stringField(output, "nodeId", gap.nodeId()).append(',');
            stringField(output, "description", gap.description());
            output.append('}');
        }
        output.append("]}");
    }

    private static void appendNode(
            StringBuilder output,
            BusinessDecisionGraph.DecisionNode node,
            AnalysisManifest manifest,
            SourceRevision revision) {
        output.append('{');
        stringField(output, "id", node.nodeId()).append(',');
        stringField(output, "kind", node.kind().name()).append(',');
        stringField(output, "label", node.businessLabel()).append(',');
        output.append("\"attributes\":");
        appendStringMap(output, node.attributes());
        AnalysisManifest.SourceMapping mapping = manifest.sourceMappings().get(node.nodeId());
        if (mapping != null) {
            output.append(',').append("\"source\":");
            appendSource(output, mapping, manifest, revision);
        }
        output.append('}');
    }

    private static void appendSource(
            StringBuilder output,
            AnalysisManifest.SourceMapping mapping,
            AnalysisManifest manifest,
            SourceRevision revision) {
        String relativePath = revision.relativePath(mapping.source());
        String fingerprint = fingerprint(manifest, mapping.source(), revision);
        output.append('{');
        stringField(output, "path", relativePath).append(',');
        numberField(output, "line", mapping.line()).append(',');
        numberField(output, "column", mapping.column()).append(',');
        stringField(output, "syntaxKind", mapping.treeKind()).append(',');
        stringField(output, "sha256", fingerprint).append(',');
        stringField(output, "url", revision.sourceUrl(relativePath, mapping.line(), mapping.column()));
        output.append('}');
    }

    private static String fingerprint(
            AnalysisManifest manifest,
            Path source,
            SourceRevision revision) {
        String relativeSource = revision.relativePath(source);
        return manifest.sourceFingerprints().entrySet().stream()
                .filter(entry -> revision.relativePath(Path.of(entry.getKey())).equals(relativeSource))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no source fingerprint exists for repository path " + relativeSource));
    }

    private static void appendEdge(StringBuilder output, BusinessDecisionGraph.DecisionEdge edge) {
        output.append('{');
        stringField(output, "id", edge.edgeId()).append(',');
        stringField(output, "from", edge.fromNodeId()).append(',');
        stringField(output, "to", edge.toNodeId()).append(',');
        stringField(output, "outcome", edge.outcome());
        output.append('}');
    }

    private static void appendRevision(StringBuilder output, SourceRevision revision) {
        output.append('{');
        stringField(output, "repository", revision.repository()).append(',');
        stringField(output, "commit", revision.commit()).append(',');
        stringField(output, "committedAt", revision.committedAt());
        output.append('}');
    }

    private static void appendSourceFiles(
            StringBuilder output,
            AnalysisManifest manifest,
            SourceRevision revision) {
        var files = new ArrayList<SourceFile>();
        manifest.sourceFingerprints().forEach((path, fingerprint) ->
                files.add(new SourceFile(revision.relativePath(Path.of(path)), fingerprint)));
        files.sort(Comparator.comparing(SourceFile::path));
        output.append('[');
        for (int index = 0; index < files.size(); index++) {
            if (index > 0) output.append(',');
            SourceFile file = files.get(index);
            output.append('{');
            stringField(output, "path", file.path()).append(',');
            stringField(output, "sha256", file.sha256());
            output.append('}');
        }
        output.append(']');
    }

    private static void verifySourceFiles(AnalysisManifest manifest, SourceRevision revision) {
        manifest.sourceFingerprints().forEach((path, expected) -> {
            Path source = Path.of(path);
            String relativePath = revision.relativePath(source);
            try {
                String actual = java.util.HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
                if (!actual.equals(expected)) {
                    throw new IllegalStateException(
                            "source file does not match the analyzed content: " + relativePath);
                }
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "cannot verify analyzed source file: " + relativePath, exception);
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("SHA-256 unavailable", impossible);
            }
        });
    }

    private static void appendStringMap(StringBuilder output, Map<String, String> values) {
        output.append('{');
        List<Map.Entry<String, String>> entries = values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) output.append(',');
            jsonString(output, entries.get(index).getKey()).append(':');
            jsonString(output, entries.get(index).getValue());
        }
        output.append('}');
    }

    private static StringBuilder stringField(StringBuilder output, String name, String value) {
        jsonString(output, name).append(':');
        return jsonString(output, value);
    }

    private static StringBuilder numberField(StringBuilder output, String name, long value) {
        jsonString(output, name).append(':').append(value);
        return output;
    }

    private static StringBuilder jsonString(StringBuilder output, String value) {
        Objects.requireNonNull(value, "JSON string");
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20 || character == '\u2028' || character == '\u2029') {
                        output.append(String.format("\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        return output.append('"');
    }

    private record SourceFile(String path, String sha256) { }

    /**
     * A clean source-control revision and a URL template for any source browser.
     *
     * @param repositoryRoot root used to remove build-machine paths
     * @param repository stable browser-facing repository identifier
     * @param commit full Git commit identifier
     * @param committedAt Git committer timestamp
     * @param sourceUrlTemplate template containing {@code {commit}} and {@code {path}}, with
     *                          optional {@code {line}} and {@code {column}}
     */
    public static final class SourceRevision {
        private final Path repositoryRoot;
        private final String repository;
        private final String commit;
        private final String committedAt;
        private final String sourceUrlTemplate;

        private SourceRevision(
                Path repositoryRoot,
                String repository,
                String commit,
                String committedAt,
                String sourceUrlTemplate) {
            this.repositoryRoot = canonical(Objects.requireNonNull(repositoryRoot, "repositoryRoot"));
            this.repository = requireText(repository, "repository");
            this.commit = requireText(commit, "commit");
            this.committedAt = requireText(committedAt, "committedAt");
            this.sourceUrlTemplate = validateTemplate(sourceUrlTemplate);
        }

        /**
         * Captures {@code HEAD} from a clean Git working tree.
         *
         * <p>Tracked and untracked changes are rejected because their source does not belong to
         * the captured commit. Git runs only when a caller uses this build-time method.
         */
        public static SourceRevision captureGit(
                Path repositoryRoot,
                String repository,
                String sourceUrlTemplate) {
            validateTemplate(sourceUrlTemplate);
            Path requestedRoot = Objects.requireNonNull(repositoryRoot, "repositoryRoot")
                    .toAbsolutePath().normalize();
            Path actualRoot = Path.of(git(requestedRoot, "rev-parse", "--show-toplevel"))
                    .toAbsolutePath().normalize();
            if (!git(actualRoot, "status", "--porcelain", "--untracked-files=all").isBlank()) {
                throw new IllegalStateException(
                        "Git working tree contains uncommitted changes; commit them before export");
            }
            return new SourceRevision(
                    actualRoot,
                    repository,
                    git(actualRoot, "rev-parse", "HEAD"),
                    git(actualRoot, "show", "-s", "--format=%cI", "HEAD"),
                    sourceUrlTemplate);
        }

        /** Returns the root that contains all exported source files. */
        public Path repositoryRoot() { return repositoryRoot; }

        /** Returns the browser-facing repository identifier. */
        public String repository() { return repository; }

        /** Returns the full Git commit identifier. */
        public String commit() { return commit; }

        /** Returns the Git committer timestamp. */
        public String committedAt() { return committedAt; }

        /** Resolves an analyzer path to a slash-separated repository path. */
        public String relativePath(Path source) {
            Path absoluteSource = canonical(Objects.requireNonNull(source, "source"));
            if (!absoluteSource.startsWith(repositoryRoot)) {
                throw new IllegalArgumentException("source path is outside the declared repository root");
            }
            String path = repositoryRoot.relativize(absoluteSource).toString().replace('\\', '/');
            if (path.isBlank()) throw new IllegalArgumentException("source path must name a file below repository root");
            return path;
        }

        /** Expands the configured commit-pinned source URL. */
        public String sourceUrl(String relativePath, long line, long column) {
            return sourceUrlTemplate
                    .replace("{commit}", encode(commit))
                    .replace("{path}", encodePath(relativePath))
                    .replace("{line}", Long.toString(line))
                    .replace("{column}", Long.toString(column));
        }

        private static String git(Path root, String... arguments) {
            var command = new ArrayList<String>();
            command.add("git");
            command.add("-C");
            command.add(root.toString());
            command.addAll(List.of(arguments));
            try {
                Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                int exit = process.waitFor();
                if (exit != 0) throw new IllegalStateException("Git source capture failed: " + output);
                return output;
            } catch (IOException exception) {
                throw new IllegalStateException("Git source capture failed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Git source capture was interrupted", exception);
            }
        }

        private static String validateTemplate(String template) {
            String value = requireText(template, "sourceUrlTemplate");
            if (!value.contains("{commit}") || !value.contains("{path}")) {
                throw new IllegalArgumentException(
                        "sourceUrlTemplate must contain {commit} and {path} placeholders");
            }
            return value;
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
            return value;
        }

        private static String encodePath(String path) {
            return java.util.Arrays.stream(path.split("/", -1))
                    .map(SourceRevision::encode)
                    .collect(java.util.stream.Collectors.joining("/"));
        }

        private static String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
        }

        private static Path canonical(Path path) {
            try {
                return path.toRealPath();
            } catch (IOException ignored) {
                return path.toAbsolutePath().normalize();
            }
        }
    }
}
