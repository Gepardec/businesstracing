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
        analysis.manifest().sourceFingerprints().keySet().forEach(path ->
                revision.relativePath(Path.of(path)));
        return export(analysis, new SourceCatalog(List.of(SourceOrigin.git("git", revision))));
    }

    /** Produces developer JSON for one or more provenance origins. */
    public String export(AnalysisManifest.AnalysisResult analysis, SourceCatalog catalog) {
        Objects.requireNonNull(analysis, "analysis");
        Objects.requireNonNull(catalog, "catalog");
        if (!analysis.graph().graphId().equals(analysis.manifest().graphId())
                || analysis.graph().version() != analysis.manifest().graphVersion()) {
            throw new IllegalArgumentException("graph and developer manifest versions do not match");
        }
        Map<Path, ResolvedSource> sources = resolveAndVerify(analysis.manifest(), catalog);
        var output = new StringBuilder(4096).append('{');
        stringField(output, "schema", SCHEMA).append(',');
        output.append("\"graph\":");
        appendGraph(output, analysis, sources);
        output.append(',').append("\"sourceOrigins\":");
        appendOrigins(output, catalog);
        output.append(',').append("\"sourceFiles\":");
        appendSourceFiles(output, sources);
        return output.append('}').append('\n').toString();
    }

    private static void appendGraph(
            StringBuilder output,
            AnalysisManifest.AnalysisResult analysis,
            Map<Path, ResolvedSource> sources) {
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
            BusinessDecisionGraph.DecisionNode node = graph.nodes().get(index);
            output.append('{');
            stringField(output, "id", node.nodeId()).append(',');
            stringField(output, "kind", node.kind().name()).append(',');
            stringField(output, "label", node.businessLabel()).append(',');
            output.append("\"attributes\":");
            appendStringMap(output, node.attributes());
            AnalysisManifest.SourceMapping mapping = analysis.manifest().sourceMappings().get(node.nodeId());
            if (mapping != null) {
                output.append(',').append("\"source\":");
                appendSource(output, mapping, sources.get(canonical(mapping.source())));
            }
            output.append('}');
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

    private static void appendSource(
            StringBuilder output,
            AnalysisManifest.SourceMapping mapping,
            ResolvedSource source) {
        if (source == null) throw new IllegalArgumentException("source has no provenance origin: " + mapping.source());
        output.append('{');
        stringField(output, "originId", source.origin().id()).append(',');
        stringField(output, "path", source.relativePath()).append(',');
        numberField(output, "line", mapping.line()).append(',');
        numberField(output, "column", mapping.column()).append(',');
        stringField(output, "syntaxKind", mapping.treeKind()).append(',');
        stringField(output, "sha256", source.sha256());
        if (source.origin().revision() != null) {
            output.append(',');
            stringField(output, "url", source.origin().revision()
                    .sourceUrl(source.relativePath(), mapping.line(), mapping.column()));
        }
        output.append('}');
    }

    private static void appendOrigins(StringBuilder output, SourceCatalog catalog) {
        output.append('[');
        for (int index = 0; index < catalog.origins().size(); index++) {
            if (index > 0) output.append(',');
            SourceOrigin origin = catalog.origins().get(index);
            output.append('{');
            stringField(output, "id", origin.id()).append(',');
            stringField(output, "kind", origin.kind().name()).append(',');
            stringField(output, "identity", origin.identity()).append(',');
            stringField(output, "checksum", origin.checksum());
            if (origin.revision() != null) {
                output.append(',').append("\"revision\":");
                appendRevision(output, origin.revision());
            }
            output.append('}');
        }
        output.append(']');
    }

    private static void appendSourceFiles(StringBuilder output, Map<Path, ResolvedSource> sources) {
        List<ResolvedSource> files = sources.values().stream()
                .sorted(Comparator.comparing(item -> item.origin().id() + ':' + item.relativePath()))
                .toList();
        output.append('[');
        for (int index = 0; index < files.size(); index++) {
            if (index > 0) output.append(',');
            ResolvedSource source = files.get(index);
            output.append('{');
            stringField(output, "originId", source.origin().id()).append(',');
            stringField(output, "path", source.relativePath()).append(',');
            stringField(output, "sha256", source.sha256());
            output.append('}');
        }
        output.append(']');
    }

    private static Map<Path, ResolvedSource> resolveAndVerify(
            AnalysisManifest manifest,
            SourceCatalog catalog) {
        var result = new java.util.LinkedHashMap<Path, ResolvedSource>();
        manifest.sourceFingerprints().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Path source = canonical(Path.of(entry.getKey()));
                    SourceOrigin origin = catalog.originFor(source);
                    String relative = origin.relativePath(source);
                    try {
                        String actual = sha256(Files.readAllBytes(source));
                        if (!actual.equals(entry.getValue())) {
                            throw new IllegalStateException("source file does not match the analyzed content: " + relative);
                        }
                        if (origin.revision() != null) {
                            origin.revision().verifyCommittedSource(source, entry.getValue());
                        }
                    } catch (IOException exception) {
                        throw new IllegalStateException("cannot verify analyzed source file: " + relative, exception);
                    }
                    result.put(source, new ResolvedSource(origin, relative, entry.getValue()));
                });
        return Map.copyOf(result);
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

    private static String sha256(byte[] content) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static Path canonical(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException ignored) {
            return path.toAbsolutePath().normalize();
        }
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

    private record ResolvedSource(SourceOrigin origin, String relativePath, String sha256) { }

    /** Supported developer source origins. */
    public enum OriginKind { GIT, MAVEN_SOURCE, GENERATED, LOCAL }

    /** One provenance origin that contains analyzed source files. */
    public record SourceOrigin(
            String id,
            OriginKind kind,
            Path root,
            String identity,
            String checksum,
            SourceRevision revision) {
        /** Creates a normalized source origin. */
        public SourceOrigin {
            id = requireOriginText(id, "id");
            kind = Objects.requireNonNull(kind, "kind");
            root = canonical(Objects.requireNonNull(root, "root"));
            identity = requireOriginText(identity, "identity");
            checksum = Objects.requireNonNullElse(checksum, "");
            if ((kind == OriginKind.GIT) != (revision != null)) {
                throw new IllegalArgumentException("only Git origins can contain a source revision");
            }
            if (revision != null && !revision.repositoryRoot().equals(root)) {
                throw new IllegalArgumentException("Git origin root must match its source revision");
            }
        }

        /** Creates a Git origin with commit-pinned browser URLs. */
        public static SourceOrigin git(String id, SourceRevision revision) {
            Objects.requireNonNull(revision, "revision");
            return new SourceOrigin(id, OriginKind.GIT, revision.repositoryRoot(),
                    revision.repository(), revision.commit(), revision);
        }

        /** Creates an origin without a source browser URL. */
        public static SourceOrigin external(
                String id, OriginKind kind, Path root, String identity, String checksum) {
            if (kind == OriginKind.GIT) {
                throw new IllegalArgumentException("use SourceOrigin.git for Git origins");
            }
            return new SourceOrigin(id, kind, root, identity, checksum, null);
        }

        private String relativePath(Path source) {
            Path value = canonical(source);
            if (!value.startsWith(root)) {
                throw new IllegalArgumentException("source is outside its provenance root: " + source);
            }
            String relative = root.relativize(value).toString().replace('\\', '/');
            if (relative.isBlank()) throw new IllegalArgumentException("source origin must contain a file");
            return relative;
        }
    }

    /** Resolves every analyzed source to the most specific declared origin root. */
    public record SourceCatalog(List<SourceOrigin> origins) {
        /** Creates a deterministic catalog and rejects ambiguous origin roots. */
        public SourceCatalog {
            Objects.requireNonNull(origins, "origins");
            var ids = new java.util.HashSet<String>();
            var roots = new java.util.HashSet<Path>();
            for (SourceOrigin origin : origins) {
                if (!ids.add(origin.id())) throw new IllegalArgumentException("duplicate source origin id: " + origin.id());
                if (!roots.add(origin.root())) {
                    throw new IllegalArgumentException("duplicate source origin root: " + origin.root());
                }
            }
            origins = origins.stream().sorted(Comparator.comparing(SourceOrigin::id)).toList();
            if (origins.isEmpty()) throw new IllegalArgumentException("at least one source origin is required");
        }

        private SourceOrigin originFor(Path source) {
            List<SourceOrigin> candidates = origins.stream()
                    .filter(origin -> source.startsWith(origin.root()))
                    .sorted(Comparator.comparingInt((SourceOrigin origin) -> origin.root().getNameCount()).reversed())
                    .toList();
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("source has no provenance origin: " + source);
            }
            if (candidates.size() > 1
                    && candidates.get(0).root().getNameCount() == candidates.get(1).root().getNameCount()) {
                throw new IllegalArgumentException("source has ambiguous provenance origins: " + source);
            }
            return candidates.getFirst();
        }
    }

    private static String requireOriginText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

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

        private void verifyCommittedSource(Path source, String expectedFingerprint) {
            String path = relativePath(source);
            byte[] content = gitBlob(repositoryRoot, commit, path);
            if (!sha256(content).equals(expectedFingerprint)) {
                throw new IllegalStateException(
                        "source file does not match the captured Git commit: " + path);
            }
        }

        private static byte[] gitBlob(Path root, String commit, String path) {
            var command = List.of("git", "-C", root.toString(), "show", commit + ":" + path);
            try {
                Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                byte[] output = process.getInputStream().readAllBytes();
                int exit = process.waitFor();
                if (exit != 0) {
                    throw new IllegalStateException(
                            "analyzed source is not present in the captured Git commit: " + path);
                }
                return output;
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "cannot read analyzed source from the captured Git commit: " + path, exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Git source verification was interrupted: " + path, exception);
            }
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
