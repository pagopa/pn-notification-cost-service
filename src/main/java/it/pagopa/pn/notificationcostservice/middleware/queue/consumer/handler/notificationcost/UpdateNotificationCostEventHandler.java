package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.UpdateNotificationCostEvent;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR;
import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpdateNotificationCostEventHandler {

    private final NotificationCostUpdaterService notificationCostUpdaterService;
    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final PaymentInfoDao paymentInfoDao;

    public Mono<Void> handleUpdateNotificationCostEvent(UpdateNotificationCostEvent.Payload payload) {
        log.info("Handling UpdateNotificationCostEvent for iun={}", payload.getIun());

        return validateUpdateNotificationCostEvent(payload)
                .flatMapMany(this::checkForRefusedOrCancelled)
                .flatMap(notificationCostUpdaterService::updateCostByPhase)
                .then()
                .doOnError(ex ->
                        log.error("Error processing UpdateNotificationCostEvent for iun={}", payload.getIun(), ex)
                );
    }

    private Mono<UpdateNotificationCostEvent.Payload> validateUpdateNotificationCostEvent(UpdateNotificationCostEvent.Payload payload) {
        CostUpdatePhaseInt phase = payload.getCostUpdatePhase();

        if (Objects.isNull(payload.getIun())) {
            return Mono.error(new PnInternalException("Missing required field 'iun'",
                    ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR));
        }

        if (Objects.isNull(phase)) {
            return Mono.error(new PnInternalException("Missing required field 'costUpdatePhase' for iun= " + payload.getIun(),
                    ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR));
        }
        
        if (phase == CostUpdatePhaseInt.VALIDATION) {
            return Mono.error(new PnInternalException("Error update operation for VALIDATION phase is not possible, iun=" + payload.getIun(),
                    ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR));
        }

        switch (phase) {
            case SEND_SIMPLE_REGISTERED_LETTER, SEND_ANALOG_DOMICILE_ATTEMPT_0, SEND_ANALOG_DOMICILE_ATTEMPT_1 -> {
                log.info("Mapping notification delivery cost for phase: {} ", phase);
                if(Objects.isNull(payload.getCost()) || Objects.isNull(payload.getProductType()) || Objects.isNull(payload.getRecIndex())) {
                    return Mono.error(new PnInternalException(String.format("Missing required field for iun = %s, cost = %s, productType = %s, recIndex = %s", payload.getIun(), payload.getCost(), payload.getProductType(), payload.getRecIndex()),
                            ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR));
                }
            }
        }
        
        return Mono.just(payload);
    }

    private Flux<NotificationCostUpdate> checkForRefusedOrCancelled(UpdateNotificationCostEvent.Payload payload) {
        CostUpdatePhaseInt phase = payload.getCostUpdatePhase();

        if (isRefusedOrCancelled(phase)) {
            return processPaymentDeletionAndMapping(payload, phase);
        }

        return createNotificationCostUpdateList(payload, phase);
    }

    private boolean isRefusedOrCancelled(CostUpdatePhaseInt phase) {
        return phase == CostUpdatePhaseInt.NOTIFICATION_CANCELLED ||
                phase == CostUpdatePhaseInt.REQUEST_REFUSED;
    }

    private Flux<NotificationCostUpdate> processPaymentDeletionAndMapping(UpdateNotificationCostEvent.Payload payload, CostUpdatePhaseInt phase) {
        return handlePaymentInfoDeletion(payload)
                .doOnNext(v -> log.info("Handled paymentInfo deletion for iun={}", v.getIun()))
                .flatMapMany(v -> notificationDeliveryCostDao.getAllByIun(v.getIun())
                        .switchIfEmpty(handleEmptyCosts(v.getIun(), phase))
                        .map(entity -> this.mapToDeletedNotificationDeliveryCost(entity, phase))
                );
    }

    private Mono<UpdateNotificationCostEvent.Payload> handlePaymentInfoDeletion(UpdateNotificationCostEvent.Payload payload) {
        return paymentInfoDao.deleteItemsByIun(payload.getIun())
                .thenReturn(payload);
    }

    private <T> Flux<T> handleEmptyCosts(String iun, CostUpdatePhaseInt phase) {
        if (phase == CostUpdatePhaseInt.NOTIFICATION_CANCELLED) {
            return Flux.error(new PnInternalException(
                    "Entities not found for iun = " + iun,
                    ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND));
        }
        log.debug("No notification delivery cost entities found for iun = {}, no updates will be performed", iun);
        return Flux.empty();
    }

    private Flux<NotificationCostUpdate> createNotificationCostUpdateList(UpdateNotificationCostEvent.Payload payload, CostUpdatePhaseInt phase) {
        return Flux.fromIterable(List.of(NotificationCostUpdate.builder()
                .iun(payload.getIun())
                .recIndex(payload.getRecIndex())
                .cost(payload.getCost())
                .productType(payload.getProductType())
                .costUpdatePhase(phase)
                .build()));
    }

    private NotificationCostUpdate mapToDeletedNotificationDeliveryCost(NotificationDeliveryCostEntity entity, CostUpdatePhaseInt phase) {
        return NotificationCostUpdate.builder()
                .iun(entity.getIun())
                .recIndex(entity.getRecIndex())
                .costUpdatePhase(phase)
                .build();
    }

}
