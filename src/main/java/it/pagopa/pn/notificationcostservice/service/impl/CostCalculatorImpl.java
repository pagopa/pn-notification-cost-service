package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notificationcostservice.dto.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.AnalogCostDto;
import it.pagopa.pn.notificationcostservice.service.CostCalculator;
import it.pagopa.pn.notificationcostservice.utils.CostUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class CostCalculatorImpl implements CostCalculator {

    @Override
    public CalculatedCosts calculateCosts(NotificationDeliveryCostDto notificationDeliveryCostDto) {
        log.debug("calculateCosts - notificationDeliveryCostDto={}", notificationDeliveryCostDto);
        int vat = nullSafe(notificationDeliveryCostDto.getVat());
        int baseCost = baseCost(notificationDeliveryCostDto);
        int analogCost = analogCost(notificationDeliveryCostDto);
        int totalCostWithVat = 0;
        if(notificationDeliveryCostDto.getNotificationFeePolicy() == NotificationFeePolicy.DELIVERY_MODE) {
            int analogCostWithVat = CostUtils.getCostWithVat(analogCost, vat);
            totalCostWithVat = baseCost + analogCostWithVat;
        }
        return CalculatedCosts.builder()
                .totalCostWithVat(totalCostWithVat)
                .baseCost(baseCost)
                .analogCost(analogCost)
                .vat(notificationDeliveryCostDto.getVat())
                .build();
    }

    /**
     * Calculate the base cost of the notification by summing the PA fee and the send fee.
     * @return cost of the base costs
     */
    public int baseCost(NotificationDeliveryCostDto notificationDelivery) {
        return nullSafe(notificationDelivery.getPaFee()) + nullSafe(notificationDelivery.getSendFee());
    }

    /**
     * Calculate the total analog cost by summing the first analog cost, second analog cost, and simple registered letter cost.
     * @return cost of the analog costs
     */
    public int analogCost(NotificationDeliveryCostDto notificationDelivery) {
        return nullSafe(notificationDelivery.getFirstAnalogCost()) + nullSafe(notificationDelivery.getSecondAnalogCost()) + nullSafe(notificationDelivery.getSimpleRegisteredLetterCost());
    }

    private Integer nullSafe(Integer cost) {
        return Optional.ofNullable(cost).orElse(0);
    }

    private Integer nullSafe(AnalogCostDto analog) {
        return analog != null ? nullSafe(analog.getCost()) : 0;
    }
}
