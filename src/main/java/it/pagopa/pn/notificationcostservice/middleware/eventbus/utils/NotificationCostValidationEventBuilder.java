package it.pagopa.pn.notificationcostservice.middleware.eventbus.utils;

import it.pagopa.pn.api.dto.events.notificationcost.utils.ValidationStatus;
import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEvent;
import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEventPayload;

public class NotificationCostValidationEventBuilder {

    private NotificationCostValidationEventBuilder() {
        // utility class, prevent instantiation
    }
    public static PnNotificationCostValidationEvent buildOkValidationEvent(String iun) {
        return PnNotificationCostValidationEvent.builder()
                .detail(
                        PnNotificationCostValidationEvent.Detail.builder()
                                //ToDo: for now the field will be unused
//                                .clientId("pn-notification-cost-service")
                                .pnNotificationCostValidationPayload(
                                        PnNotificationCostValidationEventPayload.builder()
                                                .iun(iun)
                                                .status(ValidationStatus.OK)
                                                .build()
                                )
                                .build()
                )
                .build();
    }
}
