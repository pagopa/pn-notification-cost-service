package it.pagopa.pn.notificationcostservice.middleware.eventbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;

@Component
public class PnNoticePaymentEventProducer
        extends AbstractEventBridgeProducer<PnNoticePaymentPaidEvent> {

    public PnNoticePaymentEventProducer(
            EventBridgeAsyncClient amazonEventBridge,
            PnNotificationCostServiceConfigs configs,
            ObjectMapper objectMapper
    ) {
        super(
                amazonEventBridge,
                configs.getEventBus().getSource(),
                configs.getEventBus().getNoticePaymentPaidDetailType(),
                configs.getEventBus().getName(),
                objectMapper
        );
    }
}
