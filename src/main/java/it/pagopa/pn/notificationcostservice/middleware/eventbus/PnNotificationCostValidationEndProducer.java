package it.pagopa.pn.notificationcostservice.middleware.eventbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEvent;
import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;

@Component
public class PnNotificationCostValidationEndProducer
        extends AbstractEventBridgeProducer<PnNotificationCostValidationEvent> {

    public PnNotificationCostValidationEndProducer(
            EventBridgeAsyncClient amazonEventBridge,
            PnNotificationCostServiceConfigs configs,
            ObjectMapper objectMapper
    ) {
        super(
                amazonEventBridge,
                configs.getEventBus().getSource(),
                configs.getEventBus().getOutcomeEventDetailType(),
                configs.getEventBus().getName(),
                objectMapper
        );
    }
}
