package it.pagopa.pn.notificationcostservice.middleware.queue.utils;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.utils.NotificationCostEventBuilder;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEventType.NOTIFICATION_COST_INITIALIZATION;
import static org.junit.jupiter.api.Assertions.*;

class NotificationCostEventBuilderTest {
    @Test
    void buildNotificationCostEventCreatesExpectedHeaderAndPayload() {
        String iun = "TEST-IUN-987";

        List<NotificationDeliveryCost> notificationCosts = List.of(
                NotificationDeliveryCostTestBuilder.builder()
                        .withIun(iun)
                        .withRecIndex(1)
                        .withSenderPaId("TEST-SENDER-PA-ID")
                        .withSenderTaxId("TEST-SENDER-TAX-ID")
                        .withLastUpdate(Instant.now())
                        .build()
        );

        List<PaymentInfo> payments = List.of(
                PaymentInfo.builder()
                        .iun(iun)
                        .recIndex(1)
                        .iuv("IUV-1")
                        .applyCost(true)
                        .build()
        );

        var event = NotificationCostEventBuilder.buildNotificationCostEvent(iun, notificationCosts, payments);

        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertNotNull(event.getHeader().getCreatedAt());
        assertEquals(NOTIFICATION_COST_INITIALIZATION.name(),
                event.getHeader().getEventType());
        assertEquals(EventPublisher.NOTIFICATION_COST_SERVICE.name(), event.getHeader().getPublisher());
        assertTrue(event.getHeader().getEventId().startsWith("notification_cost_init_"));
        assertTrue(event.getHeader().getEventId().length() <= 79);

        assertNotNull(event.getPayload());
        assertEquals(iun, event.getPayload().getIun());
        assertEquals(notificationCosts, event.getPayload().getNotificationCosts());
        assertEquals(payments, event.getPayload().getPayments());
    }

    @Test
    void buildNotificationCostEventAllowsNullIunWithCurrentImplementation() {
        List<NotificationDeliveryCost> notificationCosts = List.of(
                NotificationDeliveryCostTestBuilder.builder().withSenderPaId("TEST-SENDER-PA-ID")
                        .withSenderTaxId("TEST-SENDER-TAX-ID")
                        .withLastUpdate(Instant.now()).build()
        );
        List<PaymentInfo> payments = List.of();

        var event = NotificationCostEventBuilder.buildNotificationCostEvent(null, notificationCosts, payments);

        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertNotNull(event.getPayload());
        assertNull(event.getPayload().getIun());
        assertEquals(notificationCosts, event.getPayload().getNotificationCosts());
        assertEquals(payments, event.getPayload().getPayments());
    }

    @Test
    void buildNotificationCostEventAllowsNullNotificationCostsWithCurrentImplementation() {
        String iun = "IUN";
        List<PaymentInfo> payments = List.of();

        var event = NotificationCostEventBuilder.buildNotificationCostEvent(iun, null, payments);

        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertNotNull(event.getPayload());
        assertEquals(iun, event.getPayload().getIun());
        assertNull(event.getPayload().getNotificationCosts());
        assertEquals(payments, event.getPayload().getPayments());
    }
}
