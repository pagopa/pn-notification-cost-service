package it.pagopa.pn.notificationcostservice.rest;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InternalApiContractCasesTest {
    private static InternalApiContractCases contract;
    private static final String PATH = "/notification-cost-private/cost/ABCD-EFGH-ILMN-000001-A-1/recipient/0";

    @BeforeAll
    static void loadContract() throws IOException {
        contract = new InternalApiContractCases();
    }

    @Test
    void acceptsAValidProblemResolvedThroughTheCanonicalSpecification() {
        var report = contract.validator.validateResponse(PATH, Request.Method.GET,
                SimpleResponse.Builder.status(404).withContentType("application/problem+json")
                        .withBody("{\"status\":404,\"errors\":[{\"code\":\"NOT_FOUND\"}]}").build());
        assertFalse(report.hasErrors(), report.getMessages().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"status\":404,\"errors\":[]}",
            "{\"status\":404}",
            "{\"status\":404,\"errors\":[{}]}",
            "{\"status\":\"404\",\"errors\":[{\"code\":\"NOT_FOUND\"}]}",
            "{\"errors\":[{\"code\":\"NOT_FOUND\"}]}",
            "{\"status\":600,\"errors\":[{\"code\":\"NOT_FOUND\"}]}"
    })
    void rejectsProblemConstraintViolationsRatherThanFailingReferenceResolution(String body) {
        var report = contract.validator.validateResponse(PATH, Request.Method.GET,
                SimpleResponse.Builder.status(404).withContentType("application/problem+json").withBody(body).build());
        assertTrue(report.hasErrors(), "Invalid Problem must be rejected: " + body);
        assertFalse(report.getMessages().toString().contains("loops on itself"), report.getMessages().toString());
    }

    @Test
    void normalizesRelativeAliasChainsWithoutChangingRecursiveObjectsExamplesOrSourceFiles(@TempDir Path directory)
            throws IOException {
        Files.createDirectory(directory.resolve("refs"));
        Files.writeString(directory.resolve("refs/aliases.yaml"), "Alias:\n  $ref: '../types.yaml#/Concrete'\n");
        Files.writeString(directory.resolve("types.yaml"), "Concrete:\n  type: string\n  minLength: 3\n");
        String original = """
                openapi: 3.0.3
                info:
                  title: Alias regression
                  version: '1'
                paths: {}
                components:
                  schemas:
                    Alias:
                      $ref: 'refs/aliases.yaml#/Alias'
                      description: Kept on the original reference
                    Node:
                      type: object
                      properties:
                        child:
                          $ref: '#/components/schemas/Node'
                      example:
                        $ref: 'literal-example-not-a-reference.yaml'
                """;
        Path source = directory.resolve("api.yaml");
        Files.writeString(source, original);

        var normalized = InternalApiSpecification.normalize(source);

        assertEquals(directory.resolve("types.yaml") + "#/Concrete",
                normalized.at("/components/schemas/Alias/$ref").asText());
        assertEquals("Kept on the original reference", normalized.at("/components/schemas/Alias/description").asText());
        assertEquals("#/components/schemas/Node", normalized.at("/components/schemas/Node/properties/child/$ref").asText());
        assertEquals("literal-example-not-a-reference.yaml", normalized.at("/components/schemas/Node/example/$ref").asText());
        assertEquals(original, Files.readString(source));
        assertEquals(3, InternalApiSpecification.load(source).getComponents().getSchemas().get("Alias").getMinLength());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "A:\n  $ref: '#/components/schemas/B'\nB:\n  $ref: '#/components/schemas/A'\n",
            "A:\n  $ref: '#/components/schemas/Missing'\n"
    })
    void rejectsAliasCyclesAndMissingTargetsDuringPreparation(String schemas, @TempDir Path directory) throws IOException {
        Path source = directory.resolve("api.yaml");
        Files.writeString(source, "components:\n  schemas:\n" + schemas.indent(4));
        var failure = assertThrows(IOException.class, () -> InternalApiSpecification.normalize(source));
        assertTrue(failure.getMessage().contains("alias cycle") || failure.getMessage().contains("Missing OpenAPI reference"),
                failure.getMessage());
    }
}
