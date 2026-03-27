package it.pagopa.pn.notificationcostservice.service;

import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import reactor.core.publisher.Mono;

import java.util.List;


public interface NotificationCostUpdaterService {
    Mono<Void> updateCostByPhase(CostUpdatePhaseInt updateCostPhase, List<NotificationDeliveryCost> notificationDeliveryCosts);
}
