package it.pagopa.pn.notificationcostservice.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.model.CommunicationType;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

final class MinistackFixtures {
    record Scenario(InternalApiContractCases.Endpoint endpoint, String iun, String iuv) {
        JsonNode body() {
            return endpoint.body();
        }
    }

    private final AtomicInteger sequence = new AtomicInteger();
    private final DynamoDbTable<NotificationDeliveryCostEntity> costs;
    private final DynamoDbTable<PaymentInfoEntity> payments;

    MinistackFixtures(MinistackApiEnvironment environment) {
        var enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(environment.dynamo).build();
        costs = enhanced.table(MinistackApiEnvironment.COSTS, TableSchema.fromBean(NotificationDeliveryCostEntity.class));
        payments = enhanced.table(MinistackApiEnvironment.PAYMENTS, TableSchema.fromBean(PaymentInfoEntity.class));
    }

    Scenario scenario(InternalApiContractCases.Endpoint baseline) {
        int id = sequence.incrementAndGet();
        boolean informal = baseline.id().equals("initializeInformalNotificationCost");
        String iun = "ABCD-EFGH-ILMN-%06d-A-%s".formatted(id, informal ? "A" : "1");
        String noticeCode = "%018d".formatted(id);
        String iuv = "77777777777##" + noticeCode;
        ObjectNode fixture = baseline.fixture().deepCopy();
        ObjectNode path = (ObjectNode) fixture.get("path");
        if (path.has("iun")) {
            path.put("iun", iun);
        }
        if (path.has("noticeCode")) {
            path.put("noticeCode", noticeCode);
        }
        if (fixture.has("body")) {
            fixture.get("body").path("costRecipients").forEach(recipient ->
                    recipient.path("payments").forEach(payment -> ((ObjectNode) payment).put("iuv", iuv)));
        }
        return new Scenario(new InternalApiContractCases.Endpoint(
                baseline.template(), baseline.method(), baseline.operation(), fixture), iun, iuv);
    }

    void seed(Scenario scenario, int attempts, NotificationFeePolicy policy, boolean applyCost, boolean deleted) {
        int recIndex = 0;
        if (scenario.body() != null && scenario.body().path("recIndex").isTextual()
                && scenario.body().path("recIndex").asText().matches("RECINDEX_\\d+")) {
            recIndex = Integer.parseInt(scenario.body().get("recIndex").asText().substring("RECINDEX_".length()));
        }
        costs.putItem(NotificationDeliveryCostEntity.builder().iun(scenario.iun()).recIndex(recIndex)
                .recipientInternalId("contract-test-recipient").senderPaId("contract-test-sender")
                .senderTaxId("77777777777").baseCost(BaseCostEntity.builder().sendFee(100).paFee(50).build())
                .vat(22).pagoPaIntMode(PagoPaIntMode.SYNC).notificationFeePolicy(policy).isDeleted(deleted)
                .lastUpdate(Instant.parse("2026-01-01T00:00:00Z"))
                .firstAnalogCost(attempts >= 1 ? FirstAnalogCostEntity.builder().cost(100).productType("AR").build() : null)
                .secondAnalogCost(attempts >= 2 ? SecondAnalogCostEntity.builder().cost(50).productType("AR").build() : null)
                .build());
        payments.putItem(PaymentInfoEntity.builder().iuv(scenario.iuv()).iun(scenario.iun()).recIndex(recIndex)
                .communicationType(CommunicationType.LEGAL).applyCost(applyCost).build());
    }

    void seed(Scenario scenario) {
        seed(scenario, 2, NotificationFeePolicy.DELIVERY_MODE, true, false);
    }

    boolean costExists(Scenario scenario) {
        return costs.query(r -> r.queryConditional(QueryConditional.keyEqualTo(
                Key.builder().partitionValue(scenario.iun()).build())).consistentRead(true)).items().iterator().hasNext();
    }

    PaymentInfoEntity payment(Scenario scenario) {
        return payments.getItem(r -> r.key(Key.builder().partitionValue(scenario.iuv()).build()).consistentRead(true));
    }

    String recipientPath(Scenario scenario) {
        String recIndex = scenario.body() != null
                ? scenario.body().path("recIndex").asText(
                        scenario.body().at("/costRecipients/0/recIndex").asText("0")).replace("RECINDEX_", "") : "0";
        return "/notification-cost-private/cost/" + scenario.iun() + "/recipient/" + recIndex;
    }
}
