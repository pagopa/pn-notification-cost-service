package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsumerUtilsTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void setMdc_shouldPopulateMdcFromHeaders_andPreferHeaderIun() {
        String headerIun = "IUN_FROM_HEADER";
        String payloadIun = "IUN_FROM_PAYLOAD";

        Message<?> message = MessageBuilder
                .withPayload(NotificationCostInitializationEvent.Payload.builder()
                        .iun(payloadIun)
                        .build())
                .setHeader("aws_messageId", "aws-message-id-123")
                .setHeader("X-Amzn-Trace-Id", "trace-id-123")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_IUN, headerIun)
                .build();

        ConsumerUtils.setMdc(message);

        assertEquals(headerIun, MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        assertEquals("aws-message-id-123", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertEquals("trace-id-123", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
    }

    @Test
    void setMdc_shouldExtractIunFromPayloadWhenHeaderIsMissing() {
        String payloadIun = "IUN_FROM_PAYLOAD";

        Message<?> message = MessageBuilder
                .withPayload(NotificationCostInitializationEvent.Payload.builder()
                        .iun(payloadIun)
                        .build())
                .setHeader("aws_messageId", "aws-message-id-456")
                .setHeader("X-Amzn-Trace-Id", "trace-id-456")
                .build();

        ConsumerUtils.setMdc(message);

        assertEquals(payloadIun, MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        assertEquals("aws-message-id-456", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertEquals("trace-id-456", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
    }

    @Test
    void setMdc_shouldGenerateTraceIdWhenHeaderIsMissing() {
        Message<?> message = MessageBuilder
                .withPayload(NotificationCostInitializationEvent.Payload.builder()
                        .iun("IUN_NO_TRACE")
                        .build())
                .setHeader("aws_messageId", "aws-message-id-789")
                .build();

        ConsumerUtils.setMdc(message);

        String generatedTraceId = MDC.get(MDCUtils.MDC_TRACE_ID_KEY);

        assertEquals("IUN_NO_TRACE", MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        assertEquals("aws-message-id-789", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertNotNull(generatedTraceId);
        assertDoesNotThrow(() -> UUID.fromString(generatedTraceId));
    }

    @Test
    void setMdc_shouldNotSetIunWhenHeaderAndPayloadIunAreMissingOrBlank() {
        Message<?> message = MessageBuilder
                .withPayload(NotificationCostInitializationEvent.Payload.builder()
                        .iun(" ")
                        .build())
                .setHeader("aws_messageId", "aws-message-id-999")
                .setHeader("X-Amzn-Trace-Id", "trace-id-999")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_IUN, " ")
                .build();

        ConsumerUtils.setMdc(message);

        assertNull(MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        assertEquals("aws-message-id-999", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertEquals("trace-id-999", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
    }
}
