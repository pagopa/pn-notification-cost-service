package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NotificationCostInitializationEvent.Payload.class, name = "NOTIFICATION_COST_INITIALIZATION")
})
public interface InternalEvent {
    InternalEventType getEventType();
}
