package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
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

    public Mono<Void> handleUpdateNotificationCostEvent(UpdateNotificationCostEvent.Payload payload) {
        log.info("Handling UpdateNotificationCostEvent for iun={}", payload.getIun());
        log.info("Start processing UpdateNotificationCostEvent for iun={}", payload.getIun());

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
                log.info("Mapping notification delivery cost for phase: SEND_SIMPLE_REGISTERED_LETTER");
                if(Objects.isNull(payload.getCost()) || Objects.isNull(payload.getProductType()) || Objects.isNull(payload.getRecIndex())) {
                    return Mono.error(new PnInternalException(String.format("Missing required field for iun = %s, cost = %s, productType = %s, recIndex = %s", payload.getIun(), payload.getCost(), payload.getProductType(), payload.getRecIndex()),
                            ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR));
                }
            }
        };
        
        return Mono.just(payload);
    }

    private Flux<NotificationCostUpdate> checkForRefusedOrCancelled(UpdateNotificationCostEvent.Payload payload) {
        CostUpdatePhaseInt phase = payload.getCostUpdatePhase();

        if (phase == CostUpdatePhaseInt.NOTIFICATION_CANCELLED || phase == CostUpdatePhaseInt.REQUEST_REFUSED) {
            return notificationDeliveryCostDao.getAllByIun(payload.getIun())
                    .switchIfEmpty(Mono.error(new PnInternalException("Entities not found for iun = " + payload.getIun(),
                            ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND)))
                    .map(entity -> this.mapToDeletedNotificationDeliveryCost(entity, phase));
        }

        return createNotificationCostUpdateList(payload, phase);
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
