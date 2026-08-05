package at.gepardec.fachtracing.runtime;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Self-contained build-time configuration for runtime tracing. */
public record RuntimeActivationBundle(
        String boundaryFingerprint,
        String javaAgentOption,
        Map<String, String> classFingerprints,
        List<DecisionDefinition> decisions) {
    public static final String SCHEMA = "fachtracing-activation/v2";

    /** Creates a validated immutable activation bundle. */
    public RuntimeActivationBundle {
        boundaryFingerprint = text(boundaryFingerprint, "boundaryFingerprint");
        javaAgentOption = text(javaAgentOption, "javaAgentOption");
        classFingerprints = Map.copyOf(Objects.requireNonNull(classFingerprints, "classFingerprints"));
        decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions"));
        if (decisions.isEmpty()) throw new IllegalArgumentException("decisions must not be empty");
        classFingerprints.forEach((owner, fingerprint) -> {
            text(owner, "class owner");
            if (!fingerprint.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("class fingerprint must be lowercase SHA-256: " + owner);
            }
        });
    }

    /** Returns the deterministic UTF-8 JSON representation. */
    public byte[] toJson() {
        var output = new StringBuilder(8192).append('{');
        field(output, "schema", SCHEMA).append(',');
        field(output, "boundaryFingerprint", boundaryFingerprint).append(',');
        field(output, "javaAgentOption", javaAgentOption).append(',');
        output.append("\"classFingerprints\":"); strings(output, classFingerprints); output.append(',');
        output.append("\"decisions\":[");
        for (int index = 0; index < decisions.size(); index++) {
            if (index > 0) output.append(',');
            decision(output, decisions.get(index));
        }
        return output.append("]}\n").toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Reads and validates a V2 activation bundle. */
    public static RuntimeActivationBundle fromJson(byte[] json) {
        Map<String, Object> root = object(new Parser(new String(
                Objects.requireNonNull(json, "json"), StandardCharsets.UTF_8)).parse());
        if (!SCHEMA.equals(string(root, "schema"))) {
            throw new IllegalArgumentException("unsupported activation bundle schema");
        }
        var decisions = new ArrayList<DecisionDefinition>();
        for (Object value : array(root, "decisions")) decisions.add(readDecision(object(value)));
        return new RuntimeActivationBundle(
                string(root, "boundaryFingerprint"), string(root, "javaAgentOption"),
                stringMap(object(root.get("classFingerprints"))), decisions);
    }

    /** One graph and the exact probe manifest that instruments it. */
    public record DecisionDefinition(BusinessDecisionGraph graph, AnalysisManifest manifest) {
        public DecisionDefinition {
            graph = Objects.requireNonNull(graph, "graph");
            manifest = Objects.requireNonNull(manifest, "manifest");
            if (!graph.graphId().equals(manifest.graphId()) || graph.version() != manifest.graphVersion()) {
                throw new IllegalArgumentException("graph and manifest versions do not match");
            }
        }
    }

    private static void decision(StringBuilder output, DecisionDefinition definition) {
        output.append("{\"graph\":"); graph(output, definition.graph());
        output.append(",\"manifest\":"); manifest(output, definition.manifest()); output.append('}');
    }

    private static void graph(StringBuilder output, BusinessDecisionGraph graph) {
        output.append('{'); field(output, "graphId", graph.graphId()).append(',');
        number(output, "version", graph.version()).append(',');
        field(output, "decisionLabel", graph.decisionLabel()).append(',');
        field(output, "entryNodeId", graph.entryNodeId()).append(',');
        field(output, "completeness", graph.completeness().name()).append(',');
        output.append("\"nodes\":[");
        for (int index = 0; index < graph.nodes().size(); index++) {
            if (index > 0) output.append(',');
            var node = graph.nodes().get(index);
            output.append('{'); field(output, "nodeId", node.nodeId()).append(',');
            field(output, "kind", node.kind().name()).append(',');
            field(output, "businessLabel", node.businessLabel()).append(',');
            output.append("\"attributes\":"); strings(output, node.attributes()); output.append('}');
        }
        output.append("],\"edges\":[");
        for (int index = 0; index < graph.edges().size(); index++) {
            if (index > 0) output.append(',');
            var edge = graph.edges().get(index);
            output.append('{'); field(output, "edgeId", edge.edgeId()).append(',');
            field(output, "fromNodeId", edge.fromNodeId()).append(',');
            field(output, "toNodeId", edge.toNodeId()).append(',');
            field(output, "outcome", edge.outcome()); output.append('}');
        }
        output.append("],\"coverageGaps\":[");
        for (int index = 0; index < graph.coverageGaps().size(); index++) {
            if (index > 0) output.append(',');
            var gap = graph.coverageGaps().get(index);
            output.append('{'); field(output, "nodeId", gap.nodeId()).append(',');
            field(output, "description", gap.description()); output.append('}');
        }
        output.append("]}");
    }

    private static void manifest(StringBuilder output, AnalysisManifest manifest) {
        output.append('{'); field(output, "graphId", manifest.graphId()).append(',');
        number(output, "graphVersion", manifest.graphVersion()).append(',');
        output.append("\"sourceMappings\":[");
        var mappings = manifest.sourceMappings().values().stream()
                .sorted(java.util.Comparator.comparing(AnalysisManifest.SourceMapping::nodeId)).toList();
        for (int index = 0; index < mappings.size(); index++) {
            if (index > 0) output.append(',');
            var mapping = mappings.get(index);
            output.append('{'); field(output, "nodeId", mapping.nodeId()).append(',');
            field(output, "source", mapping.source().toString()).append(',');
            number(output, "line", mapping.line()).append(','); number(output, "column", mapping.column()).append(',');
            field(output, "treeKind", mapping.treeKind()); output.append('}');
        }
        output.append("],\"probeSites\":[");
        for (int index = 0; index < manifest.probeSites().size(); index++) {
            if (index > 0) output.append(',');
            var site = manifest.probeSites().get(index);
            output.append('{'); field(output, "nodeId", site.nodeId()).append(',');
            field(output, "kind", site.kind().name()).append(',');
            field(output, "ownerHint", site.ownerHint()).append(',');
            field(output, "memberHint", site.memberHint()).append(',');
            number(output, "sourceLine", site.sourceLine()); output.append('}');
        }
        output.append("],\"dispatchTargets\":[");
        for (int index = 0; index < manifest.dispatchTargets().size(); index++) {
            if (index > 0) output.append(',');
            var target = manifest.dispatchTargets().get(index);
            output.append('{'); field(output, "dispatchNodeId", target.dispatchNodeId()).append(',');
            field(output, "edgeId", target.edgeId()).append(',');
            field(output, "ownerHint", target.ownerHint()).append(',');
            field(output, "memberHint", target.memberHint()); output.append('}');
        }
        output.append("],\"branchTargets\":[");
        for (int index = 0; index < manifest.branchTargets().size(); index++) {
            if (index > 0) output.append(',');
            var target = manifest.branchTargets().get(index);
            output.append('{'); field(output, "nodeId", target.nodeId()).append(',');
            field(output, "trueEdgeId", target.trueEdgeId()).append(',');
            field(output, "falseEdgeId", target.falseEdgeId()).append(',');
            field(output, "ownerHint", target.ownerHint()).append(',');
            field(output, "memberHint", target.memberHint()).append(',');
            number(output, "sourceLine", target.sourceLine()).append(',');
            number(output, "predicateIndex", target.predicateIndex()).append(',');
            field(output, "completion", target.completion().name()); output.append('}');
        }
        output.append("],\"sourceFingerprints\":"); strings(output, manifest.sourceFingerprints());
        output.append('}');
    }

    private static DecisionDefinition readDecision(Map<String, Object> value) {
        return new DecisionDefinition(readGraph(object(value.get("graph"))),
                readManifest(object(value.get("manifest"))));
    }

    private static BusinessDecisionGraph readGraph(Map<String, Object> value) {
        var nodes = new ArrayList<BusinessDecisionGraph.DecisionNode>();
        for (Object raw : array(value, "nodes")) {
            Map<String, Object> node = object(raw);
            nodes.add(new BusinessDecisionGraph.DecisionNode(string(node, "nodeId"),
                    BusinessDecisionGraph.NodeKind.valueOf(string(node, "kind")),
                    string(node, "businessLabel"), stringMap(object(node.get("attributes")))));
        }
        var edges = new ArrayList<BusinessDecisionGraph.DecisionEdge>();
        for (Object raw : array(value, "edges")) {
            Map<String, Object> edge = object(raw);
            edges.add(new BusinessDecisionGraph.DecisionEdge(string(edge, "edgeId"),
                    string(edge, "fromNodeId"), string(edge, "toNodeId"), string(edge, "outcome")));
        }
        var gaps = new ArrayList<BusinessDecisionGraph.CoverageGap>();
        for (Object raw : array(value, "coverageGaps")) {
            Map<String, Object> gap = object(raw);
            gaps.add(new BusinessDecisionGraph.CoverageGap(
                    string(gap, "nodeId"), string(gap, "description")));
        }
        return new BusinessDecisionGraph(string(value, "graphId"), number(value, "version"),
                string(value, "decisionLabel"), string(value, "entryNodeId"), nodes, edges,
                BusinessDecisionGraph.Completeness.valueOf(string(value, "completeness")), gaps);
    }

    private static AnalysisManifest readManifest(Map<String, Object> value) {
        var mappings = new LinkedHashMap<String, AnalysisManifest.SourceMapping>();
        for (Object raw : array(value, "sourceMappings")) {
            Map<String, Object> item = object(raw); String nodeId = string(item, "nodeId");
            mappings.put(nodeId, new AnalysisManifest.SourceMapping(nodeId, Path.of(string(item, "source")),
                    number(item, "line"), number(item, "column"), string(item, "treeKind")));
        }
        var sites = new ArrayList<AnalysisManifest.ProbeSite>();
        for (Object raw : array(value, "probeSites")) {
            Map<String, Object> item = object(raw);
            sites.add(new AnalysisManifest.ProbeSite(string(item, "nodeId"),
                    AnalysisManifest.ProbeKind.valueOf(string(item, "kind")),
                    string(item, "ownerHint"), string(item, "memberHint"), number(item, "sourceLine")));
        }
        var dispatches = new ArrayList<AnalysisManifest.DispatchTarget>();
        for (Object raw : array(value, "dispatchTargets")) {
            Map<String, Object> item = object(raw);
            dispatches.add(new AnalysisManifest.DispatchTarget(string(item, "dispatchNodeId"),
                    string(item, "edgeId"), string(item, "ownerHint"), string(item, "memberHint")));
        }
        var branches = new ArrayList<AnalysisManifest.BranchTarget>();
        for (Object raw : array(value, "branchTargets")) {
            Map<String, Object> item = object(raw);
            branches.add(new AnalysisManifest.BranchTarget(string(item, "nodeId"),
                    string(item, "trueEdgeId"), string(item, "falseEdgeId"),
                    string(item, "ownerHint"), string(item, "memberHint"), number(item, "sourceLine"),
                    Math.toIntExact(number(item, "predicateIndex")),
                    AnalysisManifest.BranchCompletion.valueOf(string(item, "completion"))));
        }
        return new AnalysisManifest(string(value, "graphId"), number(value, "graphVersion"), mappings,
                sites, dispatches, branches, stringMap(object(value.get("sourceFingerprints"))));
    }

    private static StringBuilder field(StringBuilder output, String name, String value) {
        quote(output, name).append(':'); return quote(output, value);
    }
    private static StringBuilder number(StringBuilder output, String name, long value) {
        quote(output, name).append(':').append(value); return output;
    }
    private static void strings(StringBuilder output, Map<String, String> values) {
        output.append('{'); var entries = values.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) output.append(',');
            quote(output, entries.get(index).getKey()).append(':'); quote(output, entries.get(index).getValue());
        }
        output.append('}');
    }
    private static StringBuilder quote(StringBuilder output, String value) {
        output.append('"');
        for (char character : value.toCharArray()) switch (character) {
            case '"' -> output.append("\\\""); case '\\' -> output.append("\\\\");
            case '\n' -> output.append("\\n"); case '\r' -> output.append("\\r");
            case '\t' -> output.append("\\t"); default -> {
                if (character < 0x20) output.append(String.format("\\u%04x", (int) character));
                else output.append(character);
            }
        }
        return output.append('"');
    }

    @SuppressWarnings("unchecked") private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("expected JSON object");
        return (Map<String, Object>) value;
    }
    @SuppressWarnings("unchecked") private static List<Object> array(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof List<?>)) throw new IllegalArgumentException("expected JSON array: " + key);
        return (List<Object>) raw;
    }
    private static String string(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof String text)) throw new IllegalArgumentException("expected JSON string: " + key);
        return text;
    }
    private static long number(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof Number number)) throw new IllegalArgumentException("expected JSON number: " + key);
        return number.longValue();
    }
    private static Map<String, String> stringMap(Map<String, Object> value) {
        var result = new LinkedHashMap<String, String>();
        value.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            if (!(entry.getValue() instanceof String text)) throw new IllegalArgumentException("expected string map");
            result.put(entry.getKey(), text);
        });
        return result;
    }
    private static String text(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static final class Parser {
        private final String input; private int index;
        private Parser(String input) { this.input = input; }
        private Object parse() { Object value = value(); whitespace(); if (index != input.length()) error(); return value; }
        private Object value() {
            whitespace(); if (index >= input.length()) error(); char current = input.charAt(index);
            if (current == '{') return object(); if (current == '[') return array();
            if (current == '"') return string(); if (input.startsWith("null", index)) { index += 4; return null; }
            if (input.startsWith("true", index)) { index += 4; return true; }
            if (input.startsWith("false", index)) { index += 5; return false; }
            return number();
        }
        private Map<String, Object> object() {
            index++; var values = new LinkedHashMap<String, Object>(); whitespace();
            if (take('}')) return values;
            do { String key = string(); whitespace(); expect(':'); values.put(key, value()); whitespace(); }
            while (take(',')); expect('}'); return values;
        }
        private List<Object> array() {
            index++; var values = new ArrayList<>(); whitespace(); if (take(']')) return values;
            do { values.add(value()); whitespace(); } while (take(',')); expect(']'); return values;
        }
        private String string() {
            expect('"'); var value = new StringBuilder();
            while (index < input.length()) { char current = input.charAt(index++); if (current == '"') return value.toString();
                if (current != '\\') { value.append(current); continue; } char escaped = input.charAt(index++);
                switch (escaped) { case '"', '\\', '/' -> value.append(escaped); case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f'); case 'n' -> value.append('\n'); case 'r' -> value.append('\r');
                    case 't' -> value.append('\t'); case 'u' -> { value.append((char) Integer.parseInt(
                            input.substring(index, index + 4), 16)); index += 4; } default -> error(); }
            } error(); return "";
        }
        private Number number() {
            int start = index; if (input.charAt(index) == '-') index++;
            while (index < input.length() && Character.isDigit(input.charAt(index))) index++;
            return Long.parseLong(input.substring(start, index));
        }
        private void whitespace() { while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++; }
        private boolean take(char value) { if (index < input.length() && input.charAt(index) == value) { index++; return true; } return false; }
        private void expect(char value) { whitespace(); if (!take(value)) error(); }
        private void error() { throw new IllegalArgumentException("invalid activation JSON at offset " + index); }
    }
}
