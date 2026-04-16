package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notificationcostservice.model.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.AnalogCost;
import it.pagopa.pn.notificationcostservice.service.CostCalculator;
import it.pagopa.pn.notificationcostservice.utils.CostUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CostCalculatorImpl implements CostCalculator {

    @Override
    public CalculatedCosts calculateCosts(NotificationDeliveryCost notificationDeliveryCost) {
        return this.calculateCosts(notificationDeliveryCost, true);
    }

    @Override
    public CalculatedCosts calculateCosts(NotificationDeliveryCost notificationDeliveryCost, boolean applyCost) {
        log.debug("calculateCosts - notificationDeliveryCost={}, applyCost={}", notificationDeliveryCost, applyCost);
        int vat = notificationDeliveryCost.getVat();
        int baseCost = baseCost(notificationDeliveryCost.getBaseCost());
        int analogCost = analogCost(notificationDeliveryCost);
        int analogCostWithVat = CostUtils.getCostWithVat(analogCost, vat);
        int totalCostWithVat = 0;
        int partialCost = 0;
        if (notificationDeliveryCost.getNotificationFeePolicy() == NotificationFeePolicy.DELIVERY_MODE && applyCost) {
            totalCostWithVat = baseCost + analogCostWithVat;
            partialCost = notificationDeliveryCost.getBaseCost().getSendFee() + analogCost;
        }
        return CalculatedCosts.builder()
                .totalCostWithVat(totalCostWithVat)
                .partialCost(partialCost)
                .baseCost(baseCost)
                .analogCost(analogCost)
                .analogCostWithVat(analogCostWithVat)
                .vat(notificationDeliveryCost.getVat())
                .build();
    }

    /**
     * Calculate the base cost of the notification by summing the PA fee and the send fee.
     * @return sum of the base costs
     */
    public int baseCost(BaseCost baseCost) {
        return baseCost.getPaFee() + baseCost.getSendFee();
    }

    /**
     * Calculate the total analog cost by summing the first analog cost, second analog cost, simple registered letter cost, when they are present.
     * @return sum of the analog costs
     */
    public int analogCost(NotificationDeliveryCost notificationDelivery) {
        return nullSafe(notificationDelivery.getFirstAnalogCost()) + nullSafe(notificationDelivery.getSecondAnalogCost()) + nullSafe(notificationDelivery.getSimpleRegisteredLetterCost());
    }

    private int nullSafe(AnalogCost analog) {
        return analog != null ? analog.getCost() : 0;
    }
}
