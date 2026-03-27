package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HandleEventUtilsTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void mapStandardEventHeader_shouldMapAllFields() {
        Instant createdAt = Instant.parse("2024-01-01T10:15:30Z");

        MessageHeaders headers = MessageBuilder.withPayload("payload")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_EVENT_ID, "evt-123")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_IUN, "IUN-123")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_EVENT_TYPE, "NOTIFICATION_COST_INITIALIZATION")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_CREATED_AT, createdAt.toString())
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_PUBLISHER, "notification-cost-service")
                .build()
                .getHeaders();

        StandardEventHeader result = HandleEventUtils.mapStandardEventHeader(headers);

        assertNotNull(result);
        assertEquals("evt-123", result.getEventId());
        assertEquals("IUN-123", result.getIun());
        assertEquals("NOTIFICATION_COST_INITIALIZATION", result.getEventType());
        assertEquals(createdAt, result.getCreatedAt());
        assertEquals("notification-cost-service", result.getPublisher());
    }

    @Test
    void mapStandardEventHeader_shouldReturnNullCreatedAtWhenHeaderIsMissing() {
        MessageHeaders headers = MessageBuilder.withPayload("payload")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_EVENT_ID, "evt-456")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_IUN, "IUN-456")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_EVENT_TYPE, "NOTIFICATION_COST_INITIALIZATION")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_PUBLISHER, "notification-cost-service")
                .build()
                .getHeaders();

        StandardEventHeader result = HandleEventUtils.mapStandardEventHeader(headers);

        assertNotNull(result);
        assertEquals("evt-456", result.getEventId());
        assertEquals("IUN-456", result.getIun());
        assertNull(result.getCreatedAt());
    }

    @Test
    void mapStandardEventHeader_shouldThrowWhenHeadersAreNull() {
        assertThrows(
                PnInternalException.class,
                () -> HandleEventUtils.mapStandardEventHeader(null)
        );
    }


    @Test
    void getEventId_shouldReturnEventIdWhenPresent() {
        NotificationCostInitializationEvent.Payload payload =
                NotificationCostInitializationEvent.Payload.builder()
                        .iun("IUN-GET-EVENT")
                        .build();

        Message<InternalEvent> message = MessageBuilder
                .withPayload((InternalEvent) payload)
                .setHeader("eventId", "corr-id-123")
                .build();

        String eventId = HandleEventUtils.getEventId(message);

        assertEquals("corr-id-123", eventId);
    }

    @Test
    void getEventId_shouldReturnNullWhenMissing() {
        NotificationCostInitializationEvent.Payload payload =
                NotificationCostInitializationEvent.Payload.builder()
                        .iun("IUN-NO-EVENT")
                        .build();

        Message<InternalEvent> message = MessageBuilder
                .withPayload((InternalEvent) payload)
                .build();

        String eventId = HandleEventUtils.getEventId(message);

        assertNull(eventId);
    }

    @Test
    void addIunAndCorrIdToMdc_shouldPopulateBothValues() {
        HandleEventUtils.addIunAndCorrIdToMdc("IUN-MDC-123", "REQ-456");

        assertEquals("IUN-MDC-123", MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        assertEquals("REQ-456", MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void addIunToMdc_shouldPopulateOnlyIun() {
        HandleEventUtils.addIunToMdc("IUN-ONLY");

        assertEquals("IUN-ONLY", MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void addCorrelationIdToMdc_shouldPopulateOnlyCorrelationId() {
        HandleEventUtils.addCorrelationIdToMdc("REQ-ONLY");

        assertEquals("REQ-ONLY", MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
        assertNull(MDC.get(MDCUtils.MDC_PN_IUN_KEY));
    }

    @Test
    void handleException_shouldNotThrowWhenMessageContainsIunInHeader() {
        Message<?> message = MessageBuilder.withPayload("payload")
                .setHeader(StandardEventHeader.PN_EVENT_HEADER_IUN, "IUN-FROM-HEADER")
                .build();

        assertDoesNotThrow(() ->
                HandleEventUtils.handleException(message, new RuntimeException("boom"))
        );
    }

    @Test
    void handleException_shouldNotThrowWhenIunIsTakenFromPayload() {
        NotificationCostInitializationEvent.Payload payload =
                NotificationCostInitializationEvent.Payload.builder()
                        .iun("IUN-FROM-PAYLOAD")
                        .build();

        Message<?> message = MessageBuilder.withPayload(payload).build();

        assertDoesNotThrow(() ->
                HandleEventUtils.handleException(message, new RuntimeException("boom"))
        );
    }

    @Test
    void handleException_shouldNotThrowWhenIunIsTakenFromMdcFallback() {
        MDC.put(MDCUtils.MDC_PN_IUN_KEY, "IUN-FROM-MDC");

        Message<?> message = MessageBuilder.withPayload("generic-payload").build();

        assertDoesNotThrow(() ->
                HandleEventUtils.handleException(message, new RuntimeException("boom"))
        );
    }

    @Test
    void handleException_shouldNotThrowWhenMessageIsNull() {
        assertDoesNotThrow(() ->
                HandleEventUtils.handleException(null, new RuntimeException("boom"))
        );
    }
}
