package it.pagopa.pn.notificationcostservice.middleware.queue.consumer;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.router.InternalEventsRouter;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalQueueConsumerTest {

    @Mock
    private InternalEventsRouter router;

    @InjectMocks
    private InternalQueueConsumer internalQueueConsumer;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void pnNotificationDeliveryCostEventConsumer_shouldHandleMessageSuccessfully() {
        // given
        String iun = "IUN_TEST_123";
        Message<InternalEvent> message = buildMessage(iun, true);

        when(router.handleEvent(message)).thenReturn(Mono.empty());

        // when / then
        assertDoesNotThrow(() -> internalQueueConsumer.pnNotificationDeliveryCostEventConsumer(message));

        verify(router).handleEvent(message);
        assertEquals(iun, MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        assertEquals("aws-message-id-123", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertEquals("trace-id-123", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
    }

    @Test
    void pnNotificationDeliveryCostEventConsumer_shouldExtractIunFromPayloadWhenHeaderIsMissing() {
        // given
        String iun = "IUN_FROM_PAYLOAD";
        Message<InternalEvent> message = buildMessage(iun, false);

        when(router.handleEvent(message)).thenReturn(Mono.empty());

        // when
        internalQueueConsumer.pnNotificationDeliveryCostEventConsumer(message);

        // then
        verify(router).handleEvent(message);
        assertEquals(iun, MDC.get(MDCUtils.MDC_PN_IUN_KEY));
    }

    @Test
    void pnNotificationDeliveryCostEventConsumer_shouldRethrowExceptionWhenRouterFails() {
        // given
        String iun = "IUN_ERROR_123";
        Message<InternalEvent> message = buildMessage(iun, true);
        RuntimeException expectedException = new RuntimeException("Router failure");

        when(router.handleEvent(message)).thenReturn(Mono.error(expectedException));

        // when
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> internalQueueConsumer.pnNotificationDeliveryCostEventConsumer(message)
        );

        // then
        verify(router).handleEvent(message);
        assertNotNull(thrown);
        assertEquals("Router failure", thrown.getMessage());
        assertEquals(iun, MDC.get(MDCUtils.MDC_PN_IUN_KEY));
    }

    private Message<InternalEvent> buildMessage(String iun, boolean putIunInHeader) {
        NotificationCostInitializationEvent.Payload payload =
                NotificationCostInitializationEvent.Payload.builder()
                        .iun(iun)
                        .build();

        MessageBuilder<InternalEvent> builder = MessageBuilder
                .withPayload((InternalEvent) payload)
                .setHeader("aws_messageId", "aws-message-id-123")
                .setHeader("X-Amzn-Trace-Id", "trace-id-123");

        if (putIunInHeader) {
            builder.setHeader(StandardEventHeader.PN_EVENT_HEADER_IUN, iun);
        }

        return builder.build();
    }
}

