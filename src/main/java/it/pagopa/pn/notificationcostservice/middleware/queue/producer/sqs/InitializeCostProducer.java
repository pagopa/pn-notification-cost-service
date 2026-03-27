package it.pagopa.pn.notificationcostservice.middleware.queue.producer.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.List;

@Component
@Slf4j
public class InitializeCostProducer extends AbstractSqsMomProducer<NotificationCostInitializationEvent> {
    public InitializeCostProducer(SqsClient sqsClient, PnNotificationCostServiceConfigs configs, ObjectMapper objectMapper) {
        super(sqsClient, configs.getTopics().getPnNotificationCostToUpdate(), objectMapper, NotificationCostInitializationEvent.class);
    }

    @Override
    public void push(List<NotificationCostInitializationEvent> message) {
        super.push(message);
    }
}
