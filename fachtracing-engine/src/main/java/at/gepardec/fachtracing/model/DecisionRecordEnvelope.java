package at.gepardec.fachtracing.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Versioned, storage-neutral, already-redacted decision record payload. */
public record DecisionRecordEnvelope(
        String recordId,
        DecisionExecution execution,
        String boundaryFingerprint,
        Map<String, DecisionExecution.DecisionValue> correlationKeys,
        String redactionPolicyId) {
    public static final String SCHEMA = "fachtracing-decision-record/v1";

    /** Creates an immutable envelope that is safe to queue for persistence. */
    public DecisionRecordEnvelope {
        recordId = text(recordId, "recordId");
        execution = Objects.requireNonNull(execution, "execution");
        boundaryFingerprint = text(boundaryFingerprint, "boundaryFingerprint");
        correlationKeys = Map.copyOf(Objects.requireNonNull(correlationKeys, "correlationKeys"));
        redactionPolicyId = text(redactionPolicyId, "redactionPolicyId");
    }

    /** Returns the deterministic UTF-8 JSON wire representation. */
    public byte[] toJson() { return Codec.write(this).getBytes(StandardCharsets.UTF_8); }

    /** Reads a V1 envelope and ignores unknown JSON fields for forward readability. */
    public static DecisionRecordEnvelope fromJson(byte[] json) {
        return Codec.read(new String(Objects.requireNonNull(json, "json"), StandardCharsets.UTF_8));
    }

    /** Protocol status derived without losing execution and completeness details. */
    public String status() {
        if (execution.terminalStatus() == DecisionExecution.TerminalStatus.FAILED) return "FAILED";
        return execution.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE
                ? "INCOMPLETE" : "SUCCEEDED";
    }

    private static String text(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static final class Codec {
        private static String write(DecisionRecordEnvelope envelope) {
            var out = new StringBuilder(2048).append('{');
            field(out, "schema", SCHEMA).append(',');
            field(out, "recordId", envelope.recordId()).append(',');
            field(out, "executionId", envelope.execution().executionId()).append(',');
            field(out, "graphId", envelope.execution().graphId()).append(',');
            number(out, "graphVersion", envelope.execution().graphVersion()).append(',');
            field(out, "boundaryFingerprint", envelope.boundaryFingerprint()).append(',');
            field(out, "startedAt", envelope.execution().startedAt().toString()).append(',');
            field(out, "completedAt", envelope.execution().completedAt().toString()).append(',');
            field(out, "status", envelope.status()).append(',');
            field(out, "terminalStatus", envelope.execution().terminalStatus().name()).append(',');
            field(out, "completeness", envelope.execution().completeness().name()).append(',');
            out.append("\"finalDecision\":");
            value(out, envelope.execution().finalResult()).append(',');
            out.append("\"failure\":");
            failure(out, envelope.execution().failure()).append(',');
            out.append("\"observations\":[");
            for (int i = 0; i < envelope.execution().observations().size(); i++) {
                if (i > 0) out.append(',');
                observation(out, envelope.execution().observations().get(i));
            }
            out.append("],\"coverageGaps\":[");
            strings(out, envelope.execution().coverageGaps());
            out.append("],\"correlationKeys\":");
            values(out, envelope.correlationKeys());
            out.append(',');
            field(out, "redactionPolicyId", envelope.redactionPolicyId());
            return out.append("}\n").toString();
        }

        private static DecisionRecordEnvelope read(String json) {
            Map<String, Object> root = object(new Parser(json).parse());
            if (!SCHEMA.equals(string(root, "schema"))) {
                throw new IllegalArgumentException("unsupported decision record schema");
            }
            var observations = new ArrayList<DecisionExecution.NodeObservation>();
            for (Object raw : array(root, "observations")) {
                Map<String, Object> item = object(raw);
                observations.add(new DecisionExecution.NodeObservation(
                        number(item, "sequence"), string(item, "nodeId"), string(item, "outcome"),
                        readValues(object(item.get("evidence"))), nullableString(item.get("selectedEdgeId"))));
            }
            DecisionExecution.DecisionValue finalValue = readValue(root.get("finalDecision"));
            DecisionExecution.FailureData failure = readFailure(root.get("failure"));
            var execution = new DecisionExecution(
                    string(root, "executionId"), string(root, "graphId"), number(root, "graphVersion"),
                    Instant.parse(string(root, "startedAt")), Instant.parse(string(root, "completedAt")),
                    observations, DecisionExecution.TerminalStatus.valueOf(string(root, "terminalStatus")),
                    finalValue, failure, BusinessDecisionGraph.Completeness.valueOf(string(root, "completeness")),
                    array(root, "coverageGaps").stream().map(String.class::cast).toList());
            return new DecisionRecordEnvelope(string(root, "recordId"), execution,
                    string(root, "boundaryFingerprint"),
                    readValues(object(root.get("correlationKeys"))), string(root, "redactionPolicyId"));
        }

        private static StringBuilder observation(StringBuilder out, DecisionExecution.NodeObservation item) {
            out.append('{');
            number(out, "sequence", item.sequence()).append(',');
            field(out, "nodeId", item.nodeId()).append(',');
            field(out, "outcome", item.outcome()).append(',');
            out.append("\"evidence\":"); values(out, item.evidence());
            out.append(",\"selectedEdgeId\":"); nullable(out, item.selectedEdgeId());
            return out.append('}');
        }

        private static StringBuilder values(StringBuilder out, Map<String, DecisionExecution.DecisionValue> map) {
            out.append('{');
            var entries = map.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) out.append(',');
                quote(out, entries.get(i).getKey()).append(':'); value(out, entries.get(i).getValue());
            }
            return out.append('}');
        }

        private static StringBuilder value(StringBuilder out, DecisionExecution.DecisionValue value) {
            if (value == null) return out.append("null");
            out.append('{'); field(out, "type", value.type()).append(',');
            field(out, "canonicalValue", value.canonicalValue()).append(',');
            field(out, "displayValue", value.displayValue()); return out.append('}');
        }

        private static StringBuilder failure(StringBuilder out, DecisionExecution.FailureData failure) {
            if (failure == null) return out.append("null");
            out.append('{'); field(out, "canonicalValue", failure.canonicalValue()).append(',');
            field(out, "displayValue", failure.displayValue()); return out.append('}');
        }

        private static void strings(StringBuilder out, List<String> values) {
            for (int i = 0; i < values.size(); i++) { if (i > 0) out.append(','); quote(out, values.get(i)); }
        }

        private static StringBuilder field(StringBuilder out, String name, String value) {
            quote(out, name).append(':'); return quote(out, value);
        }
        private static StringBuilder number(StringBuilder out, String name, long value) {
            quote(out, name).append(':').append(value); return out;
        }
        private static void nullable(StringBuilder out, String value) {
            if (value == null) out.append("null"); else quote(out, value);
        }
        private static StringBuilder quote(StringBuilder out, String value) {
            out.append('"');
            for (char c : value.toCharArray()) switch (c) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t"); default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c)); else out.append(c);
                }
            }
            return out.append('"');
        }

        private static Map<String, DecisionExecution.DecisionValue> readValues(Map<String, Object> values) {
            var result = new LinkedHashMap<String, DecisionExecution.DecisionValue>();
            values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> result.put(entry.getKey(), readValue(entry.getValue())));
            return result;
        }
        private static DecisionExecution.DecisionValue readValue(Object raw) {
            if (raw == null) return null; Map<String, Object> value = object(raw);
            return new DecisionExecution.DecisionValue(string(value, "type"),
                    string(value, "canonicalValue"), string(value, "displayValue"));
        }
        private static DecisionExecution.FailureData readFailure(Object raw) {
            if (raw == null) return null; Map<String, Object> value = object(raw);
            return new DecisionExecution.FailureData(
                    string(value, "canonicalValue"), string(value, "displayValue"));
        }
        @SuppressWarnings("unchecked") private static Map<String, Object> object(Object value) {
            if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("expected JSON object");
            return (Map<String, Object>) value;
        }
        @SuppressWarnings("unchecked") private static List<Object> array(Map<String, Object> map, String key) {
            Object value = map.get(key); if (!(value instanceof List<?>)) throw new IllegalArgumentException("expected JSON array: " + key);
            return (List<Object>) value;
        }
        private static String string(Map<String, Object> map, String key) {
            Object value = map.get(key); if (!(value instanceof String text)) throw new IllegalArgumentException("expected JSON string: " + key); return text;
        }
        private static String nullableString(Object value) { return value == null ? null : (String) value; }
        private static long number(Map<String, Object> map, String key) {
            Object value = map.get(key); if (!(value instanceof Number number)) throw new IllegalArgumentException("expected JSON number: " + key); return number.longValue();
        }

        private static final class Parser {
            private final String input; private int index;
            private Parser(String input) { this.input = input; }
            private Object parse() { Object value = value(); whitespace(); if (index != input.length()) error(); return value; }
            private Object value() {
                whitespace(); if (index >= input.length()) error(); char c = input.charAt(index);
                if (c == '{') return object(); if (c == '[') return array(); if (c == '"') return string();
                if (input.startsWith("null", index)) { index += 4; return null; }
                if (input.startsWith("true", index)) { index += 4; return true; }
                if (input.startsWith("false", index)) { index += 5; return false; }
                return number();
            }
            private Map<String, Object> object() {
                index++; var map = new LinkedHashMap<String, Object>(); whitespace();
                if (take('}')) return map;
                do { String key = string(); whitespace(); expect(':'); map.put(key, value()); whitespace(); } while (take(','));
                expect('}'); return map;
            }
            private List<Object> array() {
                index++; var list = new ArrayList<>(); whitespace(); if (take(']')) return list;
                do { list.add(value()); whitespace(); } while (take(',')); expect(']'); return list;
            }
            private String string() {
                expect('"'); var out = new StringBuilder();
                while (index < input.length()) { char c = input.charAt(index++); if (c == '"') return out.toString();
                    if (c != '\\') { out.append(c); continue; } char escaped = input.charAt(index++);
                    switch (escaped) { case '"', '\\', '/' -> out.append(escaped); case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f'); case 'n' -> out.append('\n'); case 'r' -> out.append('\r');
                        case 't' -> out.append('\t'); case 'u' -> { out.append((char) Integer.parseInt(input.substring(index, index + 4), 16)); index += 4; }
                        default -> error(); }
                } error(); return "";
            }
            private Number number() { int start = index; if (input.charAt(index) == '-') index++; while (index < input.length() && Character.isDigit(input.charAt(index))) index++; return Long.parseLong(input.substring(start, index)); }
            private void whitespace() { while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++; }
            private boolean take(char c) { if (index < input.length() && input.charAt(index) == c) { index++; return true; } return false; }
            private void expect(char c) { whitespace(); if (!take(c)) error(); }
            private void error() { throw new IllegalArgumentException("invalid decision record JSON at offset " + index); }
        }
    }
}
