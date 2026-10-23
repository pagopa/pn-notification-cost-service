package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.router.impl;

import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.notificationcostservice.exception.PnEventRouterException;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.UpdateNotificationCostEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.NoticePaymentEventHandler;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost.NotificationCostInitializationEventHandler;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost.UpdateNotificationCostEventHandler;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.router.InternalEventsRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONCOSTSERVICE_ROUTER_EVENT_TYPE_MISSING;
import static it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.utils.HandleEventUtils.getEventId;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalEventsRouterImpl implements InternalEventsRouter {

    private final NotificationCostInitializationEventHandler notificationCostInitializationEventHandler;
    private final UpdateNotificationCostEventHandler updateNotificationCostEventHandler;
    private final NoticePaymentEventHandler noticePaymentEventHandler;

    @Override
    public Mono<Void> handleEvent(Message<InternalEvent> message) {
        log.info("Start to handle message for messageId:{}", message.getHeaders().getId());
        InternalEvent payload = message.getPayload();

        switch (payload) {
            case NotificationCostInitializationEvent.Payload notificationPayload -> {
                log.info("Routing message with event type: {}", payload.getEventType());
                String eventId = getEventId(message);
                HandleEventUtils.addIunAndCorrIdToMdc(notificationPayload.getIun(), eventId);
                return notificationCostInitializationEventHandler
                        .handleNotificationCostInitializationEvent(notificationPayload);
            }
            case UpdateNotificationCostEvent.Payload notificationPayload -> {
                log.info("Routing message with event type: {}", payload.getEventType());
                String eventId = getEventId(message);
                HandleEventUtils.addIunAndCorrIdToMdc(notificationPayload.getIun(), eventId);
                return updateNotificationCostEventHandler
                        .handleUpdateNotificationCostEvent(notificationPayload);
            }
            case PnNoticePaymentEvent.Payload paymentPayload -> {
                log.info("Routing noticePaymentEvent message");
                String eventId = getEventId(message);
                HandleEventUtils.addIunAndCorrIdToMdc(paymentPayload.getIuv(), eventId);
                return noticePaymentEventHandler
                        .handleNoticePaymentEvent(paymentPayload);
            }
            default -> log.info("Unrecognized Payload type, unable to route.");
        }

        return Mono.error(new PnEventRouterException(
                String.format("Unsupported internal event payload type or unexpected eventType: %s", payload.getEventType()),
                ERROR_CODE_NOTIFICATIONCOSTSERVICE_ROUTER_EVENT_TYPE_MISSING));
    }

}
