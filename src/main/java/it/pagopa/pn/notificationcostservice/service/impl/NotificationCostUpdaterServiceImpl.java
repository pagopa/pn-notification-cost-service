package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationCostUpdaterMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR;

@Slf4j
@AllArgsConstructor
@Service
public class NotificationCostUpdaterServiceImpl implements NotificationCostUpdaterService {

    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final NotificationCostUpdaterMapper notificationCostUpdaterMapper;

    @Override
    public Mono<Void> updateCostByPhase(CostUpdatePhaseInt updateCostPhase,
                                        List<NotificationDeliveryCost> notificationDeliveryCosts) {
        if (notificationDeliveryCosts == null || notificationDeliveryCosts.isEmpty()) {
            log.error("Skipping notification delivery cost update: phase={}, no items to process", updateCostPhase);
            return Mono.error(new PnInternalException("Missing required data " + notificationDeliveryCosts + " for phase: " + updateCostPhase,
                    ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR));
        }
        return Flux.fromIterable(notificationDeliveryCosts)
                .map(notificationDeliveryCost ->
                        notificationCostUpdaterMapper.mapNotificationCostUpdater(updateCostPhase, notificationDeliveryCost)
                )
                .flatMap(notificationDeliveryCostDao::updateNotificationDeliveryCostNotNull)
                .doOnNext(entity -> log.info(
                        "Notification delivery cost updated successfully: phase={}, iun={}, recIndex={}",
                        updateCostPhase,
                        entity.getIun(),
                        entity.getRecIndex()
                ))
                .then();
    }
}
