package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;
import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONCOSTSERVICE_HANDLE_EVENT_FAILED;

@Slf4j
public class HandleEventUtils {
    private HandleEventUtils() {}

    public static void handleException(Message<?> message, Exception ex) {
        if (message != null) {
            String iun = extractIun(message);
            log.error("Generic exception for iun={}", iun, ex);
        } else {
            log.error("Generic exception", ex);
        }
    }

    private static String extractIun(Message<?> message) {
        MessageHeaders headers = message.getHeaders();

        Object iunFromHeader = headers.get(PN_EVENT_HEADER_IUN);
        if (iunFromHeader instanceof String iun && !iun.isBlank()) {
            return iun;
        }

        Object payload = message.getPayload();
        if (payload instanceof NotificationCostInitializationEvent.Payload notificationPayload) {
            return notificationPayload.getIun();
        }

        String iunFromMdc = MDC.get(MDCUtils.MDC_PN_IUN_KEY);
        if (iunFromMdc != null && !iunFromMdc.isBlank()) {
            return iunFromMdc;
        }

        return null;
    }

    public static StandardEventHeader mapStandardEventHeader(MessageHeaders headers) {
        if(headers != null){
            return StandardEventHeader.builder()
                    .eventId((String) headers.get(PN_EVENT_HEADER_EVENT_ID))
                    .iun((String) headers.get(PN_EVENT_HEADER_IUN))
                    .eventType((String) headers.get(PN_EVENT_HEADER_EVENT_TYPE))
                    .createdAt(mapInstant(headers.get(PN_EVENT_HEADER_CREATED_AT)))
                    .publisher((String) headers.get(PN_EVENT_HEADER_PUBLISHER))
                    .build();
        } else {
            String msg = "Headers cannot be null in mapStandardEventHeader";
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_NOTIFICATIONCOSTSERVICE_HANDLE_EVENT_FAILED);
        }
    }

    private static Instant mapInstant(Object createdAt) {
        return createdAt != null ? Instant.parse((CharSequence) createdAt) : null;
    }

    @Nullable
    public static String getEventId(Message<InternalEvent> message) {
        MessageHeaders messageHeaders = message.getHeaders();
        String eventId = null;
        if (messageHeaders.containsKey("eventId")){
            eventId = messageHeaders.get("eventId", String.class);
        }
        return eventId;
    }

    public static void addIunAndCorrIdToMdc(String iun,String correlationId) {
        addIunToMdc(iun);
        addCorrelationIdToMdc(correlationId);
    }

    public static void addIunToMdc(String iun) {
        MDC.put(MDCUtils.MDC_PN_IUN_KEY, iun);
    }

    public static void addCorrelationIdToMdc(String correlationId) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, correlationId);
    }
}
