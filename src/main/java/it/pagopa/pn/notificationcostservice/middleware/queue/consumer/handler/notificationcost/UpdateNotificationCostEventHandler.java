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
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpdateNotificationCostEventHandler {

    private final NotificationCostUpdaterService  notificationCostUpdaterService;
    private final NotificationDeliveryCostDao notificationDeliveryCostDao;

    public Mono<Void> handleUpdateNotificationCostEvent(UpdateNotificationCostEvent.Payload payload) {
        log.info("Handling UpdateNotificationCostEvent for iun={}", payload.getIun());

        if (Objects.isNull(payload.getCostUpdatePhase())) {
            log.error(
                    "Skipping UpdateNotificationCostEvent for iun={} because costUpdatePhase is null",
                    payload.getIun());
            return Mono.error(new PnInternalException("Missing required data for iun: " + payload.getIun(),
                    ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR));
        }

        log.info("Start processing UpdateNotificationCostEvent for iun={}", payload.getIun());

        CostUpdatePhaseInt phase = payload.getCostUpdatePhase();

        if (phase == CostUpdatePhaseInt.VALIDATION) {
            log.info("Skipping update for VALIDATION phase, iun={}", payload.getIun());
            return Mono.empty();
        }

        return checkForRefusedOrCancelled(payload)
                .flatMapMany(Flux::fromIterable)
                .flatMap(notificationCostUpdaterService::updateCostByPhase)
                .then()
                .doOnError(ex ->
                        log.error("Error processing UpdateNotificationCostEvent for iun={}", payload.getIun(), ex)
                );
    }

    private Mono<List<NotificationCostUpdate>> checkForRefusedOrCancelled(UpdateNotificationCostEvent.Payload payload) {
        CostUpdatePhaseInt phase = payload.getCostUpdatePhase();

        if (phase == CostUpdatePhaseInt.NOTIFICATION_CANCELLED || phase == CostUpdatePhaseInt.REQUEST_REFUSED) {
            return notificationDeliveryCostDao.getAllByIun(payload.getIun())
                    .flatMap(entity -> this.buildNotCompletedEntities(entity, phase));
        }

        return Mono.just(List.of(NotificationCostUpdate.builder()
                .iun(payload.getIun())
                .recIndex(payload.getRecIndex())
                .cost(payload.getCost())
                .productType(payload.getProductType())
                .costUpdatePhase(phase)
                .build()));
    }

    private Mono<List<NotificationCostUpdate>> buildNotCompletedEntities(Page<NotificationDeliveryCostEntity> notificationDeliveryCostEntityPage, CostUpdatePhaseInt costUpdatePhase) {
        return Mono.just(
                notificationDeliveryCostEntityPage.items()
                        .stream()
                        .map(entity -> this.mapToDeletedNotificationDeliveryCost(entity, costUpdatePhase))
                        .toList()
        );
    }

    private NotificationCostUpdate mapToDeletedNotificationDeliveryCost(NotificationDeliveryCostEntity model, CostUpdatePhaseInt costUpdatePhase) {
        return NotificationCostUpdate.builder()
                .iun(model.getIun())
                .recIndex(model.getRecIndex())
                .costUpdatePhase(costUpdatePhase)
                .cost(0)
                .productType(null)
                .build();
    }

}
