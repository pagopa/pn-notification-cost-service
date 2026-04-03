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
import reactor.core.publisher.Mono;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR;

@Slf4j
@AllArgsConstructor
@Service
public class NotificationCostUpdaterServiceImpl implements NotificationCostUpdaterService {

    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final NotificationCostUpdaterMapper notificationCostUpdaterMapper;

    @Override
    public Mono<Void> updateBaseCost(NotificationDeliveryCost notificationDeliveryCost) {
        return Mono.justOrEmpty(notificationDeliveryCost)
                .switchIfEmpty(Mono.error(new PnInternalException("NotificationDeliveryCost cannot be null",
                        ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR)))
                .map(notificationCostUpdaterMapper::toEntityForBaseCostUpdate)
                .flatMap(notificationDeliveryCostDao::updateNotificationDeliveryCostNotNull)
                .doOnNext(entity -> log.info(
                        "Notification delivery cost updated successfully: phase={}, iun={}, recIndex={}",
                        CostUpdatePhaseInt.VALIDATION, // placeholder for future audit log
                        entity.getIun(),
                        entity.getRecIndex()
                ))
                .then();
    }
}
