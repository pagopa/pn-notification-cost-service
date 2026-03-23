package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.utils;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEventType;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEventType.NOTIFICATION_COST_INITIALIZATION;

@NoArgsConstructor
public class NotificationCostEventBuilder {
    private static final String NOTIFICATION_COST_INIT_EVENT_ID_DESCRIPTOR = "notification_cost_init_";
    private static final int MAX_EVENT_ID_LENGTH = 79;


    public static NotificationCostInitializationEvent buildNotificationCostEvent(String iun, List<NotificationDeliveryCost> notificationCosts, List<PaymentInfo> payments) {
        return NotificationCostInitializationEvent.builder()
                .header(buildInternalEventHeader(iun))
                .payload(NotificationCostInitializationEvent.Payload.builder()
                        .iun(iun)
                        .notificationCosts(notificationCosts)
                        .payments(payments)
                        .eventType(InternalEventType.NOTIFICATION_COST_INITIALIZATION)
                        .build())
                .build();
    }

    private static GenericEventHeader buildInternalEventHeader(String pk) {
        return GenericEventHeader.builder()
                .eventId(generateEventId(pk))
                .eventType(NOTIFICATION_COST_INITIALIZATION.name())
                .publisher(EventPublisher.NOTIFICATION_COST_SERVICE.name())
                .createdAt(Instant.now())
                .build();
    }

    private static String generateEventId(String requestId) {
        String eventId = (NOTIFICATION_COST_INIT_EVENT_ID_DESCRIPTOR + UUID.randomUUID() + "_" + requestId)
                .replaceAll("[^a-zA-Z0-9]", "_");
        return eventId.substring(0, Math.min(MAX_EVENT_ID_LENGTH, eventId.length()));
    }
}
