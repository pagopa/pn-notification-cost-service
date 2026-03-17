package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.RecipientCostData;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationCostInitializationEvent implements GenericEvent<GenericEventHeader, NotificationCostInitializationEvent.Payload> {
    private GenericEventHeader header;
    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String iun;
        private List<RecipientCostData> recipients;
    }
}
