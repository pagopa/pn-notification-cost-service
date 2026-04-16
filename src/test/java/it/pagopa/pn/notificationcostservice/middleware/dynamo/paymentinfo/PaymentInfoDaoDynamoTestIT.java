package it.pagopa.pn.notificationcostservice.middleware.dynamo.paymentinfo;

import it.pagopa.pn.notificationcostservice.LocalStackTestConfig;
import it.pagopa.pn.notificationcostservice.MockAWSObjectsTest;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.PaymentInfoDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.List;

@SpringBootTest
@Import({LocalStackTestConfig.class, MockAWSObjectsTest.class})
@Disabled
class PaymentInfoDaoDynamoTestIT {

    @Autowired
    private PaymentInfoDaoDynamo dao;

    @Autowired
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Value("${pn.notification-cost-service.payment-info-table.table-name}")
    private String tableName;

    private DynamoDbAsyncTable<PaymentInfoEntity> paymentInfoTable;

    @BeforeEach
    void setup() {
        paymentInfoTable = dynamoDbEnhancedAsyncClient.table(
                tableName,
                TableSchema.fromBean(PaymentInfoEntity.class)
        );
    }

    @Test
    void updateItemIfNotExistsOrMatch_shouldPersistSinglePayment() throws Exception {
        String iuv = "IUV-SINGLE-" + System.nanoTime();
        PaymentInfo payment = newPaymentInfo(iuv, "IUN-SINGLE", 0, true);

        try {
            StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(payment)))
                    .verifyComplete();

            PaymentInfoEntity persisted = getItem(iuv);

            Assertions.assertNotNull(persisted);
            Assertions.assertEquals(payment.getIuv(), persisted.getIuv());
            Assertions.assertEquals(payment.getIun(), persisted.getIun());
            Assertions.assertEquals(payment.getRecIndex(), persisted.getRecIndex());
            Assertions.assertEquals(payment.isApplyCost(), persisted.getApplyCost());
        } finally {
            deleteItem(iuv);
        }
    }

    @Test
    void updateItemIfNotExistsOrMatch_shouldSucceedWhenExistingItemMatchesCondition() throws Exception {
        String iuv = "IUV-UPDATE-" + System.nanoTime();

        PaymentInfoEntity existing = newPaymentInfoEntity(iuv);
        PaymentInfo updated = newPaymentInfo(iuv, "IUN-OLD", 1, true);

        try {
            putItem(existing);

            StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(updated)))
                    .verifyComplete();

            PaymentInfoEntity persisted = getItem(iuv);

            Assertions.assertNotNull(persisted);
            Assertions.assertEquals(updated.getIuv(), persisted.getIuv());
            Assertions.assertEquals(updated.getIun(), persisted.getIun());
            Assertions.assertEquals(updated.getRecIndex(), persisted.getRecIndex());
            Assertions.assertEquals(updated.isApplyCost(), persisted.getApplyCost());
        } finally {
            deleteItem(iuv);
        }
    }

    @Test
    void updateItemIfNotExistsOrMatch_shouldPersistMultiplePayments() throws Exception {
        String iuv1 = "IUV-MULTI-1-" + System.nanoTime();
        String iuv2 = "IUV-MULTI-2-" + System.nanoTime();

        PaymentInfo payment1 = newPaymentInfo(iuv1, "IUN-MULTI-1", 0, true);
        PaymentInfo payment2 = newPaymentInfo(iuv2, "IUN-MULTI-2", 1, false);

        try {
            StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(payment1, payment2)))
                    .verifyComplete();

            PaymentInfoEntity persisted1 = getItem(iuv1);
            PaymentInfoEntity persisted2 = getItem(iuv2);

            Assertions.assertNotNull(persisted1);
            Assertions.assertNotNull(persisted2);

            Assertions.assertEquals(payment1.getIun(), persisted1.getIun());
            Assertions.assertEquals(payment1.getRecIndex(), persisted1.getRecIndex());
            Assertions.assertEquals(payment1.isApplyCost(), persisted1.getApplyCost());

            Assertions.assertEquals(payment2.getIun(), persisted2.getIun());
            Assertions.assertEquals(payment2.getRecIndex(), persisted2.getRecIndex());
            Assertions.assertEquals(payment2.isApplyCost(), persisted2.getApplyCost());
        } finally {
            deleteItem(iuv1);
            deleteItem(iuv2);
        }
    }

    private void putItem(PaymentInfoEntity entity) throws Exception {
        PutItemEnhancedRequest<PaymentInfoEntity> request =
                PutItemEnhancedRequest.builder(PaymentInfoEntity.class)
                        .item(entity)
                        .build();

        paymentInfoTable.putItem(request).get();
    }

    private PaymentInfoEntity getItem(String iuv) throws Exception {
        GetItemEnhancedRequest request = GetItemEnhancedRequest.builder()
                .key(Key.builder().partitionValue(iuv).build())
                .build();

        return paymentInfoTable.getItem(request).get();
    }

    private void deleteItem(String iuv) throws Exception {
        DeleteItemEnhancedRequest request = DeleteItemEnhancedRequest.builder()
                .key(Key.builder().partitionValue(iuv).build())
                .build();

        paymentInfoTable.deleteItem(request).get();
    }

    private static PaymentInfo newPaymentInfo(String iuv, String iun, Integer recIndex, boolean applyCost) {
        return PaymentInfo.builder()
                .iuv(iuv)
                .iun(iun)
                .recIndex(recIndex)
                .applyCost(applyCost)
                .build();
    }

    private static PaymentInfoEntity newPaymentInfoEntity(String iuv) {
        return PaymentInfoEntity.builder()
                .iuv(iuv)
                .iun("IUN-OLD")
                .recIndex(1)
                .applyCost(true)
                .build();
    }
}
