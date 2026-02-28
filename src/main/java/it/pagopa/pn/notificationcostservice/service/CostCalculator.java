package it.pagopa.pn.notificationcostservice.service;

import it.pagopa.pn.notificationcostservice.dto.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;

public interface CostCalculator {
    CalculatedCosts calculateCosts(NotificationDeliveryCostDto notificationDeliveryCostDto);
}
