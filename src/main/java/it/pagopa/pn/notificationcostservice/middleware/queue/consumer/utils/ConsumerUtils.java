package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.UUID;


public class ConsumerUtils {

    private ConsumerUtils(){
        // utility class, prevent instantiation
    }

    public static void setMdc(Message<?> message) {
        MessageHeaders messageHeaders = message.getHeaders();
        MDCUtils.clearMDCKeys();

        if (messageHeaders.containsKey("aws_messageId")) {
            String awsMessageId = messageHeaders.get("aws_messageId", String.class);
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, awsMessageId);
        }

        if (messageHeaders.containsKey("X-Amzn-Trace-Id")) {
            String traceId = messageHeaders.get("X-Amzn-Trace-Id", String.class);
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }

        extractIun(message);
    }

    private static void extractIun(Message<?> message) {
        Object iunFromHeader = message.getHeaders().get(StandardEventHeader.PN_EVENT_HEADER_IUN);
        if (iunFromHeader instanceof String iun && !iun.isBlank()) {
            MDC.put(MDCUtils.MDC_PN_IUN_KEY, iun);
            return;
        }

        Object payload = message.getPayload();

        if (payload instanceof NotificationCostInitializationEvent.Payload notificationPayload) {
            String iun = notificationPayload.getIun();
            if (iun != null && !iun.isBlank()) {
                MDC.put(MDCUtils.MDC_PN_IUN_KEY, iun);
            }
        }
    }
}
