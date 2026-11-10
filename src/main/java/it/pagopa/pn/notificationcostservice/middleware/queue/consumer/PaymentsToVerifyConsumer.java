package it.pagopa.pn.notificationcostservice.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.NoticePaymentEventHandler;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.notificationcostservice.middleware.queue.consumer.utils.ConsumerUtils.setMdc;

@Component
@AllArgsConstructor
@CustomLog
public class PaymentsToVerifyConsumer {

    private final NoticePaymentEventHandler noticePaymentEventHandler;

    @SqsListener(value = "${pn.notification-cost-service.topics.payments-to-verify}")
    public void paymentsToVerifyConsumer(Message<PnNoticePaymentEvent.Payload> message) {
        final String processName = "NotificationDeliveryCostEventConsumer";
        log.logStartingProcess(processName);
        setMdc(message);
        try {
            log.info("Handle pnNotificationDeliveryCostEventConsumer, messageId={}",
                    message.getHeaders().getId());
            MDCUtils.addMDCToContextAndExecute(noticePaymentEventHandler.handle(message.getPayload())).block();
            log.logEndingProcess(processName);

        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage(), ex);
            HandleEventUtils.handleException(message, ex);
            throw ex;
        }
    }
}

