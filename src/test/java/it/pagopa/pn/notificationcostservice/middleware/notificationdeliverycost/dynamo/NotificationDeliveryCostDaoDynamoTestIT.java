package it.pagopa.pn.notificationcostservice.middleware.notificationdeliverycost.dynamo;

import it.pagopa.pn.notificationcostservice.LocalStackTestConfig;
import it.pagopa.pn.notificationcostservice.MockAWSObjectsTest;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.NotificationDeliveryCostDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCost;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.time.Instant;

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
            NotificationDeliveryCostDto elementFromDb = dao.getNotificationDeliveryCostItem(iun, recIndex).block();

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
                .ttl(10000L)
                .firstAnalogCost(FirstAnalogCost.builder()
                        .cost(50)
                        .productType("AR")
                        .build())
                .secondAnalogCost(SecondAnalogCost.builder()
                        .cost(30)
                        .productType("890")
                        .build())
                .simpleRegisteredLetterCost(null)
                .recipientInternalId("recipientInternalId")
                .build();
    }

    private static BaseCost newBaseCost() {
        return BaseCost.builder()
                .paFee(2)
                .sendFee(10)
                .build();
    }


    @Test
    void getNotificationDeliveryCostItemWhenItemNotFound() {
        String iun = "iun";
        Integer recIndex = 0;
        try {
            dao.getNotificationDeliveryCostItem(iun,recIndex).block();
            Assertions.fail("Expected PnNotFoundException to be thrown");
        } catch (Throwable e) {
            Assertions.assertInstanceOf(PnNotFoundException.class, e);
        }
    }
}
