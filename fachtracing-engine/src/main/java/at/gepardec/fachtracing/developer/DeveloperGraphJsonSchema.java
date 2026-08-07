package at.gepardec.fachtracing.developer;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.util.Arrays;
import java.util.Objects;

/** Generates the formal JSON Schema for supported developer graph documents. */
public final class DeveloperGraphJsonSchema {
    private static final String DIALECT = "https://json-schema.org/draft/2020-12/schema";

    /** Generates a deterministic JSON Schema for one developer graph data-schema identifier. */
    public String generate(String dataSchemaId) {
        Objects.requireNonNull(dataSchemaId, "dataSchemaId");
        return switch (dataSchemaId) {
            case DeveloperGraphExporter.SCHEMA -> generate(false);
            case DeveloperGraphExporter.SCHEMA_V2 -> generate(true);
            default -> throw new IllegalArgumentException(
                    "unsupported developer graph schema: " + dataSchemaId);
        };
    }

    private static String generate(boolean multiOrigin) {
        String dataSchemaId = multiOrigin
                ? DeveloperGraphExporter.SCHEMA_V2
                : DeveloperGraphExporter.SCHEMA;
        String version = multiOrigin ? "v2" : "v1";
        var output = new StringBuilder(8192);
        output.append("{\n");
        field(output, 1, "$schema", DIALECT, true);
        field(output, 1, "$id", "urn:fachtracing:schema:developer-graph:" + version, true);
        field(output, 1, "title", "Fachtracing developer graph " + version, true);
        field(output, 1, "type", "object", true);
        booleanField(output, 1, "additionalProperties", false, true);
        if (multiOrigin) {
            arrayField(output, 1, "required",
                    new String[]{"schema", "graph", "sourceOrigins", "sourceFiles"}, true);
        } else {
            arrayField(output, 1, "required",
                    new String[]{"schema", "graph", "sourceRevision", "sourceFiles"}, true);
        }
        properties(output, dataSchemaId, multiOrigin);
        output.append(",\n");
        definitions(output, multiOrigin);
        return output.append("\n}\n").toString();
    }

    private static void properties(StringBuilder output, String dataSchemaId, boolean multiOrigin) {
        indent(output, 1).append("\"properties\": {\n");
        indent(output, 2).append("\"schema\": {\"const\": ");
        string(output, dataSchemaId).append("},\n");
        ref(output, 2, "graph", "graph", true);
        if (multiOrigin) {
            indent(output, 2).append("\"sourceOrigins\": {\n");
            indent(output, 3).append("\"type\": \"array\",\n");
            indent(output, 3).append("\"minItems\": 1,\n");
            indent(output, 3).append("\"items\": {\"$ref\": \"#/$defs/sourceOrigin\"}\n");
            indent(output, 2).append("},\n");
        } else {
            ref(output, 2, "sourceRevision", "revision", true);
        }
        indent(output, 2).append("\"sourceFiles\": {\n");
        indent(output, 3).append("\"type\": \"array\",\n");
        indent(output, 3).append("\"items\": {\"$ref\": \"#/$defs/sourceFile\"}\n");
        indent(output, 2).append("}\n");
        indent(output, 1).append('}');
    }

    private static void definitions(StringBuilder output, boolean multiOrigin) {
        indent(output, 1).append("\"$defs\": {\n");
        graphDefinition(output);
        output.append(",\n");
        nodeDefinition(output);
        output.append(",\n");
        edgeDefinition(output);
        output.append(",\n");
        coverageGapDefinition(output);
        output.append(",\n");
        sourceDefinition(output, multiOrigin);
        output.append(",\n");
        sourceFileDefinition(output, multiOrigin);
        output.append(",\n");
        revisionDefinition(output);
        if (multiOrigin) {
            output.append(",\n");
            sourceOriginDefinition(output);
        }
        output.append('\n');
        indent(output, 1).append('}');
    }

    private static void graphDefinition(StringBuilder output) {
        definitionStart(output, "graph",
                "id", "version", "label", "entryNodeId", "completeness",
                "nodes", "edges", "coverageGaps");
        stringProperty(output, 4, "id", true, true);
        integerProperty(output, 4, "version", 1, true);
        stringProperty(output, 4, "label", true, true);
        stringProperty(output, 4, "entryNodeId", true, true);
        enumProperty(output, 4, "completeness",
                Arrays.stream(BusinessDecisionGraph.Completeness.values())
                        .map(Enum::name).toArray(String[]::new), true);
        arrayReferenceProperty(output, 4, "nodes", "node", true, true);
        arrayReferenceProperty(output, 4, "edges", "edge", false, true);
        arrayReferenceProperty(output, 4, "coverageGaps", "coverageGap", false, false);
        definitionEnd(output);
    }

    private static void nodeDefinition(StringBuilder output) {
        definitionStart(output, "node", "id", "kind", "label", "attributes");
        stringProperty(output, 4, "id", true, true);
        enumProperty(output, 4, "kind",
                Arrays.stream(BusinessDecisionGraph.NodeKind.values())
                        .map(Enum::name).toArray(String[]::new), true);
        stringProperty(output, 4, "label", true, true);
        indent(output, 4).append("\"attributes\": {\n");
        indent(output, 5).append("\"type\": \"object\",\n");
        indent(output, 5).append("\"additionalProperties\": {\"type\": \"string\"}\n");
        indent(output, 4).append("},\n");
        ref(output, 4, "source", "source", false);
        definitionEnd(output);
    }

    private static void edgeDefinition(StringBuilder output) {
        definitionStart(output, "edge", "id", "from", "to", "outcome");
        stringProperty(output, 4, "id", true, true);
        stringProperty(output, 4, "from", true, true);
        stringProperty(output, 4, "to", true, true);
        stringProperty(output, 4, "outcome", false, false);
        definitionEnd(output);
    }

    private static void coverageGapDefinition(StringBuilder output) {
        definitionStart(output, "coverageGap", "nodeId", "description");
        stringProperty(output, 4, "nodeId", true, true);
        stringProperty(output, 4, "description", true, false);
        definitionEnd(output);
    }

    private static void sourceDefinition(StringBuilder output, boolean multiOrigin) {
        if (multiOrigin) {
            definitionStart(output, "source",
                    "originId", "path", "line", "column", "syntaxKind", "sha256");
            stringProperty(output, 4, "originId", true, true);
        } else {
            definitionStart(output, "source",
                    "path", "line", "column", "syntaxKind", "sha256", "url");
        }
        stringProperty(output, 4, "path", true, true);
        integerProperty(output, 4, "line", null, true);
        integerProperty(output, 4, "column", null, true);
        stringProperty(output, 4, "syntaxKind", true, true);
        sha256Property(output, 4, "sha256", true);
        stringProperty(output, 4, "url", true, false);
        definitionEnd(output);
    }

    private static void sourceFileDefinition(StringBuilder output, boolean multiOrigin) {
        if (multiOrigin) {
            definitionStart(output, "sourceFile", "originId", "path", "sha256");
            stringProperty(output, 4, "originId", true, true);
        } else {
            definitionStart(output, "sourceFile", "path", "sha256");
        }
        stringProperty(output, 4, "path", true, true);
        sha256Property(output, 4, "sha256", false);
        definitionEnd(output);
    }

    private static void revisionDefinition(StringBuilder output) {
        definitionStart(output, "revision", "repository", "commit", "committedAt");
        stringProperty(output, 4, "repository", true, true);
        indent(output, 4).append("\"commit\": {\"type\": \"string\", ")
                .append("\"pattern\": \"^[0-9a-f]{40,64}$\"},\n");
        indent(output, 4).append("\"committedAt\": {\"type\": \"string\", ")
                .append("\"format\": \"date-time\"}\n");
        definitionEnd(output);
    }

    private static void sourceOriginDefinition(StringBuilder output) {
        definitionStart(output, "sourceOrigin", "id", "kind", "identity", "checksum");
        stringProperty(output, 4, "id", true, true);
        enumProperty(output, 4, "kind",
                Arrays.stream(DeveloperGraphExporter.OriginKind.values())
                        .map(Enum::name).toArray(String[]::new), true);
        stringProperty(output, 4, "identity", true, true);
        stringProperty(output, 4, "checksum", false, true);
        ref(output, 4, "revision", "revision", false);
        indent(output, 3).append("},\n");
        indent(output, 3).append("\"allOf\": [{\n");
        indent(output, 4).append("\"if\": {\"properties\": {\"kind\": {\"const\": \"GIT\"}}, ")
                .append("\"required\": [\"kind\"]},\n");
        indent(output, 4).append("\"then\": {\"required\": [\"revision\"]},\n");
        indent(output, 4).append("\"else\": {\"not\": {\"required\": [\"revision\"]}}\n");
        indent(output, 3).append("}]\n");
        indent(output, 2).append('}');
    }

    private static void definitionStart(StringBuilder output, String name, String... required) {
        indent(output, 2).append('"').append(name).append("\": {\n");
        field(output, 3, "type", "object", true);
        booleanField(output, 3, "additionalProperties", false, true);
        arrayField(output, 3, "required", required, true);
        indent(output, 3).append("\"properties\": {\n");
    }

    private static void definitionEnd(StringBuilder output) {
        output.append('\n');
        indent(output, 3).append("}\n");
        indent(output, 2).append('}');
    }

    private static void stringProperty(
            StringBuilder output,
            int level,
            String name,
            boolean nonEmpty,
            boolean comma) {
        indent(output, level).append('"').append(name).append("\": {\"type\": \"string\"");
        if (nonEmpty) output.append(", \"minLength\": 1");
        output.append('}');
        if (comma) output.append(',');
        output.append('\n');
    }

    private static void integerProperty(
            StringBuilder output,
            int level,
            String name,
            Integer minimum,
            boolean comma) {
        indent(output, level).append('"').append(name).append("\": {\"type\": \"integer\"");
        if (minimum != null) output.append(", \"minimum\": ").append(minimum);
        output.append('}');
        if (comma) output.append(',');
        output.append('\n');
    }

    private static void sha256Property(
            StringBuilder output,
            int level,
            String name,
            boolean comma) {
        indent(output, level).append('"').append(name)
                .append("\": {\"type\": \"string\", \"pattern\": \"^[0-9a-f]{64}$\"}");
        if (comma) output.append(',');
        output.append('\n');
    }

    private static void enumProperty(
            StringBuilder output,
            int level,
            String name,
            String[] values,
            boolean comma) {
        indent(output, level).append('"').append(name)
                .append("\": {\"type\": \"string\", \"enum\": ");
        array(output, values).append('}');
        if (comma) output.append(',');
        output.append('\n');
    }

    private static void arrayReferenceProperty(
            StringBuilder output,
            int level,
            String name,
            String definition,
            boolean nonEmpty,
            boolean comma) {
        indent(output, level).append('"').append(name).append("\": {\n");
        indent(output, level + 1).append("\"type\": \"array\",\n");
        if (nonEmpty) indent(output, level + 1).append("\"minItems\": 1,\n");
        indent(output, level + 1).append("\"items\": {\"$ref\": \"#/$defs/")
                .append(definition).append("\"}\n");
        indent(output, level).append('}');
        if (comma) output.append(',');
        output.append('\n');
    }

    private static void ref(
            StringBuilder output,
            int level,
            String property,
            String definition,
            boolean comma) {
        indent(output, level).append('"').append(property)
                .append("\": {\"$ref\": \"#/$defs/").append(definition).append("\"}");
        if (comma) output.append(',');
        output.append('\n');
    }

    private static void field(
            StringBuilder output,
            int level,
            String name,
            String value,
            boolean comma) {
        indent(output, level).append('"').append(name).append("\": ");
        string(output, value);
        if (comma) output.append(',');
        output.append('\n');
    }

    private static void booleanField(
            StringBuilder output,
            int level,
            String name,
            boolean value,
            boolean comma) {
        indent(output, level).append('"').append(name).append("\": ").append(value);
        if (comma) output.append(',');
        output.append('\n');
    }

    private static void arrayField(
            StringBuilder output,
            int level,
            String name,
            String[] values,
            boolean comma) {
        indent(output, level).append('"').append(name).append("\": ");
        array(output, values);
        if (comma) output.append(',');
        output.append('\n');
    }

    private static StringBuilder array(StringBuilder output, String[] values) {
        output.append('[');
        for (int index = 0; index < values.length; index++) {
            if (index > 0) output.append(", ");
            string(output, values[index]);
        }
        return output.append(']');
    }

    private static StringBuilder string(StringBuilder output, String value) {
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"' || character == '\\') output.append('\\');
            output.append(character);
        }
        return output.append('"');
    }

    private static StringBuilder indent(StringBuilder output, int level) {
        return output.append("  ".repeat(level));
    }
}
