package it.pagopa.pn.notificationcostservice.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.router.InternalEventsRouter;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.notificationcostservice.middleware.queue.consumer.utils.ConsumerUtils.setMdc;

@Component
@AllArgsConstructor
@CustomLog
public class InternalQueueConsumer {

    private final InternalEventsRouter router;

    @SqsListener(value = "${pn.notification-cost-service.topics.pn-notification-cost-to-update}")
    public void pnNotificationDeliveryCostEventConsumer(Message<InternalEvent> message) {
        final String processName = "NotificationDeliveryCostEventConsumer";
        log.logStartingProcess(processName);
        setMdc(message);
        try {
            log.info("Handle pnNotificationDeliveryCostEventConsumer, messageId={}, payloadType={}",
                    message.getHeaders().getId(),
                    message.getPayload() != null ? message.getPayload().getClass().getSimpleName() : "null");
            if (log.isDebugEnabled()) {
                log.debug("Full message content for pnNotificationDeliveryCostEventConsumer: {}", message);
            }
            router.handleEvent(message).block();
            log.logEndingProcess(processName);

        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage(), ex);
            HandleEventUtils.handleException(message, ex);
            throw ex;
        }
    }
}

