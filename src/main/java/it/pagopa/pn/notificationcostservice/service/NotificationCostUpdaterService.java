package it.pagopa.pn.notificationcostservice.service;

import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import reactor.core.publisher.Mono;


public interface NotificationCostUpdaterService {
    Mono<Void> updateBaseCost(NotificationDeliveryCost notificationDeliveryCost);

    Mono<Void> updateCostByPhase(NotificationCostUpdate notificationDeliveryCosts);
}
