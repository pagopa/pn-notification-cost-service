package it.pagopa.pn.notificationcostservice.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class InternalApiSpecification {
    private static final Set<String> LITERAL_FIELDS = Set.of("example", "examples", "default", "enum");
    private final Map<URI, JsonNode> documents = new HashMap<>();
    private final URI source;

    private InternalApiSpecification(Path source) {
        this.source = source.toAbsolutePath().normalize().toUri();
    }

    static OpenAPI load(Path source) throws IOException {
        var options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        var parsed = new OpenAPIV3Parser().readContents(Json.mapper().writeValueAsString(normalize(source)), null, options);
        if (parsed.getOpenAPI() == null || (parsed.getMessages() != null && !parsed.getMessages().isEmpty())) {
            throw new IOException("Cannot prepare internal OpenAPI: " + parsed.getMessages());
        }
        return parsed.getOpenAPI();
    }

    static JsonNode normalize(Path source) throws IOException {
        var resolver = new InternalApiSpecification(source);
        JsonNode normalized = resolver.document(resolver.source).deepCopy();
        // Same-named external aliases otherwise become self-references in Swagger Parser.
        resolver.normalizeReferences(normalized);
        return normalized;
    }

    private void normalizeReferences(JsonNode node) throws IOException {
        if (node instanceof ObjectNode object) {
            if (object.path("$ref").isTextual()) {
                URI resolved = terminalReference(source.resolve(object.get("$ref").asText()).normalize());
                URI location = documentUri(resolved);
                String fragment = resolved.getRawFragment() == null ? "" : "#" + resolved.getRawFragment();
                if (location.equals(source)) {
                    object.put("$ref", fragment.isEmpty() ? "#" : fragment);
                } else if ("file".equals(location.getScheme())) {
                    // Swagger Parser accepts local filesystem paths, but not file: URIs.
                    object.put("$ref", Path.of(location) + fragment);
                } else {
                    object.put("$ref", resolved.toString());
                }
            }
            var fields = object.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                if (!LITERAL_FIELDS.contains(field.getKey())) {
                    normalizeReferences(field.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                normalizeReferences(item);
            }
        }
    }

    private URI terminalReference(URI reference) throws IOException {
        Set<URI> visited = new LinkedHashSet<>();
        while (true) {
            if (!visited.add(reference)) {
                throw new IOException("OpenAPI alias cycle: " + visited + " -> " + reference);
            }
            URI location = documentUri(reference);
            JsonNode target = document(location);
            String pointer = reference.getFragment();
            if (pointer != null && !pointer.isEmpty()) {
                if (!pointer.startsWith("/")) {
                    throw new IOException("Unsupported OpenAPI reference fragment: " + reference);
                }
                target = target.at(pointer);
            }
            if (target.isMissingNode()) {
                throw new IOException("Missing OpenAPI reference target: " + reference);
            }
            if (target.isObject() && target.size() == 1 && target.path("$ref").isTextual()) {
                reference = location.resolve(target.get("$ref").asText()).normalize();
            } else {
                return reference;
            }
        }
    }

    private JsonNode document(URI location) throws IOException {
        JsonNode cached = documents.get(location);
        if (cached != null) {
            return cached;
        }
        if (!Set.of("file", "http", "https").contains(location.getScheme())) {
            throw new IOException("Unsupported OpenAPI reference location: " + location);
        }
        var connection = location.toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        try (var input = connection.getInputStream()) {
            JsonNode document = Yaml.mapper().readTree(input);
            if (document == null || document.isNull()) {
                throw new IOException("Empty OpenAPI reference document: " + location);
            }
            documents.put(location, document);
            return document;
        }
    }

    private URI documentUri(URI reference) {
        return URI.create(reference.toString().split("#", 2)[0]);
    }
}
