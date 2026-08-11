package at.gepardec.fachtracing.conformance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Parses a JSON document and validates the JSON Schema features used by the business graph schema. */
final class BusinessJsonSchemaConformance {
    private BusinessJsonSchemaConformance() { }

    static void validate(String document, String schemaDocument) {
        Object value = new JsonParser(document).parse();
        Map<String, Object> schema = object(new JsonParser(schemaDocument).parse(), "schema");
        validateValue("$", value, schema);
    }

    private static void validateValue(String path, Object value, Map<String, Object> schema) {
        Object expectedType = schema.get("type");
        if (expectedType != null) requireType(path, value, string(expectedType, path + ".type"));
        if (schema.containsKey("const") && !schema.get("const").equals(value)) {
            throw failure(path, "does not equal the required constant");
        }
        if (schema.containsKey("enum")
                && !array(schema.get("enum"), path + ".enum").contains(value)) {
            throw failure(path, "is not an allowed value");
        }
        if (value instanceof String text) validateString(path, text, schema);
        if (value instanceof Long number) validateNumber(path, number, schema);
        if (value instanceof List<?> values) validateArray(path, values, schema);
        if (value instanceof Map<?, ?> values) validateObject(path, castObject(values), schema);
    }

    private static void validateString(String path, String value, Map<String, Object> schema) {
        if (schema.containsKey("minLength")
                && value.length() < number(schema.get("minLength"), path + ".minLength")) {
            throw failure(path, "is shorter than minLength");
        }
    }

    private static void validateNumber(String path, long value, Map<String, Object> schema) {
        if (schema.containsKey("minimum")
                && value < number(schema.get("minimum"), path + ".minimum")) {
            throw failure(path, "is below minimum");
        }
    }

    private static void validateArray(String path, List<?> values, Map<String, Object> schema) {
        if (schema.containsKey("minItems")
                && values.size() < number(schema.get("minItems"), path + ".minItems")) {
            throw failure(path, "has fewer items than minItems");
        }
        if (Boolean.TRUE.equals(schema.get("uniqueItems"))
                && new HashSet<>(values).size() != values.size()) {
            throw failure(path, "contains duplicate items");
        }
        if (schema.containsKey("items")) {
            Map<String, Object> itemSchema = object(schema.get("items"), path + ".items");
            for (int index = 0; index < values.size(); index++) {
                validateValue(path + '[' + index + ']', values.get(index), itemSchema);
            }
        }
    }

    private static void validateObject(
            String path, Map<String, Object> values, Map<String, Object> schema) {
        Map<String, Object> properties = schema.containsKey("properties")
                ? object(schema.get("properties"), path + ".properties") : Map.of();
        if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            Set<String> additional = new HashSet<>(values.keySet());
            additional.removeAll(properties.keySet());
            if (!additional.isEmpty()) {
                throw failure(path, "contains additional properties " + additional);
            }
        }
        if (schema.containsKey("required")) {
            for (Object required : array(schema.get("required"), path + ".required")) {
                String name = string(required, path + ".required");
                if (!values.containsKey(name)) {
                    throw failure(path, "does not contain required property " + name);
                }
            }
        }
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            if (values.containsKey(property.getKey())) {
                validateValue(path + '.' + property.getKey(), values.get(property.getKey()),
                        object(property.getValue(), path + ".properties." + property.getKey()));
            }
        }
    }

    private static void requireType(String path, Object value, String expected) {
        boolean valid = switch (expected) {
            case "object" -> value instanceof Map<?, ?>;
            case "array" -> value instanceof List<?>;
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Long;
            case "boolean" -> value instanceof Boolean;
            default -> throw new IllegalArgumentException("unsupported schema type: " + expected);
        };
        if (!valid) throw failure(path, "is not a " + expected);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castObject(Map<?, ?> value) {
        return (Map<String, Object>) value;
    }

    private static Map<String, Object> object(Object value, String path) {
        if (value instanceof Map<?, ?> map) return castObject(map);
        throw failure(path, "is not an object");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value, String path) {
        if (value instanceof List<?> list) return (List<Object>) list;
        throw failure(path, "is not an array");
    }

    private static String string(Object value, String path) {
        if (value instanceof String text) return text;
        throw failure(path, "is not a string");
    }

    private static long number(Object value, String path) {
        if (value instanceof Long number) return number;
        throw failure(path, "is not an integer");
    }

    private static AssertionError failure(String path, String message) {
        return new AssertionError(path + ' ' + message);
    }

    /** Test-only parser for complete JSON documents. */
    private static final class JsonParser {
        private final String input;
        private int index;

        private JsonParser(String input) {
            this.input = input;
        }

        private Object parse() {
            Object value = value();
            whitespace();
            if (index != input.length()) throw error("unexpected trailing content");
            return value;
        }

        private Object value() {
            whitespace();
            if (index >= input.length()) throw error("expected value");
            return switch (input.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            expect('{');
            var values = new LinkedHashMap<String, Object>();
            whitespace();
            if (take('}')) return values;
            do {
                whitespace();
                String name = string();
                whitespace();
                expect(':');
                if (values.containsKey(name)) throw error("duplicate object member");
                values.put(name, value());
                whitespace();
            } while (take(','));
            expect('}');
            return values;
        }

        private List<Object> array() {
            expect('[');
            var values = new ArrayList<>();
            whitespace();
            if (take(']')) return values;
            do {
                values.add(value());
                whitespace();
            } while (take(','));
            expect(']');
            return values;
        }

        private String string() {
            expect('"');
            var value = new StringBuilder();
            while (index < input.length()) {
                char character = input.charAt(index++);
                if (character == '"') return value.toString();
                if (character != '\\') {
                    if (character < 0x20) throw error("unescaped control character");
                    value.append(character);
                    continue;
                }
                if (index >= input.length()) throw error("incomplete escape");
                char escape = input.charAt(index++);
                switch (escape) {
                    case '"', '\\', '/' -> value.append(escape);
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append((char) Integer.parseInt(take(4), 16));
                    default -> throw error("invalid escape");
                }
            }
            throw error("unterminated string");
        }

        private Long number() {
            int start = index;
            if (take('-')) { /* The sign is optional. */ }
            while (index < input.length() && Character.isDigit(input.charAt(index))) index++;
            if (start == index) throw error("expected number");
            return Long.parseLong(input.substring(start, index));
        }

        private Object literal(String text, Object value) {
            if (!input.startsWith(text, index)) throw error("invalid literal");
            index += text.length();
            return value;
        }

        private String take(int count) {
            if (index + count > input.length()) throw error("incomplete escape");
            String value = input.substring(index, index + count);
            index += count;
            return value;
        }

        private boolean take(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw error("expected '" + expected + "'");
        }

        private void whitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at JSON offset " + index);
        }
    }
}
