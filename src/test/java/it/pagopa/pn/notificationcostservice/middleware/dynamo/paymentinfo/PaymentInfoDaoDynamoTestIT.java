package it.pagopa.pn.notificationcostservice.middleware.dynamo.paymentinfo;

import it.pagopa.pn.notificationcostservice.LocalStackTestConfig;
import it.pagopa.pn.notificationcostservice.MockAWSObjectsTest;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.PaymentInfoDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.model.CommunicationType;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.notificationcostservice.model.CommunicationType.INFORMAL;
import static it.pagopa.pn.notificationcostservice.model.CommunicationType.LEGAL;

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
        PaymentInfo payment = PaymentInfo.builder()
                .iuv(iuv)
                .iun("IUN-SINGLE")
                .recIndex(0)
                .applyCost(true)
                .communicationType(LEGAL)
                .build();

        try {
            StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(payment)))
                    .verifyComplete();

            PaymentInfoEntity persisted = getItem(iuv);

            Assertions.assertNotNull(persisted);
            Assertions.assertEquals(payment.getIuv(), persisted.getIuv());
            Assertions.assertEquals(payment.getIun(), persisted.getIun());
            Assertions.assertEquals(payment.getRecIndex(), persisted.getRecIndex());
            Assertions.assertEquals(payment.getApplyCost(), persisted.getApplyCost());
            Assertions.assertEquals(payment.getCommunicationType(), persisted.getCommunicationType());
        } finally {
            deleteItem(iuv);
        }
    }

    @Test
    void updateItemIfNotExistsOrMatch_shouldSucceedWhenExistingItemMatchesCondition() throws Exception {
        String iuv = "IUV-UPDATE-" + System.nanoTime();

        PaymentInfoEntity existing = PaymentInfoEntity.builder()
                .iuv(iuv)
                .iun("IUN-OLD")
                .recIndex(1)
                .applyCost(true)
                .build();
        PaymentInfo updated = PaymentInfo.builder()
                .iuv(iuv)
                .iun("IUN-OLD")
                .recIndex(1)
                .applyCost(true)
                .communicationType(LEGAL)
                .build();

        try {
            putItem(existing);

            StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(updated)))
                    .verifyComplete();

            PaymentInfoEntity persisted = getItem(iuv);

            Assertions.assertNotNull(persisted);
            Assertions.assertEquals(updated.getIuv(), persisted.getIuv());
            Assertions.assertEquals(updated.getIun(), persisted.getIun());
            Assertions.assertEquals(updated.getRecIndex(), persisted.getRecIndex());
            Assertions.assertEquals(updated.getApplyCost(), persisted.getApplyCost());
            Assertions.assertEquals(updated.getCommunicationType(), persisted.getCommunicationType());
        } finally {
            deleteItem(iuv);
        }
    }

    @Test
    void updateItemIfNotExistsOrMatch_shouldPersistMultiplePayments() throws Exception {
        String iuv1 = "IUV-MULTI-1-" + System.nanoTime();
        String iuv2 = "IUV-MULTI-2-" + System.nanoTime();

        PaymentInfo payment1 = PaymentInfo.builder()
                .iuv(iuv1)
                .iun("IUN-MULTI-1")
                .recIndex(0)
                .applyCost(true)
                .communicationType(LEGAL)
                .build();
        PaymentInfo payment2 = PaymentInfo.builder()
                .iuv(iuv2)
                .iun("IUN-MULTI-2")
                .recIndex(1)
                .applyCost(false)
                .communicationType(LEGAL)
                .build();

        try {
            StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(payment1, payment2)))
                    .verifyComplete();

            PaymentInfoEntity persisted1 = getItem(iuv1);
            PaymentInfoEntity persisted2 = getItem(iuv2);

            Assertions.assertNotNull(persisted1);
            Assertions.assertNotNull(persisted2);

            Assertions.assertEquals(payment1.getIun(), persisted1.getIun());
            Assertions.assertEquals(payment1.getRecIndex(), persisted1.getRecIndex());
            Assertions.assertEquals(payment1.getApplyCost(), persisted1.getApplyCost());
            Assertions.assertEquals(payment1.getCommunicationType(), persisted1.getCommunicationType());

            Assertions.assertEquals(payment2.getIun(), persisted2.getIun());
            Assertions.assertEquals(payment2.getRecIndex(), persisted2.getRecIndex());
            Assertions.assertEquals(payment2.getApplyCost(), persisted2.getApplyCost());
            Assertions.assertEquals(payment2.getCommunicationType(), persisted2.getCommunicationType());
        } finally {
            deleteItem(iuv1);
            deleteItem(iuv2);
        }
    }

    @Test
    void updateItemIfNotExistsOrMatch_shouldPersistPaymentWithoutApplyCost() throws Exception {
        String iuv1 = "IUV-MULTI-1-" + System.nanoTime();

        PaymentInfo payment1 = PaymentInfo.builder()
                .iuv(iuv1)
                .iun("IUN-MULTI-1")
                .recIndex(0)
                .applyCost(null)
                .communicationType(LEGAL)
                .build();

        try {
            StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(payment1)))
                    .verifyComplete();

            PaymentInfoEntity persisted1 = getItem(iuv1);

            Assertions.assertNotNull(persisted1);

            Assertions.assertEquals(payment1.getIun(), persisted1.getIun());
            Assertions.assertEquals(payment1.getRecIndex(), persisted1.getRecIndex());
            Assertions.assertNull(persisted1.getApplyCost());
            Assertions.assertEquals(payment1.getCommunicationType(), persisted1.getCommunicationType());
        } finally {
            deleteItem(iuv1);
        }
    }

    /**
     * Test per verificare la gestione retrocompatibile di aggiornamenti di record già presenti in tabella
     * che non hanno il campo communicationType valorizzato (perché creati con una versione precedente del servizio).
     * Quando il campo non è presente, il record deve essere trattato come LEGAL, dunque permettere aggiornamenti solo se il nuovo record ha communicationType LEGAL.
     */
    @Nested
    class UpdateItemTreatMissingAsTests {
        static Stream<Arguments> compatibleCommunicationTypes() {
            return Stream.of(
                    Arguments.of("legacy senza attributo, richiesto LEGAL", null, LEGAL),
                    Arguments.of("LEGAL su LEGAL", LEGAL, LEGAL),
                    Arguments.of("INFORMAL su INFORMAL", INFORMAL, INFORMAL));
        }

        static Stream<Arguments> incompatibleCommunicationTypes() {
            return Stream.of(
                    Arguments.of("legacy senza attributo, richiesto INFORMAL", null, INFORMAL),
                    Arguments.of("LEGAL esistente, richiesto INFORMAL", LEGAL, INFORMAL),
                    Arguments.of("INFORMAL esistente, richiesto LEGAL", INFORMAL, LEGAL));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("compatibleCommunicationTypes")
        void updateItemIfNotExistsOrMatch_shouldSucceed_whenCommunicationTypeIsCompatible(
                String description, CommunicationType existing, CommunicationType requested) throws Exception {
            String iuv = "IUV-UPDATE-" + System.nanoTime();
            PaymentInfoEntity existingItem = givenExistingItem(iuv, existing);

            try {
                putItem(existingItem);
                StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(newPaymentInfo(iuv, requested))))
                        .verifyComplete();

                PaymentInfoEntity persisted = getItem(iuv);
                Assertions.assertNotNull(persisted);
                Assertions.assertEquals(existing, persisted.getCommunicationType());
            } finally {
                deleteItem(iuv);
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("incompatibleCommunicationTypes")
        void updateItemIfNotExistsOrMatch_shouldFail_whenCommunicationTypeIsIncompatible(
                String description, CommunicationType existing, CommunicationType requested) throws Exception {
            String iuv = "IUV-UPDATE-" + System.nanoTime();
            PaymentInfoEntity existingItem = givenExistingItem(iuv, existing);

            putItem(existingItem);
            StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(newPaymentInfo(iuv, requested))))
                .verifyError(ConditionalCheckFailedException.class);

            // Verifica che il record esistente non sia stato modificato
            PaymentInfoEntity persisted = getItem(iuv);

            Assertions.assertNotNull(persisted);
            Assertions.assertEquals(existingItem.getIun(), persisted.getIun());
            Assertions.assertEquals(existingItem.getRecIndex(), persisted.getRecIndex());
            Assertions.assertEquals(existingItem.getApplyCost(), persisted.getApplyCost());
            Assertions.assertEquals(existingItem.getCommunicationType(), persisted.getCommunicationType());
        }

        /** Metodo helper per creare un record esistente con un certo communicationType e persisterlo */
        private PaymentInfoEntity givenExistingItem(String iuv, CommunicationType communicationType) {
            return PaymentInfoEntity.builder()
                    .iuv(iuv)
                    .iun("IUN_OLD")
                    .recIndex(0)
                    .applyCost(true)
                    .communicationType(communicationType)
                    .build();
        }

        private PaymentInfo newPaymentInfo(String iuv, CommunicationType requested) {
            return PaymentInfo.builder()
                    .iuv(iuv)
                    .iun("IUN_OLD")
                    .recIndex(0)
                    .applyCost(true)
                    .communicationType(requested)
                    .build();
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
}
