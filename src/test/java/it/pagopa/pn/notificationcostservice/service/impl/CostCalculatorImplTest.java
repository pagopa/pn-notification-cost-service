package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.utils.CostUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CostCalculatorImplTest {
    private final CostCalculatorImpl calculator = new CostCalculatorImpl();

    @Test
    void calculateCosts_ReturnsZeroTotalCostWithVatWhenFeePolicyIsNotDeliveryMode() {
       NotificationDeliveryCost notificationDeliveryCost = NotificationDeliveryCostTestBuilder.builder()
               .withBaseCost(BaseCost.builder().paFee(100).sendFee(50).build())
               .withNotificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
               .withVat(22)
               .build();

       var result = calculator.calculateCosts(notificationDeliveryCost);

       assertEquals(0, result.getTotalCostWithVat());
       assertEquals(150, result.getBaseCost());
       assertEquals(0, result.getAnalogCost());
       assertEquals(22, result.getVat());
    }

    @Test
    void calculateCosts_ComputesTotalCostWithVatWhenFeePolicyIsDeliveryMode() {
        NotificationDeliveryCost notificationDeliveryCost = NotificationDeliveryCostTestBuilder.builder()
                .withBaseCost(BaseCost.builder().paFee(100).sendFee(50).build())
                .withFirstAnalogCost(FirstAnalogCost.builder().cost(200).build())
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withVat(10)
                .build();

       var result = calculator.calculateCosts(notificationDeliveryCost);

       int expectedAnalogWithVat = CostUtils.getCostWithVat(200, 10);
       assertEquals(150 + expectedAnalogWithVat, result.getTotalCostWithVat());
       assertEquals(expectedAnalogWithVat, result.getAnalogCostWithVat());
       assertEquals(150, result.getBaseCost());
       assertEquals(200, result.getAnalogCost());
       assertEquals(10, result.getVat());
    }

    @Test
    void analogCost_ReturnsSumOfAllAnalogCosts() {
        NotificationDeliveryCost notificationDeliveryCost = NotificationDeliveryCostTestBuilder.builder()
                .withBaseCost(BaseCost.builder().paFee(100).sendFee(50).build())
                .withFirstAnalogCost(FirstAnalogCost.builder().cost(40).build())
                .withSecondAnalogCost(SecondAnalogCost.builder().cost(20).build())
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withVat(10)
                .build();

       assertEquals(60, calculator.analogCost(notificationDeliveryCost));
    }

    @Test
    void analogCost_ReturnsZeroWhenAllAnalogCostsAreNull() {
       NotificationDeliveryCost notificationDeliveryCost = NotificationDeliveryCostTestBuilder.builder()
               .withBaseCost(BaseCost.builder().paFee(100).sendFee(50).build())
               .build();

       assertEquals(0, calculator.analogCost(notificationDeliveryCost));
    }
}