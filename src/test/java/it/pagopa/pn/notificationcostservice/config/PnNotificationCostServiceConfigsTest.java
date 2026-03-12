package it.pagopa.pn.notificationcostservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class PnNotificationCostServiceConfigsTest {

    @Autowired
    private PnNotificationCostServiceConfigs pnNotificationCostServiceConfigs;

    @Test
    void testConfigLoading() {
        assertNotNull(pnNotificationCostServiceConfigs);

        PnNotificationCostServiceConfigs.NotificationDeliveryCostTable deliveryCostTable = pnNotificationCostServiceConfigs.getNotificationDeliveryCostTable();
        assertNotNull(deliveryCostTable);
        assertEquals("pn-NotificationDeliveryCost", deliveryCostTable.getTableName());

        PnNotificationCostServiceConfigs.PaymentInfoTable paymentInfoTable = pnNotificationCostServiceConfigs.getPaymentInfoTable();
        assertNotNull(paymentInfoTable);
        assertEquals("pn-PaymentInfo", paymentInfoTable.getTableName());

        PnNotificationCostServiceConfigs.Topics topics = pnNotificationCostServiceConfigs.getTopics();
        assertNotNull(topics);
        assertEquals("pn-cost-to-update.fifo", topics.getPnCostToUpdate());
    }
}

