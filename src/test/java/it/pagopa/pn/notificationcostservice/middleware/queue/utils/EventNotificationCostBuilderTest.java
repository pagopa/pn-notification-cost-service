package it.pagopa.pn.notificationcostservice.middleware.queue.utils;
import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.NotificationCostInitializationEventType;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.NotificationCostRequest;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentData;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.RecipientCostData;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class EventNotificationCostBuilderTest {
    @Test
    void buildNotificationCostEventCreatesExpectedHeaderAndPayload() {
        String iun = "TEST-IUN-987";
        NotificationCostRequest request = NotificationCostRequest.builder()
                .recipients(List.of(RecipientCostData.builder()
                        .recIndex(1)
                        .recipientInternalId("recipient")
                        .senderInternalId("sender")
                        .payments(List.of(PaymentData.builder().iuv("IUV-1").applyCost(true).build()))
                        .baseCost(100)
                        .sendFee(10)
                        .paFee(90)
                        .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
                        .pagoPaIntMode(PagoPaIntMode.SYNC)
                        .vat(22)
                        .build()))
                .build();
        var event = EventNotificationCostBuilder.buildNotificationCostEvent(request, iun);
        assertNotNull(event.getHeader());
        assertNotNull(event.getHeader().getCreatedAt());
        assertEquals(NotificationCostInitializationEventType.NOTIFICATION_COST_INITIALIZATION.getValue(), event.getHeader().getEventType());
        assertEquals(EventPublisher.NOTIFICATION_COST_SERVICE.name(), event.getHeader().getPublisher());
        assertTrue(event.getHeader().getEventId().startsWith("notification_cost_init_"));
        assertTrue(event.getHeader().getEventId().length() <= 79);
        assertEquals(iun, event.getPayload().getIun());
        assertEquals(request.getRecipients(), event.getPayload().getRecipients());
    }
    @Test
    void buildNotificationCostEventThrowsWhenIunIsNull() {
        NotificationCostRequest request = NotificationCostRequest.builder().recipients(List.of()).build();
        assertThrows(NullPointerException.class, () -> EventNotificationCostBuilder.buildNotificationCostEvent(request, null));
    }
    @Test
    void buildNotificationCostEventThrowsWhenRecipientsAreNull() {
        NotificationCostRequest request = NotificationCostRequest.builder().recipients(null).build();
        assertThrows(NullPointerException.class, () -> EventNotificationCostBuilder.buildNotificationCostEvent(request, "IUN"));
    }
}
