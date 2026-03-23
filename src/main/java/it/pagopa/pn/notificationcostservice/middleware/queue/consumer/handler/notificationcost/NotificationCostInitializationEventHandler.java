package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost;

import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEvent;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.eventbus.EventBridgeProducer;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.notificationcostservice.middleware.eventbus.utils.NotificationCostValidationEventBuilder.buildOkValidationEvent;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class NotificationCostInitializationEventHandler {

    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final PaymentInfoDao paymentInfoDao;
    private final EventBridgeProducer<PnNotificationCostValidationEvent> producer;


    public Mono<Void> handleNotificationCostInitializationEvent(NotificationCostInitializationEvent.Payload payload) {
        log.info("Handling NotificationCostInitializationEvent for iun={}", payload.getIun());
        List<NotificationDeliveryCost> notificationCosts = payload.getNotificationCosts();
        List<PaymentInfo> payments = payload.getPayments();

        if (notificationCosts == null || notificationCosts.isEmpty() || payments == null || payments.isEmpty()) {
            log.warn(
                    "Skipping NotificationCostInitializationEvent for iun={} because notificationCosts or payments are null/empty. notificationCostsSize={}, paymentsSize={}",
                    payload.getIun(),
                    notificationCosts != null ? notificationCosts.size() : null,
                    payments != null ? payments.size() : null
            );
            return Mono.empty();
        }

        log.info(
                "Start processing NotificationCostInitializationEvent for iun={}.",
                payload.getIun()
        );

        return saveNotificationCosts(notificationCosts)
                .then(updatePaymentsInfo(payments))
                .then(sendOutcomeEvent(payload.getIun()))
                .doOnError(ex ->
                        log.error("Error processing NotificationCostInitializationEvent for iun={}", payload.getIun(), ex)
                );
    }


    private Mono<Void> sendOutcomeEvent(String iun) {
        return producer.sendEvent(buildOkValidationEvent(iun))
                .doOnError(ex -> log.error("Error sending outcome event to EventBridge for iun={}", iun, ex));
    }

    private Mono<Void> saveNotificationCosts(List<NotificationDeliveryCost> notificationCosts) {
        return notificationDeliveryCostDao.putIfAbsent(notificationCosts)
                .onErrorResume(PnIdConflictException.class, ex -> {
                    log.warn("Conflict while saving notification costs for iun={}", notificationCosts.getFirst().getIun(), ex);
                    return Mono.empty();
                })
                .doOnError(ex -> {
                    if (!(ex instanceof PnIdConflictException)) {
                        log.error("Error saving notification costs", ex);
                    }
                });
    }

    private Mono<Void> updatePaymentsInfo(List<PaymentInfo> payments) {
        return paymentInfoDao.updateItem(payments)
                .doOnError(ex -> log.error("Error updating payments info", ex));
    }

}
