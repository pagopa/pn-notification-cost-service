package it.pagopa.pn.notificationcostservice.middleware.queue.consumer;

import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.NoticePaymentEventHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentsToVerifyConsumerTest {

    @Mock
    private NoticePaymentEventHandler noticePaymentEventHandler;

    @InjectMocks
    private PaymentsToVerifyConsumer paymentsToVerifyConsumer;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void paymentsToVerifyConsumer_shouldHandleMessageSuccessfully() {
        // given
        Message<PnNoticePaymentEvent.Payload> message = buildMessage();

        when(noticePaymentEventHandler.handle(message.getPayload())).thenReturn(Mono.empty());

        // when / then
        assertDoesNotThrow(() -> paymentsToVerifyConsumer.paymentsToVerifyConsumer(message));

        verify(noticePaymentEventHandler).handle(message.getPayload());
        assertEquals("aws-message-id-123", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertEquals("trace-id-123", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
    }

    @Test
    void paymentsToVerifyConsumer_shouldRethrowExceptionWhenHandlerFails() {
        // given
        Message<PnNoticePaymentEvent.Payload> message = buildMessage();
        RuntimeException expectedException = new RuntimeException("Router failure");

        when(noticePaymentEventHandler.handle(message.getPayload())).thenReturn(Mono.error(expectedException));

        // when
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> paymentsToVerifyConsumer.paymentsToVerifyConsumer(message)
        );

        // then
        verify(noticePaymentEventHandler).handle(message.getPayload());
        assertNotNull(thrown);
        assertEquals("Router failure", thrown.getMessage());
    }

    private Message<PnNoticePaymentEvent.Payload> buildMessage() {
        PnNoticePaymentEvent.Payload payload =
                PnNoticePaymentEvent.Payload.builder()
                        .paymentId("test-payment-id")
                        .build();

        MessageBuilder<PnNoticePaymentEvent.Payload> builder = MessageBuilder
                .withPayload(payload)
                .setHeader("aws_messageId", "aws-message-id-123")
                .setHeader("X-Amzn-Trace-Id", "trace-id-123");

        return builder.build();
    }
}