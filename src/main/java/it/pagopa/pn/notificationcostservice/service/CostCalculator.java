package it.pagopa.pn.notificationcostservice.service;

import it.pagopa.pn.notificationcostservice.model.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;

public interface CostCalculator {
    CalculatedCosts calculateCosts(NotificationDeliveryCost notificationDeliveryCost);
    CalculatedCosts calculateCosts(NotificationDeliveryCost notificationDeliveryCost, boolean applyCost);

}
