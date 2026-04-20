package it.pagopa.pn.notificationcostservice.middleware.dynamo.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.LocalStackTestConfig;
import it.pagopa.pn.notificationcostservice.MockAWSObjectsTest;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.NotificationDeliveryCostDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dynamo.TestDao;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Import({LocalStackTestConfig.class, MockAWSObjectsTest.class})
@Disabled
public class NotificationDeliveryCostDaoDynamoTestIT {
    @Autowired
    private NotificationDeliveryCostDaoDynamo dao;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Value("${pn.notification-cost-service.notification-delivery-cost-dao.table-name}")
    String table;

    TestDao testDao;

    @BeforeEach
    void setup() {
        testDao = new TestDao(dynamoDbEnhancedAsyncClient, table);
    }

    @Test
    void getNotificationDeliveryCostItem() {
        String iun = "iun";
        Integer recIndex = 0;
        NotificationDeliveryCostEntity notificationDeliveryCostEntity = newNotificationDeliveryCostEntity(iun, recIndex);
        try {
            testDao.putItem(notificationDeliveryCostEntity);
            NotificationDeliveryCost elementFromDb = dao.getNotificationDeliveryCostItem(iun, recIndex).block();

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals(notificationDeliveryCostEntity.getIun(), elementFromDb.getIun());
            Assertions.assertEquals(notificationDeliveryCostEntity.getRecIndex(), elementFromDb.getRecIndex());
            Assertions.assertEquals(notificationDeliveryCostEntity.getBaseCost().getPaFee(), elementFromDb.getBaseCost().getPaFee());
            Assertions.assertEquals(notificationDeliveryCostEntity.getBaseCost().getSendFee(), elementFromDb.getBaseCost().getSendFee());


            // Verifica FirstAnalogCost
            Assertions.assertNotNull(elementFromDb.getFirstAnalogCost());
            Assertions.assertEquals(
                    notificationDeliveryCostEntity.getFirstAnalogCost().getCost(),
                    elementFromDb.getFirstAnalogCost().getCost()
            );
            Assertions.assertEquals(
                    notificationDeliveryCostEntity.getFirstAnalogCost().getProductType(),
                    elementFromDb.getFirstAnalogCost().getProductType()
            );

            // Verifica SecondAnalogCost
            Assertions.assertNotNull(elementFromDb.getSecondAnalogCost());
            Assertions.assertEquals(
                    notificationDeliveryCostEntity.getSecondAnalogCost().getCost(),
                    elementFromDb.getSecondAnalogCost().getCost()
            );
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(notificationDeliveryCostEntity.getIun(), notificationDeliveryCostEntity.getRecIndex());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    public static NotificationDeliveryCostEntity newNotificationDeliveryCostEntity(String iun, Integer recIndex) {
        return NotificationDeliveryCostEntity.builder()
                .iun(iun)
                .recIndex(recIndex)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .baseCost(newBaseCost())
                .vat(0)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .isDeleted(false)
                .lastUpdate(Instant.now())
                .firstAnalogCost(FirstAnalogCostEntity.builder()
                        .cost(50)
                        .productType("AR")
                        .build())
                .secondAnalogCost(SecondAnalogCostEntity.builder()
                        .cost(30)
                        .productType("890")
                        .build())
                .simpleRegisteredLetterCost(null)
                .recipientInternalId("recipientInternalId")
                .senderPaId("senderInternalId")
                .senderTaxId("senderTaxId")
                .build();
    }

    private static BaseCostEntity newBaseCost() {
        return BaseCostEntity.builder()
                .paFee(2)
                .sendFee(10)
                .build();
    }


    @Test
    void getNotificationDeliveryCostItemWhenItemNotFound() {
        StepVerifier.create(dao.getNotificationDeliveryCostItem("iun", 0))
                .expectError(PnNotFoundException.class)
                .verify();
    }
    @Test
    void updateBaseCostIfNotExistsOrMatch_whenExistingItemDoesNotMatch_returnsError() {
        String iun = "iun-update-mismatch-" + System.nanoTime();
        int recIndex = 2;

        NotificationDeliveryCostEntity original = newNotificationDeliveryCostEntity(iun, recIndex);

        NotificationDeliveryCostEntity updated = NotificationDeliveryCostEntity.builder()
                .iun(iun)
                .recIndex(recIndex)
                .recipientInternalId("recipient-updated")
                .senderPaId("senderInternalId")
                .senderTaxId("senderTaxId")
                .baseCost(BaseCostEntity.builder()
                        .paFee(5)
                        .sendFee(15)
                        .build())
                .vat(0)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .build();

        try {
            StepVerifier.create(dao.updateBaseCostIfNotExistsOrMatch(original))
                    .expectNextCount(1)
                    .verifyComplete();

            StepVerifier.create(dao.updateBaseCostIfNotExistsOrMatch(updated))
                    .expectErrorMatches(Objects::nonNull)
                    .verify();

            NotificationDeliveryCost elementFromDb = dao.getNotificationDeliveryCostItem(iun, recIndex).block();

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals(original.getRecipientInternalId(), elementFromDb.getRecipientInternalId());
            Assertions.assertEquals(original.getBaseCost().getPaFee(), elementFromDb.getBaseCost().getPaFee());
            Assertions.assertEquals(original.getBaseCost().getSendFee(), elementFromDb.getBaseCost().getSendFee());
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(iun, recIndex);
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void updateBaseCostIfNotExistsOrMatch_whenExistingItemMatchesCondition_returnsOk() {
        String iun = "iun-update-ok-" + System.nanoTime();
        int recIndex = 3;

        NotificationDeliveryCostEntity original = newNotificationDeliveryCostEntity(iun, recIndex);

        try {
            // primo inserimento
            StepVerifier.create(dao.updateBaseCostIfNotExistsOrMatch(original))
                    .assertNext(saved -> {
                        Assertions.assertEquals(original.getIun(), saved.getIun());
                        Assertions.assertEquals(original.getRecIndex(), saved.getRecIndex());
                        Assertions.assertEquals(original.getVat(), saved.getVat());
                        Assertions.assertEquals(original.getNotificationFeePolicy(), saved.getNotificationFeePolicy());
                        Assertions.assertEquals(original.getPagoPaIntMode(), saved.getPagoPaIntMode());
                        Assertions.assertEquals(original.getSenderPaId(), saved.getSenderPaId());
                        Assertions.assertEquals(original.getSenderTaxId(), saved.getSenderTaxId());
                        Assertions.assertEquals(original.getRecipientInternalId(), saved.getRecipientInternalId());
                        Assertions.assertNotNull(saved.getBaseCost());
                        Assertions.assertEquals(original.getBaseCost().getPaFee(), saved.getBaseCost().getPaFee());
                        Assertions.assertEquals(original.getBaseCost().getSendFee(), saved.getBaseCost().getSendFee());
                    })
                    .verifyComplete();

            // seconda chiamata con stessi dati: deve andare ancora OK
            StepVerifier.create(dao.updateBaseCostIfNotExistsOrMatch(original))
                    .assertNext(saved -> {
                        Assertions.assertEquals(original.getIun(), saved.getIun());
                        Assertions.assertEquals(original.getRecIndex(), saved.getRecIndex());
                        Assertions.assertEquals(original.getVat(), saved.getVat());
                        Assertions.assertEquals(original.getNotificationFeePolicy(), saved.getNotificationFeePolicy());
                        Assertions.assertEquals(original.getPagoPaIntMode(), saved.getPagoPaIntMode());
                        Assertions.assertEquals(original.getSenderPaId(), saved.getSenderPaId());
                        Assertions.assertEquals(original.getSenderTaxId(), saved.getSenderTaxId());
                        Assertions.assertEquals(original.getRecipientInternalId(), saved.getRecipientInternalId());
                        Assertions.assertNotNull(saved.getBaseCost());
                        Assertions.assertEquals(original.getBaseCost().getPaFee(), saved.getBaseCost().getPaFee());
                        Assertions.assertEquals(original.getBaseCost().getSendFee(), saved.getBaseCost().getSendFee());
                    })
                    .verifyComplete();

            NotificationDeliveryCost elementFromDb = dao.getNotificationDeliveryCostItem(iun, recIndex).block();

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals(original.getIun(), elementFromDb.getIun());
            Assertions.assertEquals(original.getRecIndex(), elementFromDb.getRecIndex());
            Assertions.assertEquals(original.getRecipientInternalId(), elementFromDb.getRecipientInternalId());
            Assertions.assertEquals(original.getSenderPaId(), elementFromDb.getSenderPaId());
            Assertions.assertEquals(original.getSenderTaxId(), elementFromDb.getSenderTaxId());
            Assertions.assertEquals(original.getBaseCost().getPaFee(), elementFromDb.getBaseCost().getPaFee());
            Assertions.assertEquals(original.getBaseCost().getSendFee(), elementFromDb.getBaseCost().getSendFee());
            Assertions.assertEquals(original.getVat(), elementFromDb.getVat());
            Assertions.assertEquals(original.getNotificationFeePolicy(), elementFromDb.getNotificationFeePolicy());
            Assertions.assertEquals(original.getPagoPaIntMode(), elementFromDb.getPagoPaIntMode());
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(iun, recIndex);
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void updateBaseCostIfNotExistsOrMatch_whenItemDoesNotExist_createsItem() {
        String iun = "iun-put-if-absent-" + System.nanoTime();
        Integer recIndex = 0;
        NotificationDeliveryCostEntity notification = newNotificationDeliveryCostEntity(iun, recIndex);

        try {
            StepVerifier.create(dao.updateBaseCostIfNotExistsOrMatch(notification))
                    .assertNext(saved -> {
                        Assertions.assertEquals(notification.getIun(), saved.getIun());
                        Assertions.assertEquals(notification.getRecIndex(), saved.getRecIndex());
                        Assertions.assertEquals(notification.getVat(), saved.getVat());
                        Assertions.assertEquals(notification.getNotificationFeePolicy(), saved.getNotificationFeePolicy());
                        Assertions.assertEquals(notification.getPagoPaIntMode(), saved.getPagoPaIntMode());
                        Assertions.assertEquals(notification.getSenderPaId(), saved.getSenderPaId());
                        Assertions.assertEquals(notification.getSenderTaxId(), saved.getSenderTaxId());
                        Assertions.assertEquals(notification.getRecipientInternalId(), saved.getRecipientInternalId());
                        Assertions.assertNotNull(saved.getBaseCost());
                        Assertions.assertEquals(notification.getBaseCost().getPaFee(), saved.getBaseCost().getPaFee());
                        Assertions.assertEquals(notification.getBaseCost().getSendFee(), saved.getBaseCost().getSendFee());
                    })
                    .verifyComplete();

            NotificationDeliveryCost elementFromDb = dao.getNotificationDeliveryCostItem(iun, recIndex).block();

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals(notification.getIun(), elementFromDb.getIun());
            Assertions.assertEquals(notification.getRecIndex(), elementFromDb.getRecIndex());
            Assertions.assertEquals(notification.getRecipientInternalId(), elementFromDb.getRecipientInternalId());
            Assertions.assertEquals(notification.getSenderPaId(), elementFromDb.getSenderPaId());
            Assertions.assertEquals(notification.getSenderTaxId(), elementFromDb.getSenderTaxId());
            Assertions.assertEquals(notification.getBaseCost().getPaFee(), elementFromDb.getBaseCost().getPaFee());
            Assertions.assertEquals(notification.getBaseCost().getSendFee(), elementFromDb.getBaseCost().getSendFee());
            Assertions.assertEquals(notification.getVat(), elementFromDb.getVat());
            Assertions.assertEquals(notification.getNotificationFeePolicy(), elementFromDb.getNotificationFeePolicy());
            Assertions.assertEquals(notification.getPagoPaIntMode(), elementFromDb.getPagoPaIntMode());
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(iun, recIndex);
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }


    @Test
    void getAllByIun_returnsOnlyItemsWithSameIun() {
        String iun = "iun-query-" + System.nanoTime();
        String otherIun = "iun-other-" + System.nanoTime();

        NotificationDeliveryCostEntity item0 = newNotificationDeliveryCostEntity(iun, 0);
        NotificationDeliveryCostEntity item1 = newNotificationDeliveryCostEntity(iun, 1);
        NotificationDeliveryCostEntity itemOther = newNotificationDeliveryCostEntity(otherIun, 0);

        try {
            testDao.putItem(item0);
            testDao.putItem(item1);
            testDao.putItem(itemOther);

            List<NotificationDeliveryCostEntity> items = dao.getAllByIun(iun).collectList().block();

            Assertions.assertNotNull(items);
            Assertions.assertEquals(2, items.size());
            Assertions.assertTrue(items.stream().allMatch(item -> iun.equals(item.getIun())));

            List<Integer> recIndexes = items.stream()
                    .map(NotificationDeliveryCostEntity::getRecIndex)
                    .sorted()
                    .collect(Collectors.toList());
            Assertions.assertEquals(List.of(0, 1), recIndexes);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(item0.getIun(), item0.getRecIndex());
                testDao.delete(item1.getIun(), item1.getRecIndex());
                testDao.delete(itemOther.getIun(), itemOther.getRecIndex());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void getAllByIun_returnsEmptyPageWhenNoItemsExist() {
        String iun = "iun-missing-" + System.nanoTime();

        StepVerifier.create(dao.getAllByIun(iun))
                .expectNextCount(0)
                .verifyComplete();
    }
}
