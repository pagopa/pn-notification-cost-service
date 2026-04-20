package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost;

import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.eventbus.EventBridgeProducer;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR;
import static it.pagopa.pn.notificationcostservice.middleware.eventbus.utils.NotificationCostValidationEventBuilder.buildOkValidationEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationCostInitializationEventHandler {

    private final NotificationCostUpdaterService notificationCostUpdaterService;
    private final PaymentInfoDao paymentInfoDao;
    private final EventBridgeProducer<PnNotificationCostValidationEvent> producer;


    public Mono<Void> handleNotificationCostInitializationEvent(NotificationCostInitializationEvent.Payload payload) {
        log.info("Handling NotificationCostInitializationEvent for iun={}", payload.getIun());
        List<NotificationDeliveryCost> notificationCosts = payload.getNotificationCosts();
        List<PaymentInfo> payments = payload.getPayments();

        if (notificationCosts == null || notificationCosts.isEmpty()) {
            log.error(
                    "Skipping NotificationCostInitializationEvent for iun={} because notificationCosts are null/empty.",
                    payload.getIun());
            return Mono.error(new PnInternalException("Missing required data for iun:"+  payload.getIun(),
                    ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR));
        }

        log.info(
                "Start processing NotificationCostInitializationEvent for iun={}.",
                payload.getIun()
        );

        return updateNotificationBaseCosts(notificationCosts)
                .then(Mono.defer(() -> updatePaymentsInfo(payments)))
                .then(Mono.defer(() -> sendOutcomeEvent(payload.getIun())))
                .doOnError(ex ->
                        log.error("Error processing NotificationCostInitializationEvent for iun={}", payload.getIun(), ex)
                );
    }

    private Mono<Void> updateNotificationBaseCosts(List<NotificationDeliveryCost> notificationCosts) {
        return Flux.fromIterable(notificationCosts)
                .flatMap(notificationCostUpdaterService::updateBaseCost)
                .then()
                .doOnSuccess(ignored -> log.info("Successfully saved notification costs for iun={}", notificationCosts.getFirst().getIun()))
                .doOnError(ex -> log.error("Error saving notification costs", ex));
    }

    private Mono<Void> updatePaymentsInfo(List<PaymentInfo> payments) {
        if (payments == null || payments.isEmpty()) {
            log.info("No payments to update");
            return Mono.empty();
        }

        return paymentInfoDao.updateItemIfNotExistsOrMatch(payments)
                .doOnSuccess(ignored -> log.info("Successfully updated payments info for iun={}", payments.getFirst().getIun()))
                .doOnError(ex -> log.error("Error updating payments info", ex));
    }

    private Mono<Void> sendOutcomeEvent(String iun) {
        return producer.sendEvent(buildOkValidationEvent(iun))
                .doOnError(ex -> log.error("Error sending outcome event to EventBridge for iun={}", iun, ex));
    }
}
