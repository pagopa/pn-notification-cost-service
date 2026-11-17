package it.pagopa.pn.notificationcostservice.rest;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import it.pagopa.pn.notificationcostservice.PnNotificationCostServiceApplication;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.GenericContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = PnNotificationCostServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Import(MinistackApiContractIT.EnvironmentLifecycle.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class MinistackApiContractIT {
    static final GenericContainer<?> MINISTACK = MinistackApiEnvironment.container();
    private static MinistackApiEnvironment environment;
    private static MinistackFaultProxy proxy;
    private InternalApiContractCases contract;
    private MinistackFixtures fixtures;
    private final List<Observation> observations = new ArrayList<>();
    record Observation(String operation, String scenario, String path, int expected, int actual, List<String> violations) {}
    record Coverage(String operation, String status, boolean exercised, boolean observed, boolean schemaConformant) {}

    @DynamicPropertySource
    static void awsProperties(DynamicPropertyRegistry properties) throws Exception {
        assertTrue("test".equals(System.getProperty("aws.accessKeyId"))
                        && "test".equals(System.getProperty("aws.secretAccessKey")), "Run with -Papi-contract verify");
        try {
            MINISTACK.start();
            environment = new MinistackApiEnvironment(MINISTACK);
            environment.verifyCompatibility();
            proxy = new MinistackFaultProxy(environment.endpoint);
        } catch (Exception | AssertionError failure) {
            if (environment != null) {
                environment.close();
            }
            if (MINISTACK.isRunning()) {
                Files.createDirectories(Path.of("target/api-contract"));
                Files.writeString(Path.of("target/api-contract/ministack.log"), MINISTACK.getLogs());
            }
            MINISTACK.stop();
            throw failure;
        }
        properties.add("aws.endpoint-url", () -> proxy.endpoint().toString());
        properties.add("aws.region-code", () -> "us-east-1");
        properties.add("aws.profile-name", () -> "");
        properties.add("spring.cloud.aws.endpoint", () -> proxy.endpoint().toString());
        properties.add("spring.cloud.aws.region.static", () -> "us-east-1");
        properties.add("spring.cloud.aws.credentials.access-key", () -> "test");
        properties.add("spring.cloud.aws.credentials.secret-key", () -> "test");
        properties.add("spring.cloud.aws.credentials.profile-name", () -> "");
        properties.add("pn.notification-cost-service.notification-delivery-cost-table.table-name",
                () -> MinistackApiEnvironment.COSTS);
        properties.add("pn.notification-cost-service.payment-info-table.table-name", () -> MinistackApiEnvironment.PAYMENTS);
        properties.add("pn.notification-cost-service.topics.pn-notification-cost-to-update",
                () -> MinistackApiEnvironment.INITIALIZATION);
        properties.add("pn.notification-cost-service.topics.payments-to-verify",
                () -> MinistackApiEnvironment.VERIFY_PAYMENTS);
        properties.add("pn.notification-cost-service.event-bus.name", () -> MinistackApiEnvironment.BUS);
        properties.add("pn.notification-cost-service.event-bus.source", () -> MinistackApiEnvironment.SOURCE);
        properties.add("pn.notification-cost-service.event-bus.outcome-event-detail-type",
                () -> MinistackApiEnvironment.DETAIL_TYPE);
        properties.add("pn.notification-cost-service.event-bus.notice-payment-paid-detail-type", () -> "NoticePaymentPaidEvent");
        properties.add("pn.notification-cost-service.send-fee", () -> 100);
        properties.add("logging.config", () -> "classpath:logback-base.xml");
    }

    @Test
    void paymentWhoseCostIsAbsentReturnsNotFound() throws Exception {
        var endpoint = contract.endpoints.stream().filter(e -> e.id().equals("getNotificationCostByPayment"))
                .findFirst().orElseThrow();
        var scenario = fixtures.scenario(endpoint);
        fixtures.seed(scenario);
        environment.dynamo.deleteItem(r -> r.tableName(MinistackApiEnvironment.COSTS).key(java.util.Map.of(
                "pk", MinistackApiEnvironment.text(scenario.iun()), "sk", MinistackApiEnvironment.number(0))));
        checkResponse(scenario.endpoint(), exchange(scenario.endpoint(), scenario.endpoint().path(),
                null, "application/json", "*/*"), 404, "payment exists but cost absent");
    }

    @LocalServerPort
    int port;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @TestConfiguration(proxyBeanMethods = false)
    static class EnvironmentLifecycle {
        @Bean
        AutoCloseable contractEnvironmentCleanup() {
            return () -> {
                proxy.close();
                environment.close();
                MINISTACK.stop();
            };
        }
    }

    @BeforeAll
    void loadContract() throws Exception {
        contract = new InternalApiContractCases();
        fixtures = new MinistackFixtures(environment);
    }

    @Test
    void realHttpReachesTheCompleteApplication() throws Exception {
        var response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
                .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
    }

    @Test
    void openApiAndControllerExposeExactlyTheSameOperations() {
        Set<String> declared = contract.endpoints.stream().map(e -> e.method() + " " + e.template())
                .collect(Collectors.toSet());
        Set<String> exposed = mappings.getHandlerMethods().entrySet().stream()
                .filter(e -> NotificationCostServiceController.class.isAssignableFrom(e.getValue().getBeanType()))
                .flatMap(e -> e.getKey().getMethodsCondition().getMethods().stream().flatMap(method ->
                        e.getKey().getPatternsCondition().getPatterns().stream()
                                .map(path -> method + " " + path.getPatternString())))
                .collect(Collectors.toSet());
        assertEquals(declared, exposed, "Undocumented or missing internal operation");
    }

    @TestFactory
    Stream<DynamicTest> getResponsesAndAnalogRepresentations() {
        return contract.endpoints.stream().filter(e -> e.method() == Request.Method.GET).flatMap(endpoint ->
                Stream.of(0, 1, 2).map(attempts -> DynamicTest.dynamicTest(
                        endpoint.id() + " 200 analogAttempts=" + attempts, () -> {
                            var scenario = fixtures.scenario(endpoint);
                            fixtures.seed(scenario, attempts, NotificationFeePolicy.DELIVERY_MODE, true, false);
                            checkResponse(scenario.endpoint(), exchange(scenario.endpoint(), scenario.endpoint().path(),
                                    null, "application/json", "*/*"), 200, "analogAttempts=" + attempts);
                        })));
    }

    @TestFactory
    Stream<DynamicTest> invalidatingOneAttemptCanBeRepeatedWithoutChangingTheOther() {
        var baseline = contract.endpoints.stream().filter(e -> e.id().equals("invalidatePaperCost")).findFirst().orElseThrow();
        return Stream.of("SEND_ANALOG_DOMICILE_ATTEMPT_0", "SEND_ANALOG_DOMICILE_ATTEMPT_1").map(phase ->
                DynamicTest.dynamicTest(baseline.id() + " repeat single phase " + phase, () -> {
                    var scenario = fixtures.scenario(baseline);
                    ((ObjectNode) scenario.body()).putArray("costPhases").add(phase);
                    fixtures.seed(scenario);
                    var first = exchange(scenario.endpoint(), scenario.endpoint().path(), scenario.body(), "application/json", "*/*");
                    var repeated = exchange(scenario.endpoint(), scenario.endpoint().path(), scenario.body(), "application/json", "*/*");
                    assertAll(
                            () -> checkResponse(scenario.endpoint(), first, 204, "single phase " + phase),
                            () -> checkResponse(scenario.endpoint(), repeated, 204, "repeat single phase " + phase),
                            () -> verifySuccessfulEffect(scenario));
                }));
    }

    @TestFactory
    Stream<DynamicTest> everyDocumentedOperationAndStatus() {
        return contract.endpoints.stream().flatMap(endpoint -> endpoint.operation().getResponses().keySet().stream()
                .map(code -> DynamicTest.dynamicTest(endpoint.id() + " HTTP " + code, () -> {
                    int expected = Integer.parseInt(code);
                    var scenario = fixtures.scenario(endpoint);
                    var actual = scenario.endpoint();
                    String path = actual.path();
                    if (expected == 400) {
                        var parameter = actual.operation().getParameters().getFirst();
                        path = path.replace(actual.fixture().path("path").get(parameter.getName()).asText(), "!invalid!");
                        assertTrue(contract.validator.validateRequest(actual.request(path, actual.body())).hasErrors());
                    } else {
                        assertFalse(contract.validator.validateRequest(actual.request(path, actual.body())).hasErrors());
                        assertTrue(expected == actual.success() || expected == 404 || expected == 500,
                                "Add an explicit scenario for new status " + code);
                        if (expected != 404 && actual.method() != Request.Method.POST) {
                            fixtures.seed(scenario);
                        }
                    }
                    if (expected == 500) {
                        exerciseBackendFault(scenario);
                    } else {
                        int before = proxy.calls.size();
                        var response = exchange(actual, path, actual.body(), "application/json", "*/*");
                        assertAll(
                                () -> checkResponse(actual, response, expected, "documented " + expected),
                                () -> {
                                    if (expected == actual.success()) {
                                        verifySuccessfulEffect(scenario);
                                    } else if (expected == 400) {
                                        assertNoWrites(scenario, before);
                                    }
                                });
                    }
                })));
    }

    private void exerciseBackendFault(MinistackFixtures.Scenario scenario) throws Exception {
        var endpoint = scenario.endpoint();
        String operation = switch (endpoint.method()) {
            case GET -> "GetItem";
            case PUT -> "UpdateItem";
            case POST -> "SendMessage";
            default -> throw new AssertionError("Add fault for " + endpoint.method());
        };
        String correlation = endpoint.id().equals("getNotificationCostByPayment") ? scenario.iuv() : scenario.iun();
        proxy.fail(operation, correlation);
        HttpResponse<String> response;
        int injected;
        try {
            response = exchange(endpoint, endpoint.path(), endpoint.body(), "application/json", "*/*");
        } finally {
            injected = proxy.recover();
        }
        assertTrue(injected > 0, "Fault must reach the real SDK operation " + operation);
        // Recovery uses the same operation and fixture, not just the emulator health endpoint.
        var recovered = exchange(endpoint, endpoint.path(), endpoint.body(), "application/json", "*/*");
        assertAll(
                () -> checkResponse(endpoint, response, 500, "documented 500 fault=" + operation),
                () -> checkResponse(endpoint, recovered, endpoint.success(), "recovery " + operation),
                () -> verifySuccessfulEffect(scenario));
    }

    private void verifySuccessfulEffect(MinistackFixtures.Scenario scenario) throws Exception {
        var endpoint = scenario.endpoint();
        if (endpoint.method() == Request.Method.GET) {
            return;
        }
        if (endpoint.method() == Request.Method.POST) {
            environment.awaitOutcome(scenario.iun());
            if (endpoint.id().equals("initializeInformalNotificationCost")) {
                assertFalse(fixtures.costExists(scenario), "Informal initialization must not create legal costs");
                var payment = fixtures.payment(scenario);
                assertNotNull(payment, "Informal payment persisted after outcome");
                assertEquals(scenario.iun(), payment.getIun());
                assertEquals("INFORMAL", payment.getCommunicationType().name());
                return;
            }
        }
        var get = contract.endpoints.stream().filter(e -> e.id().equals("getNotificationCost")).findFirst().orElseThrow();
        var response = exchange(get, fixtures.recipientPath(scenario), (JsonNode) null, "application/json", "*/*");
        assertAll(
                () -> checkResponse(get, response, 200, "read after " + endpoint.id()),
                () -> {
                    if (endpoint.method() == Request.Method.PUT) {
                        JsonNode body = Json.mapper().readTree(response.body());
                        assertEquals(200, response.statusCode(), response.body());
                        for (JsonNode phase : scenario.body().path("costPhases")) {
                            String component = phase.asText().endsWith("_0") ? "FIRST_ATTEMPT" : "SECOND_ATTEMPT";
                            var components = body.at("/totalCost/details/analogCostDetail/analogCostComponents");
                            assertTrue(components.isArray(), "Analog components must be exposed");
                            boolean found = false;
                            for (JsonNode cost : components) {
                                if (cost.path("costName").asText().equals(component)) {
                                    found = true;
                                    assertEquals(0, cost.path("cost").asInt(), "Invalidated " + component);
                                }
                            }
                            assertTrue(found, "Invalidated component missing: " + component);
                        }
                        for (JsonNode component : body.at("/totalCost/details/analogCostDetail/analogCostComponents")) {
                            boolean first = component.path("costName").asText().equals("FIRST_ATTEMPT");
                            String phase = "SEND_ANALOG_DOMICILE_ATTEMPT_" + (first ? "0" : "1");
                            boolean invalidated = false;
                            for (JsonNode requested : scenario.body().path("costPhases")) {
                                invalidated |= requested.asText().equals(phase);
                            }
                            assertEquals(invalidated ? 0 : first ? 100 : 50, component.path("cost").asInt(),
                                    "Unselected analog attempts must remain unchanged");
                        }
                    }
                });
    }

    private void assertNoWrites(MinistackFixtures.Scenario scenario, int from) {
        var writes = proxy.calls.stream().skip(from).filter(call ->
                        Set.of("SendMessage", "SendMessageBatch", "UpdateItem", "PutItem", "DeleteItem").stream()
                                .anyMatch(operation -> call.operation().endsWith("." + operation)))
                .filter(call -> call.body().contains(scenario.iun()) || call.body().contains(scenario.iuv())).toList();
        assertTrue(writes.isEmpty(), "Rejected request caused AWS writes: " + writes);
    }

    @TestFactory
    Stream<DynamicTest> requiredTypesEnumsPatternsAndBoundsIncludingNestedObjects() {
        return contract.endpoints.stream().flatMap(endpoint -> {
            var variants = contract.bodyVariants(endpoint);
            return IntStream.range(0, variants.size()).mapToObj(index -> DynamicTest.dynamicTest(
                    endpoint.id() + " " + variants.get(index).name(), () -> {
                        var original = fixtures.scenario(endpoint);
                        var variant = contract.bodyVariants(original.endpoint()).get(index);
                        ObjectNode fixture = original.endpoint().fixture().deepCopy();
                        fixture.set("body", variant.body());
                        var actual = new InternalApiContractCases.Endpoint(endpoint.template(), endpoint.method(),
                                endpoint.operation(), fixture);
                        var scenario = new MinistackFixtures.Scenario(actual, original.iun(), original.iuv());
                        var validation = contract.validator.validateRequest(actual.request(actual.path(), actual.body()));
                        assertEquals(!variant.valid(), validation.hasErrors(),
                                () -> "Generated variant must agree with OpenAPI: " + validation.getMessages());
                        if (actual.method() == Request.Method.PUT) {
                            fixtures.seed(scenario);
                        }
                        int before = proxy.calls.size();
                        var response = exchange(actual, actual.path(), actual.body(), "application/json", "*/*");
                        assertAll(
                                () -> checkResponse(actual, response, variant.valid() ? actual.success() : 400, variant.name()),
                                () -> {
                                    if (variant.valid()) {
                                        verifySuccessfulEffect(scenario);
                                    } else {
                                        assertNoWrites(scenario, before);
                                    }
                                });
                    }));
        });
    }

    @TestFactory
    Stream<DynamicTest> pathParametersRejectInvalidPatternsLengthsAndTypes() {
        return contract.endpoints.stream().flatMap(endpoint -> endpoint.operation().getParameters().stream()
                .flatMap(parameter -> Stream.of("!invalid!", "integer".equals(parameter.getSchema().getType()) ? "1.5"
                        : endpoint.fixture().path("path").get(parameter.getName()).asText().repeat(2)).map(invalid ->
                        DynamicTest.dynamicTest(endpoint.id() + " path " + parameter.getName() + "=" + invalid, () -> {
                            var scenario = fixtures.scenario(endpoint);
                            var actual = scenario.endpoint();
                            String path = actual.path().replace(actual.fixture().path("path")
                                    .get(parameter.getName()).asText(), invalid);
                            assertTrue(contract.validator.validateRequest(actual.request(path, actual.body())).hasErrors());
                            int before = proxy.calls.size();
                            var response = exchange(actual, path, actual.body(), "application/json", "*/*");
                            assertAll(() -> checkResponse(actual, response, 400, "invalid " + parameter.getName()),
                                    () -> assertNoWrites(scenario, before));
                        }))));
    }

    @TestFactory
    Stream<DynamicTest> recipientIndexInt32BoundariesAreAcceptedAndOverflowIsRejected() {
        var baseline = contract.endpoints.stream().filter(e -> e.id().equals("getNotificationCost"))
                .findFirst().orElseThrow();
        return Stream.of("-2147483648", "-1", "2147483647", "-2147483649", "2147483648").map(value ->
                DynamicTest.dynamicTest(baseline.id() + " recIndex=" + value, () -> {
                    var scenario = fixtures.scenario(baseline);
                    String path = "/notification-cost-private/cost/" + scenario.iun() + "/recipient/" + value;
                    boolean valid = Long.parseLong(value) >= Integer.MIN_VALUE && Long.parseLong(value) <= Integer.MAX_VALUE;
                    assertEquals(!valid, contract.validator.validateRequest(baseline.request(path, null)).hasErrors(),
                            "recIndex has int32 format but no non-negative minimum");
                    int before = proxy.calls.size();
                    var response = exchange(baseline, path, (JsonNode) null, "application/json", "*/*");
                    assertAll(() -> checkResponse(baseline, response, valid ? 404 : 400, "recIndex=" + value),
                            () -> assertNoWrites(scenario, before));
                }));
    }

    @TestFactory
    Stream<DynamicTest> missingMalformedBodiesAndUnsupportedMediaTypes() {
        return contract.endpoints.stream().flatMap(endpoint -> {
            var cases = new ArrayList<DynamicTest>();
            cases.add(DynamicTest.dynamicTest(endpoint.id() + " unsupported Accept", () -> {
                var scenario = fixtures.scenario(endpoint);
                int before = proxy.calls.size();
                var response = exchange(scenario.endpoint(), scenario.endpoint().path(), scenario.body(),
                        "application/json", "application/xml");
                assertAll(() -> checkResponse(scenario.endpoint(), response, 406, "unsupported Accept"),
                        () -> assertNoWrites(scenario, before));
            }));
            if (endpoint.body() != null) {
                for (String body : List.of("", "{\"broken\":")) {
                    cases.add(DynamicTest.dynamicTest(endpoint.id() + " body=" + body, () -> {
                        var scenario = fixtures.scenario(endpoint);
                        int before = proxy.calls.size();
                        var response = exchangeRaw(scenario.endpoint(), scenario.endpoint().path(), body,
                                "application/json", "*/*");
                        assertAll(() -> checkResponse(scenario.endpoint(), response, 400, "missing/malformed body"),
                                () -> assertNoWrites(scenario, before));
                    }));
                }
                cases.add(DynamicTest.dynamicTest(endpoint.id() + " unsupported Content-Type", () -> {
                    var scenario = fixtures.scenario(endpoint);
                    int before = proxy.calls.size();
                    var response = exchange(scenario.endpoint(), scenario.endpoint().path(), scenario.body(),
                            "application/xml", "*/*");
                    assertAll(() -> checkResponse(scenario.endpoint(), response, 415, "unsupported Content-Type"),
                            () -> assertNoWrites(scenario, before));
                }));
            }
            return cases.stream();
        });
    }

    @TestFactory
    Stream<DynamicTest> deletedCostsReturnNotFound() {
        return contract.endpoints.stream().filter(e -> e.method() == Request.Method.GET).map(endpoint ->
                DynamicTest.dynamicTest(endpoint.id() + " deleted cost 404", () -> {
                    var scenario = fixtures.scenario(endpoint);
                    fixtures.seed(scenario, 2, NotificationFeePolicy.DELIVERY_MODE, true, true);
                    checkResponse(scenario.endpoint(), exchange(scenario.endpoint(), scenario.endpoint().path(),
                            null, "application/json", "*/*"), 404, "deleted cost");
                }));
    }

    @TestFactory
    Stream<DynamicTest> validatorDetectsCorruptResponsesAndUndocumentedStatus() {
        return contract.endpoints.stream().filter(e -> e.method() == Request.Method.GET).map(endpoint ->
                DynamicTest.dynamicTest(endpoint.id() + " response mutation sensitivity", () -> {
                    var scenario = fixtures.scenario(endpoint);
                    fixtures.seed(scenario);
                    var response = exchange(scenario.endpoint(), scenario.endpoint().path(), null, "application/json", "*/*");
                    checkResponse(scenario.endpoint(), response, 200, "mutation baseline");
                    JsonNode body = Json.mapper().readTree(response.body());
                    assertFalse(contract.responseVariants(endpoint, body).isEmpty(), "No response constraints tested");
                    for (var variant : contract.responseVariants(endpoint, body)) {
                        assertEquals(!variant.valid(), contract.validator.validateResponse(scenario.endpoint().path(),
                                        endpoint.method(), SimpleResponse.Builder.ok().withContentType("application/json")
                                                .withBody(variant.body().toString()).build()).hasErrors(),
                                "Response validator sensitivity: " + variant.name());
                    }
                    assertTrue(contract.validator.validateResponse(scenario.endpoint().path(), endpoint.method(),
                            SimpleResponse.Builder.status(418).build()).hasErrors());
                }));
    }

    @TestFactory
    Stream<DynamicTest> totalsEqualTheDocumentedSumOfDetails() {
        return contract.endpoints.stream().filter(e -> e.method() == Request.Method.GET).flatMap(endpoint ->
                Stream.of(NotificationFeePolicy.values()).flatMap(policy ->
                        (endpoint.id().equals("getNotificationCostByPayment") ? Stream.of(true, false) : Stream.of(true))
                                .map(applyCost -> DynamicTest.dynamicTest(
                                        endpoint.id() + " sums " + policy + " applyCost=" + applyCost, () -> {
                                            var scenario = fixtures.scenario(endpoint);
                                            fixtures.seed(scenario, 2, policy, applyCost, false);
                                            var response = exchange(scenario.endpoint(), scenario.endpoint().path(), null,
                                                    "application/json", "*/*");
                                            JsonNode body = Json.mapper().readTree(response.body());
                                            assertAll(
                                                    () -> checkResponse(scenario.endpoint(), response, 200, "economic sums"),
                                                    () -> assertSum(body, "/totalCost", "costWithVat", "costWithVat"),
                                                    () -> {
                                                        if (endpoint.id().equals("getNotificationCostByPayment")) {
                                                            assertSum(body, "/partialCost", "cost", "cost");
                                                        }
                                                    });
                                        }))));
    }

    private void assertSum(JsonNode body, String pointer, String cost, String analogCost) {
        JsonNode details = body.at(pointer + "/details");
        assertEquals(details.at("/baseCostDetail/cost").intValue()
                        + details.at("/analogCostDetail/" + analogCost).intValue(), body.at(pointer + "/" + cost).intValue(),
                pointer + " must equal the sum of details described by OpenAPI");
    }

    private HttpResponse<String> exchange(InternalApiContractCases.Endpoint endpoint, String path,
                                          JsonNode body, String contentType, String accept) throws Exception {
        return exchangeRaw(endpoint, path, body == null ? null : body.toString(), contentType, accept);
    }

    private HttpResponse<String> exchangeRaw(InternalApiContractCases.Endpoint endpoint, String path,
                                          String body, String contentType, String accept) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(60)).header("Accept", accept);
        if (body != null) {
            request.header("Content-Type", contentType);
        }
        return http.send(request.method(endpoint.method().name(), body == null
                        ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private void checkResponse(InternalApiContractCases.Endpoint endpoint, HttpResponse<String> result,
                               int expected, String scenario) {
        var response = SimpleResponse.Builder.status(result.statusCode());
        result.headers().map().forEach(response::withHeader);
        if (!result.body().isEmpty()) {
            response.withBody(result.body());
        }
        var validation = contract.validator.validateResponse(result.uri().getRawPath(), endpoint.method(), response.build());
        List<String> violations = new ArrayList<>(validation.getMessages().stream().map(Object::toString).toList());
        var documented = endpoint.operation().getResponses().get(Integer.toString(result.statusCode()));
        if (documented != null && documented.getContent() != null && documented.getContent().size() == 1) {
            String declaredMedia = documented.getContent().keySet().iterator().next();
            if (!result.headers().firstValue("Content-Type").orElse("").startsWith(declaredMedia)) {
                // Keep the real media-type failure, but also check the body it would otherwise hide.
                var schemaOnly = contract.validator.validateResponse(result.uri().getRawPath(), endpoint.method(),
                        SimpleResponse.Builder.status(result.statusCode()).withContentType(declaredMedia)
                                .withBody(result.body()).build());
                schemaOnly.getMessages().forEach(message -> violations.add("Body schema: " + message));
                assertAll(
                        () -> recordAndAssertResponse(endpoint, result, expected, scenario, violations, validation.hasErrors()),
                        () -> assertFalse(schemaOnly.hasErrors(), schemaOnly.getMessages().toString()));
                return;
            }
        }
        recordAndAssertResponse(endpoint, result, expected, scenario, violations, validation.hasErrors());
    }

    private void recordAndAssertResponse(InternalApiContractCases.Endpoint endpoint, HttpResponse<String> result,
                                         int expected, String scenario, List<String> violations, boolean invalid) {
        if (expected != result.statusCode()) {
            violations.add("Expected HTTP " + expected + " but received " + result.statusCode());
        }
        observations.add(new Observation(endpoint.id(), scenario, result.uri().getRawPath(), expected, result.statusCode(),
                violations));
        assertAll(
                () -> assertEquals(expected, result.statusCode(), result.body()),
                () -> assertFalse(invalid, violations.toString()),
                () -> {
                    if (result.statusCode() == 204) {
                        assertEquals("", result.body(), "204 must not contain a body");
                    } else if (result.statusCode() >= 400) {
                        assertEquals(result.statusCode(), Json.mapper().readTree(result.body()).path("status").asInt());
                        assertTrue(result.headers().firstValue("Content-Type").orElse("")
                                .startsWith("application/problem+json"), "Errors must use application/problem+json");
                    } else if (expected == 202) {
                        assertEquals("OK", result.body());
                    }
                });
    }

    @AfterAll
    void shutdown() throws Exception {
        Files.createDirectories(Path.of("target/api-contract"));
        Files.writeString(Path.of("target/api-contract/ministack.log"), MINISTACK.getLogs());
        Files.writeString(Path.of("target/api-contract/observations.json"),
                Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(observations));
        var coverage = contract.endpoints.stream().flatMap(endpoint -> endpoint.operation().getResponses().keySet()
                .stream().map(status -> {
                    var attempted = observations.stream().filter(o -> o.operation().equals(endpoint.id())
                            && o.expected() == Integer.parseInt(status) && o.scenario().startsWith("documented ")).toList();
                    return new Coverage(endpoint.id(), status, !attempted.isEmpty(),
                            attempted.stream().anyMatch(o -> o.actual() == o.expected()),
                            attempted.stream().anyMatch(o -> o.actual() == o.expected() && o.violations().isEmpty()));
                })).toList();
        Files.writeString(Path.of("target/api-contract/coverage.json"),
                Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(coverage));
        http.close();
    }
}
