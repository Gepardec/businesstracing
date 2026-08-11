package at.gepardec.fachtracing.business;

/** Supplies the stable Draft 2020-12 schema for business graph JSON. */
public final class BusinessGraphJsonSchema {
    /** Returns the schema text. */
    public String generate() {
        return """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$id": "fachtracing-business-graph/v1",
                  "title": "Fachtracing business graph",
                  "type": "object",
                  "additionalProperties": false,
                  "required": ["schema", "graphId", "version", "decision", "completeness", "entryNodeIds", "nodes", "edges"],
                  "properties": {
                    "schema": {"const": "fachtracing-business-graph/v1"},
                    "graphId": {"type": "string", "minLength": 1},
                    "version": {"type": "integer", "minimum": 1},
                    "decision": {"type": "string", "minLength": 1},
                    "completeness": {"enum": ["COMPLETE", "INCOMPLETE"]},
                    "entryNodeIds": {
                      "type": "array",
                      "minItems": 1,
                      "uniqueItems": true,
                      "items": {"type": "string", "minLength": 1}
                    },
                    "nodes": {
                      "type": "array",
                      "minItems": 1,
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": ["id", "kind", "label"],
                        "properties": {
                          "id": {"type": "string", "minLength": 1},
                          "kind": {"enum": ["RULE", "ACTION", "RESULT", "GAP"]},
                          "label": {"type": "string", "minLength": 1}
                        }
                      }
                    },
                    "edges": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": ["id", "from", "to", "outcome"],
                        "properties": {
                          "id": {"type": "string", "minLength": 1},
                          "from": {"type": "string", "minLength": 1},
                          "to": {"type": "string", "minLength": 1},
                          "outcome": {"type": "string"}
                        }
                      }
                    }
                  }
                }
                """;
    }
}
