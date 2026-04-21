package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEventType;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class UpdateNotificationCostEvent implements GenericEvent<GenericEventHeader, UpdateNotificationCostEvent.Payload> {
    private GenericEventHeader header;
    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload implements InternalEvent {
        private String iun;
        private InternalEventType eventType = InternalEventType.COST_UPDATE;
        private Integer recIndex;
        private Integer cost;
        private String productType;
        private CostUpdatePhaseInt costUpdatePhase;
        private Instant elementTimestamp;
    }
}
