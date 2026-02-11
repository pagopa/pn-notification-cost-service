package it.pagopa.pn.notificationcostservice.middleware.notificationdeliverycost.dynamo;

import it.pagopa.pn.notificationcostservice.LocalStackTestConfig;
import it.pagopa.pn.notificationcostservice.MockAWSObjectsTest;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.NotificationDeliveryCostDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.NotificationDeliveryCostEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
            NotificationDeliveryCostDto elementFromDb = dao.getNotificationDeliveryCostItem(iun,recIndex).block();
            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals(notificationDeliveryCostEntity, elementFromDb);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(notificationDeliveryCostEntity.getPk(), notificationDeliveryCostEntity.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
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

    public static NotificationDeliveryCostEntity newNotificationDeliveryCostEntity(String iun, Integer recIndex) {
        return NotificationDeliveryCostEntity.builder()
                .pk(iun)
                .sk(recIndex)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .baseCost(12)
                .vat(0)
                .sendFee(10)
                .paFee(2)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .isRefused(false)
                .isCancelled(false)
                .notificationViewDate(Instant.now())
                .lastUpdate(Instant.now())
                .ttl(10000L)
                .firstAnalogCost(0)
                .secondAnalogCost(0)
                .recipientInternalId("recipientInternalId")
                .build();
    }
}
