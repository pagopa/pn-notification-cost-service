package it.pagopa.pn.notificationcostservice.middleware.queue.router;

import it.pagopa.pn.notificationcostservice.exception.PnEventRouterException;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEventType;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost.NotificationCostInitializationEventHandler;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.router.impl.InternalEventsRouterImpl;
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
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalEventsRouterImplTest {

    @Mock
    private NotificationCostInitializationEventHandler notificationCostInitializationEventHandler;

    @InjectMocks
    private InternalEventsRouterImpl router;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void handleEvent_shouldRouteNotificationCostInitializationPayload() {
        NotificationCostInitializationEvent.Payload payload =
                NotificationCostInitializationEvent.Payload.builder()
                        .iun("TEST-IUN-123")
                        .eventType(InternalEventType.NOTIFICATION_COST_INITIALIZATION)
                        .build();

        Message<InternalEvent> message = MessageBuilder
                .withPayload((InternalEvent) payload)
                .setHeader("eventId", "evt-123")
                .build();

        when(notificationCostInitializationEventHandler.handleNotificationCostInitializationEvent(payload))
                .thenReturn(Mono.empty());

        StepVerifier.create(router.handleEvent(message))
                .verifyComplete();

        verify(notificationCostInitializationEventHandler)
                .handleNotificationCostInitializationEvent(payload);
        verifyNoMoreInteractions(notificationCostInitializationEventHandler);
    }

    @Test
    void handleEvent_shouldPropagateHandlerError() {
        NotificationCostInitializationEvent.Payload payload =
                NotificationCostInitializationEvent.Payload.builder()
                        .iun("TEST-IUN-123")
                        .eventType(InternalEventType.NOTIFICATION_COST_INITIALIZATION)
                        .build();

        Message<InternalEvent> message = MessageBuilder
                .withPayload((InternalEvent) payload)
                .setHeader("eventId", "evt-456")
                .build();

        RuntimeException expectedException = new RuntimeException("handler failed");

        when(notificationCostInitializationEventHandler.handleNotificationCostInitializationEvent(payload))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(router.handleEvent(message))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "handler failed".equals(ex.getMessage()))
                .verify();

        verify(notificationCostInitializationEventHandler)
                .handleNotificationCostInitializationEvent(payload);
    }

    @Test
    void handleEvent_shouldReturnPnEventRouterExceptionForUnsupportedPayload() {
        InternalEvent unsupportedPayload = new UnsupportedInternalEvent();

        Message<InternalEvent> message = MessageBuilder
                .withPayload(unsupportedPayload)
                .build();

        StepVerifier.create(router.handleEvent(message))
                .expectErrorSatisfies(ex ->
                        assertInstanceOf(PnEventRouterException.class, ex))
                .verify();

        verifyNoInteractions(notificationCostInitializationEventHandler);
    }

    private static class UnsupportedInternalEvent implements InternalEvent {
        @Override
        public InternalEventType getEventType() {
            return InternalEventType.COST_UPDATE;
        }
    }
}
