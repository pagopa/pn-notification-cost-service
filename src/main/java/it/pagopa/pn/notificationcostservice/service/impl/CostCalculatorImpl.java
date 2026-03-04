package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notificationcostservice.dto.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.AnalogCostDto;
import it.pagopa.pn.notificationcostservice.service.CostCalculator;
import it.pagopa.pn.notificationcostservice.utils.CostUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CostCalculatorImpl implements CostCalculator {

    @Override
    public CalculatedCosts calculateCosts(NotificationDeliveryCostDto notificationDeliveryCostDto) {
        log.debug("calculateCosts - notificationDeliveryCostDto={}", notificationDeliveryCostDto);
        int vat = notificationDeliveryCostDto.getVat();
        int baseCost = baseCost(notificationDeliveryCostDto.getBaseCost());
        int analogCost = analogCost(notificationDeliveryCostDto);
        int analogCostWithVat = CostUtils.getCostWithVat(analogCost, vat);
        int totalCostWithVat = 0;
        if(notificationDeliveryCostDto.getNotificationFeePolicy() == NotificationFeePolicy.DELIVERY_MODE) {
            totalCostWithVat = baseCost + analogCostWithVat;
        }
        return CalculatedCosts.builder()
                .totalCostWithVat(totalCostWithVat)
                .baseCost(baseCost)
                .analogCost(analogCost)
                .analogCostWithVat(analogCostWithVat)
                .vat(notificationDeliveryCostDto.getVat())
                .build();
    }

    /**
     * Calculate the base cost of the notification by summing the PA fee and the send fee.
     * @return sum of the base costs
     */
    public int baseCost(BaseCostDto baseCostDto) {
        return baseCostDto.getPaFee() + baseCostDto.getSendFee();
    }

    /**
     * Calculate the total analog cost by summing the first analog cost, second analog cost, simple registered letter cost, when they are present.
     * @return sum of the analog costs
     */
    public int analogCost(NotificationDeliveryCostDto notificationDelivery) {
        return nullSafe(notificationDelivery.getFirstAnalogCost()) + nullSafe(notificationDelivery.getSecondAnalogCost()) + nullSafe(notificationDelivery.getSimpleRegisteredLetterCost());
    }

    private int nullSafe(AnalogCostDto analog) {
        return analog != null ? analog.getCost() : 0;
    }
}
