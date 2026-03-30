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
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.List;
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
    void updateNotNullWhenItemDoesNotExist() {
        String iun = "iun-put-if-absent-" + System.nanoTime();
        Integer recIndex = 0;
        NotificationDeliveryCostEntity notification = newNotificationDeliveryCostEntity(iun, recIndex);

        try {
            StepVerifier.create(dao.updateNotificationDeliveryCostNotNull(notification))
                    .verifyComplete();

            NotificationDeliveryCost elementFromDb = dao.getNotificationDeliveryCostItem(iun, recIndex).block();

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals(notification.getIun(), elementFromDb.getIun());
            Assertions.assertEquals(notification.getRecIndex(), elementFromDb.getRecIndex());
            Assertions.assertEquals(notification.getRecipientInternalId(), elementFromDb.getRecipientInternalId());
            Assertions.assertEquals(notification.getSenderPaId(), elementFromDb.getSenderPaId());
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
    void updateNotificationDeliveryCost_whenNotNullAlreadyExists_updatesOnlyNonNullFields() {
        String iun = "iun-update-existing-" + System.nanoTime();
        int recIndex = 1;

        NotificationDeliveryCostEntity original = NotificationDeliveryCostEntity.builder()
                .iun(iun)
                .recIndex(recIndex)
                .recipientInternalId("recipient-original")
                .senderPaId("sender-original")
                .baseCost(BaseCostEntity.builder()
                        .paFee(2)
                        .sendFee(10)
                        .build())
                .vat(22)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .isDeleted(false)
                .lastUpdate(Instant.now())
                .build();

        NotificationDeliveryCostEntity updated = NotificationDeliveryCostEntity.builder()
                .iun(iun)
                .recIndex(recIndex)
                .recipientInternalId("recipient-updated")
                .senderPaId(null)
                .baseCost(BaseCostEntity.builder()
                        .paFee(5)
                        .sendFee(15)
                        .build())
                .vat(22)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .isDeleted(false)
                .lastUpdate(Instant.now())
                .build();

        try {
            StepVerifier.create(dao.updateNotificationDeliveryCostNotNull(original))
                    .verifyComplete();

            StepVerifier.create(dao.updateNotificationDeliveryCostNotNull(updated))
                    .verifyComplete();

            NotificationDeliveryCost elementFromDb = dao.getNotificationDeliveryCostItem(iun, recIndex).block();

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals("recipient-updated", elementFromDb.getRecipientInternalId());
            Assertions.assertEquals("sender-original", elementFromDb.getSenderPaId());
            Assertions.assertEquals(5, elementFromDb.getBaseCost().getPaFee());
            Assertions.assertEquals(15, elementFromDb.getBaseCost().getSendFee());
            Assertions.assertEquals(NotificationFeePolicy.DELIVERY_MODE, elementFromDb.getNotificationFeePolicy());
            Assertions.assertEquals(PagoPaIntMode.ASYNC, elementFromDb.getPagoPaIntMode());
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

            Page<NotificationDeliveryCostEntity> resultPage = dao.getAllByIun(iun).block();

            Assertions.assertNotNull(resultPage);
            List<NotificationDeliveryCostEntity> items = resultPage.items();
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
                .assertNext(page -> Assertions.assertTrue(page.items().isEmpty()))
                .verifyComplete();
    }
}
