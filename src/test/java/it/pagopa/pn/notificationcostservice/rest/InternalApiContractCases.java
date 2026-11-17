package it.pagopa.pn.notificationcostservice.rest;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class InternalApiContractCases {
    record Endpoint(String template, Request.Method method, Operation operation, JsonNode fixture) {
        String id() {
            return operation.getOperationId();
        }

        String path() {
            String path = template;
            var values = fixture.path("path").fields();
            while (values.hasNext()) {
                var entry = values.next();
                path = path.replace("{" + entry.getKey() + "}", entry.getValue().asText());
            }
            assertFalse(path.contains("{"), "Missing path fixture for " + id());
            return path;
        }

        JsonNode body() {
            return fixture.get("body");
        }

        int success() {
            return operation.getResponses().keySet().stream().filter(code -> code.startsWith("2"))
                    .mapToInt(Integer::parseInt).findFirst().orElseThrow();
        }

        SimpleRequest request(String path, JsonNode body) {
            var builder = new SimpleRequest.Builder(method, path);
            if (body != null) {
                builder.withContentType("application/json").withBody(body.toString());
            }
            return builder.build();
        }
    }

    record Variant(String name, JsonNode body, boolean valid) {}

    final OpenAPI api;
    final OpenApiInteractionValidator validator;
    final List<Endpoint> endpoints = new ArrayList<>();

    InternalApiContractCases() throws IOException {
        api = InternalApiSpecification.load(Path.of("docs/openapi/api-private.yaml"));
        validator = OpenApiInteractionValidator.createFor(api).build();
        try (var input = getClass().getResourceAsStream("/contracts/internal-api.json")) {
            assertNotNull(input, "Missing contract fixtures");
            JsonNode fixtures = Json.mapper().readTree(input);
            Set<String> used = new LinkedHashSet<>();
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                assertNotNull(operation.getOperationId(), "Every operation needs an operationId");
                assertTrue(used.add(operation.getOperationId()), "Duplicate operationId");
                JsonNode fixture = fixtures.get(operation.getOperationId());
                assertNotNull(fixture, "Missing fixture for " + operation.getOperationId());
                endpoints.add(new Endpoint(path, Request.Method.valueOf(method.name()), operation, fixture));
            }));
            Set<String> declared = new LinkedHashSet<>();
            fixtures.fieldNames().forEachRemaining(declared::add);
            assertEquals(used, declared, "Fixtures must cover exactly the exposed operations");
        }
        assertFalse(endpoints.isEmpty(), "No operations discovered");
        endpoints.forEach(endpoint -> assertFalse(validator.validateRequest(
                endpoint.request(endpoint.path(), endpoint.body())).hasErrors(),
                "Invalid baseline fixture for " + endpoint.id()));
    }

    List<Variant> bodyVariants(Endpoint endpoint) {
        if (endpoint.body() == null) {
            return List.of();
        }
        Schema<?> schema = endpoint.operation().getRequestBody().getContent()
                .get("application/json").getSchema();
        List<Variant> variants = new ArrayList<>();
        collect(Json.mapper().valueToTree(schema), endpoint.body(), endpoint.body(), "", variants);
        assertFalse(variants.isEmpty(), "No body constraints exercised for " + endpoint.id());
        return variants;
    }

    List<Variant> responseVariants(Endpoint endpoint, JsonNode body) {
        Schema<?> schema = endpoint.operation().getResponses().get("200").getContent()
                .get("application/json").getSchema();
        List<Variant> variants = new ArrayList<>();
        collect(Json.mapper().valueToTree(schema), body, body, "", variants);
        return variants;
    }

    private void collect(JsonNode schema, JsonNode value, JsonNode root, String pointer, List<Variant> variants) {
        assertFalse(schema.has("$ref"), "Unresolved reference at " + pointer);
        assertFalse(schema.has("oneOf") || schema.has("anyOf"),
                "Add explicit variant coverage for union at " + pointer);
        Map<String, JsonNode> properties = new LinkedHashMap<>();
        Set<String> required = new LinkedHashSet<>();
        collectProperties(schema, properties, required);
        if (!properties.isEmpty()) {
            assertTrue(value.isObject(), "Object fixture required at " + pointer);
            for (String field : required) {
                JsonNode copy = root.deepCopy();
                ((ObjectNode) copy.at(pointer)).remove(field);
                variants.add(new Variant(pointer + "/" + field + " missing", copy, false));
            }
            properties.forEach((field, child) -> {
                String childPointer = pointer + "/" + field.replace("~", "~0").replace("/", "~1");
                assertTrue(value.has(field), "Provide a fixture for optional property " + childPointer);
                if (!required.contains(field)) {
                    JsonNode copy = root.deepCopy();
                    ((ObjectNode) copy.at(pointer)).remove(field);
                    variants.add(new Variant(childPointer + " omitted optional", copy, true));
                }
                collect(child, value.get(field), root, childPointer, variants);
            });
        }
        if (!pointer.isEmpty() && !schema.path("nullable").asBoolean()) {
            add(variants, root, pointer, "null", JsonNodeFactory.instance.nullNode(), false);
        }
        String type = schema.path("type").asText();
        if (type.isEmpty() && !properties.isEmpty()) {
            type = "object";
        }
        switch (type) {
            case "object", "array", "integer", "number", "boolean" ->
                    add(variants, root, pointer, "wrong type",
                            JsonNodeFactory.instance.textNode("not-a-" + type), false);
            case "string" -> add(variants, root, pointer, "wrong type",
                    JsonNodeFactory.instance.numberNode(123), false);
            case "" -> assertTrue(schema.has("allOf"), "Unsupported schema at " + pointer);
            default -> fail("Unsupported type " + type + " at " + pointer);
        }
        if ("integer".equals(type)) {
            add(variants, root, pointer, "numeric string", JsonNodeFactory.instance.textNode("1"), false);
            add(variants, root, pointer, "fraction", JsonNodeFactory.instance.numberNode(1.5), false);
            if ("int32".equals(schema.path("format").asText())) {
                if (!schema.has("minimum")) {
                    add(variants, root, pointer, "int32 lower bound",
                            JsonNodeFactory.instance.numberNode(Integer.MIN_VALUE), true);
                }
                if (!schema.has("maximum")) {
                    add(variants, root, pointer, "int32 upper bound",
                            JsonNodeFactory.instance.numberNode(Integer.MAX_VALUE), true);
                }
                add(variants, root, pointer, "int32 overflow",
                        JsonNodeFactory.instance.numberNode((long) Integer.MAX_VALUE + 1), false);
                add(variants, root, pointer, "int32 underflow",
                        JsonNodeFactory.instance.numberNode((long) Integer.MIN_VALUE - 1), false);
            }
        }
        if ("boolean".equals(type)) {
            add(variants, root, pointer, "boolean string", JsonNodeFactory.instance.textNode("true"), false);
        }
        if (schema.has("enum")) {
            add(variants, root, pointer, "unknown enum", JsonNodeFactory.instance.textNode("UNKNOWN"), false);
            schema.get("enum").forEach(choice -> add(variants, root, pointer, "enum " + choice, choice, true));
        }
        if (schema.has("pattern")) {
            add(variants, root, pointer, "pattern", JsonNodeFactory.instance.textNode("!invalid!"), false);
            if (schema.path("pattern").asText().startsWith("^RECINDEX_")) {
                add(variants, root, pointer, "pattern upper bound", JsonNodeFactory.instance.textNode("RECINDEX_99"), true);
                add(variants, root, pointer, "pattern outside", JsonNodeFactory.instance.textNode("RECINDEX_100"), false);
            }
        }
        if ("date-time".equals(schema.path("format").asText())) {
            add(variants, root, pointer, "date-time", JsonNodeFactory.instance.textNode("not-a-date"), false);
        }
        for (String bound : List.of("minimum", "maximum")) {
            if (schema.has(bound)) {
                int limit = schema.get(bound).intValue();
                add(variants, root, pointer, bound, JsonNodeFactory.instance.numberNode(limit), true);
                add(variants, root, pointer, bound + " outside",
                        JsonNodeFactory.instance.numberNode(limit + (bound.equals("minimum") ? -1 : 1)), false);
            }
        }
        if (value.isArray()) {
            assertFalse(value.isEmpty(), "Non-empty baseline array required at " + pointer);
            if (!schema.has("minItems")) {
                add(variants, root, pointer, "empty allowed", JsonNodeFactory.instance.arrayNode(), true);
            }
            if (!schema.path("uniqueItems").asBoolean()) {
                add(variants, root, pointer, "duplicates allowed", arrayOf(value.get(0), 2), true);
            }
            for (String bound : List.of("minItems", "maxItems")) {
                if (schema.has(bound)) {
                    int limit = schema.get(bound).intValue();
                    add(variants, root, pointer, bound, arrayOf(value.get(0), limit), true);
                    add(variants, root, pointer, bound + " outside",
                            arrayOf(value.get(0), limit + (bound.equals("minItems") ? -1 : 1)), false);
                }
            }
            collect(schema.get("items"), value.get(0), root, pointer + "/0", variants);
        }
    }

    private void collectProperties(JsonNode schema, Map<String, JsonNode> properties, Set<String> required) {
        var fields = schema.path("properties").fields();
        while (fields.hasNext()) {
            var field = fields.next();
            properties.put(field.getKey(), field.getValue());
        }
        schema.path("required").forEach(field -> required.add(field.asText()));
        schema.path("allOf").forEach(part -> collectProperties(part, properties, required));
    }

    private ArrayNode arrayOf(JsonNode item, int count) {
        assertTrue(count >= 0 && count <= 1000, "Add bounded fixture strategy for cardinality " + count);
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (int index = 0; index < count; index++) {
            array.add(item.deepCopy());
        }
        return array;
    }

    private void add(List<Variant> variants, JsonNode root, String pointer, String name,
                     JsonNode replacement, boolean valid) {
        if (pointer.isEmpty()) {
            variants.add(new Variant("body " + name, replacement, valid));
            return;
        }
        JsonNode copy = root.deepCopy();
        int slash = pointer.lastIndexOf('/');
        JsonNode parent = copy.at(pointer.substring(0, slash));
        String field = pointer.substring(slash + 1).replace("~1", "/").replace("~0", "~");
        if (parent instanceof ObjectNode object) {
            object.set(field, replacement);
        } else if (parent instanceof ArrayNode array) {
            array.set(Integer.parseInt(field), replacement);
        } else {
            fail("No parent for mutation " + pointer);
        }
        variants.add(new Variant(pointer + " " + name, copy, valid));
    }
}
