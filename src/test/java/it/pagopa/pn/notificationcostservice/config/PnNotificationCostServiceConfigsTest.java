package it.pagopa.pn.notificationcostservice.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PnNotificationCostServiceConfigsTest {

        private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testConfigLoading() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("pn.notification-cost-service.notification-delivery-cost-table.table-name", "pn-NotificationDeliveryCost")
                .withProperty("pn.notification-cost-service.payment-info-table.table-name", "pn-PaymentInfo")
                .withProperty("pn.notification-cost-service.topics.pn-notification-cost-to-update", "pn-notification-cost-to-update")
                .withProperty("pn.notification-cost-service.send-fee", "100");

        PnNotificationCostServiceConfigs pnNotificationCostServiceConfigs = Binder.get(environment)
                .bind("pn.notification-cost-service", Bindable.of(PnNotificationCostServiceConfigs.class))
                .orElseThrow(() -> new IllegalStateException("Failed to bind PnNotificationCostServiceConfigs"));

        assertNotNull(pnNotificationCostServiceConfigs);

        PnNotificationCostServiceConfigs.NotificationDeliveryCostTable deliveryCostTable =
                pnNotificationCostServiceConfigs.getNotificationDeliveryCostTable();
        assertNotNull(deliveryCostTable);
        assertEquals("pn-NotificationDeliveryCost", deliveryCostTable.getTableName());

        PnNotificationCostServiceConfigs.PaymentInfoTable paymentInfoTable =
                pnNotificationCostServiceConfigs.getPaymentInfoTable();
        assertNotNull(paymentInfoTable);
        assertEquals("pn-PaymentInfo", paymentInfoTable.getTableName());

        PnNotificationCostServiceConfigs.Topics topics =
                pnNotificationCostServiceConfigs.getTopics();
        assertNotNull(topics);
        assertEquals("pn-notification-cost-to-update", topics.getPnNotificationCostToUpdate());
        assertEquals(100, pnNotificationCostServiceConfigs.getSendFee());
    }

    @Test
    void shouldRequireSendFee() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("pn.notification-cost-service.notification-delivery-cost-table.table-name", "pn-NotificationDeliveryCost")
                .withProperty("pn.notification-cost-service.payment-info-table.table-name", "pn-PaymentInfo")
                .withProperty("pn.notification-cost-service.topics.pn-notification-cost-to-update", "pn-notification-cost-to-update");

        PnNotificationCostServiceConfigs pnNotificationCostServiceConfigs = Binder.get(environment)
                .bind("pn.notification-cost-service", Bindable.of(PnNotificationCostServiceConfigs.class))
                .orElseThrow(() -> new IllegalStateException("Failed to bind PnNotificationCostServiceConfigs"));

        assertTrue(validator.validate(pnNotificationCostServiceConfigs).stream()
                .anyMatch(violation -> "sendFee".equals(violation.getPropertyPath().toString())));
    }
}