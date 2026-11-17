package it.pagopa.pn.notificationcostservice.rest;

import io.swagger.v3.core.util.Json;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.Target;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

final class MinistackApiEnvironment implements AutoCloseable {
    // Multi-architecture OCI index for MiniStack 1.5.41 (linux/amd64 and linux/arm64).
    static final String IMAGE = "docker.io/ministackorg/ministack@sha256:aef90fec1a4581c5fa7b4f79d347898f8475111bac95bbab3736f4a44f5f8573";
    static final String COSTS = "contract-costs";
    static final String PAYMENTS = "contract-payments";
    static final String INITIALIZATION = "contract-initialization";
    static final String VERIFY_PAYMENTS = "contract-verify-payments";
    static final String BUS = "contract-outcomes";
    static final String SOURCE = "pn-notification-cost-service";
    static final String DETAIL_TYPE = "NotificationCostServiceOutcomeEvent";

    static GenericContainer<?> container() {
        return new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withExposedPorts(4566)
                .waitingFor(Wait.forHttp("/_ministack/health").forStatusCode(200))
                .withStartupTimeout(Duration.ofMinutes(2));
    }

    final URI endpoint;
    final DynamoDbClient dynamo;
    final SqsClient sqs;
    final EventBridgeClient events;
    final String outcomes;
    private final Map<String, String> receivedOutcomes = new ConcurrentHashMap<>();

    MinistackApiEnvironment(GenericContainer<?> container) {
        endpoint = URI.create("http://" + container.getHost() + ":" + container.getMappedPort(4566));
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
        var limits = ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofSeconds(15))
                .apiCallAttemptTimeout(Duration.ofSeconds(5)).build();
        dynamo = DynamoDbClient.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .credentialsProvider(credentials).overrideConfiguration(limits).build();
        sqs = SqsClient.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .credentialsProvider(credentials).overrideConfiguration(limits).build();
        events = EventBridgeClient.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .credentialsProvider(credentials).overrideConfiguration(limits).build();
        createTables();
        sqs.createQueue(r -> r.queueName(INITIALIZATION));
        sqs.createQueue(r -> r.queueName(VERIFY_PAYMENTS));
        outcomes = sqs.createQueue(r -> r.queueName("contract-outcome-capture")).queueUrl();
        String queueArn = sqs.getQueueAttributes(r -> r.queueUrl(outcomes)
                .attributeNames(QueueAttributeName.QUEUE_ARN)).attributes().get(QueueAttributeName.QUEUE_ARN);
        String busArn = events.createEventBus(r -> r.name(BUS)).eventBusArn();
        String ruleArn = events.putRule(r -> r.name("capture").eventBusName(BUS)
                .eventPattern("{\"source\":[\"" + SOURCE + "\"],\"detail-type\":[\"" + DETAIL_TYPE + "\"]}"))
                .ruleArn();
        assertNotNull(busArn, "MiniStack CreateEventBus");
        sqs.setQueueAttributes(r -> r.queueUrl(outcomes).attributes(Map.of(QueueAttributeName.POLICY,
                "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                        + "\"Principal\":{\"Service\":\"events.amazonaws.com\"},\"Action\":\"sqs:SendMessage\","
                        + "\"Resource\":\"" + queueArn + "\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\""
                        + ruleArn + "\"}}}]}")));
        assertEquals(0, events.putTargets(r -> r.rule("capture").eventBusName(BUS)
                .targets(Target.builder().id("capture").arn(queueArn).inputPath("$.detail").build()))
                .failedEntryCount(), "MiniStack EventBridge target");
    }

    private void createTables() {
        dynamo.createTable(r -> r.tableName(COSTS).billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(attribute("pk", ScalarAttributeType.S), attribute("sk", ScalarAttributeType.N))
                .keySchema(key("pk", KeyType.HASH), key("sk", KeyType.RANGE)));
        dynamo.createTable(r -> r.tableName(PAYMENTS).billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(attribute("pk", ScalarAttributeType.S), attribute("iun", ScalarAttributeType.S))
                .keySchema(key("pk", KeyType.HASH))
                .globalSecondaryIndexes(GlobalSecondaryIndex.builder().indexName("iun_gsi")
                        .keySchema(key("iun", KeyType.HASH))
                        .projection(p -> p.projectionType(ProjectionType.ALL)).build()));
        for (String table : new String[]{COSTS, PAYMENTS}) {
            try (var waiter = dynamo.waiter()) {
                var ready = waiter.waitUntilTableExists(r -> r.tableName(table));
                assertTrue(ready.matched().response().isPresent(), "MiniStack table readiness: " + table);
            }
        }
    }

    void verifyCompatibility() throws Exception {
        Map<String, AttributeValue> key = Map.of("pk", text("compatibility"), "sk", number(0));
        var update = UpdateItemRequest.builder().tableName(COSTS).key(key)
                .updateExpression("SET #b = :b")
                .conditionExpression("(attribute_not_exists(#pk) AND attribute_not_exists(#sk)) OR (#b.#f = :f)")
                .expressionAttributeNames(Map.of("#b", "baseCost", "#f", "paFee", "#pk", "pk", "#sk", "sk"))
                .expressionAttributeValues(Map.of(":b", AttributeValue.builder().m(Map.of("paFee", number(50))).build(),
                        ":f", number(50))).returnValues(ReturnValue.ALL_NEW).build();
        assertEquals("50", dynamo.updateItem(update).attributes().get("baseCost").m().get("paFee").n());
        dynamo.updateItem(update);
        assertThrows(ConditionalCheckFailedException.class, () -> dynamo.updateItem(update.toBuilder()
                .expressionAttributeValues(Map.of(":b", AttributeValue.builder().m(Map.of("paFee", number(51))).build(),
                        ":f", number(51))).build()), "MiniStack must enforce nested conditional expressions");
        assertFalse(dynamo.getItem(r -> r.tableName(COSTS).key(key).consistentRead(true)).item().isEmpty());
        assertEquals(1, dynamo.query(r -> r.tableName(COSTS).keyConditionExpression("pk = :pk")
                .expressionAttributeValues(Map.of(":pk", text("compatibility")))).count());
        dynamo.putItem(r -> r.tableName(PAYMENTS).item(Map.of("pk", text("compatibility"), "iun", text("compatibility"))));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(1, dynamo.query(r ->
                r.tableName(PAYMENTS).indexName("iun_gsi").keyConditionExpression("iun = :iun")
                        .expressionAttributeValues(Map.of(":iun", text("compatibility")))).count()));
        sqs.sendMessage(r -> r.queueUrl(outcomes).messageBody("{\"compatibility\":true}"));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var messages = sqs.receiveMessage(r -> r.queueUrl(outcomes).waitTimeSeconds(1)).messages();
            assertFalse(messages.isEmpty(), "MiniStack SQS send/receive");
            messages.forEach(message -> sqs.deleteMessage(r -> r.queueUrl(outcomes).receiptHandle(message.receiptHandle())));
        });
        assertEquals(0, events.putEvents(r -> r.entries(PutEventsRequestEntry.builder()
                .eventBusName(BUS).source(SOURCE).detailType(DETAIL_TYPE)
                .detail("{\"iun\":\"compatibility\",\"status\":\"OK\"}").build())).failedEntryCount());
        awaitOutcome("compatibility");
        dynamo.deleteItem(r -> r.tableName(COSTS).key(key));
        dynamo.deleteItem(r -> r.tableName(PAYMENTS).key(Map.of("pk", text("compatibility"))));
    }

    String awaitOutcome(String iun) {
        await("EventBridge outcome for " + iun).atMost(Duration.ofSeconds(30)).until(() -> {
            for (var message : sqs.receiveMessage(r -> r.queueUrl(outcomes).waitTimeSeconds(1)
                    .maxNumberOfMessages(10)).messages()) {
                var payload = Json.mapper().readTree(message.body());
                if (payload.has("pnNotificationCostValidationPayload")) {
                    payload = payload.get("pnNotificationCostValidationPayload");
                }
                assertTrue(payload.hasNonNull("iun"), "Unexpected outcome: " + message.body());
                receivedOutcomes.put(payload.get("iun").asText(), payload.path("status").asText());
                sqs.deleteMessage(r -> r.queueUrl(outcomes).receiptHandle(message.receiptHandle()));
            }
            return receivedOutcomes.containsKey(iun);
        });
        assertEquals("OK", receivedOutcomes.get(iun), "Initialization outcome for " + iun);
        return receivedOutcomes.get(iun);
    }

    static AttributeValue text(String value) {
        return AttributeValue.builder().s(value).build();
    }

    static AttributeValue number(int value) {
        return AttributeValue.builder().n(Integer.toString(value)).build();
    }

    private static AttributeDefinition attribute(String name, ScalarAttributeType type) {
        return AttributeDefinition.builder().attributeName(name).attributeType(type).build();
    }

    private static KeySchemaElement key(String name, KeyType type) {
        return KeySchemaElement.builder().attributeName(name).keyType(type).build();
    }

    @Override
    public void close() {
        events.close();
        sqs.close();
        dynamo.close();
    }
}
