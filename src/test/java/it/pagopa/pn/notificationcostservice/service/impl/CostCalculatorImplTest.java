package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostDtoTestBuilder;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.utils.CostUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CostCalculatorImplTest {
    private final CostCalculatorImpl calculator = new CostCalculatorImpl();

    @Test
    void calculateCosts_ReturnsZeroTotalCostWithVatWhenFeePolicyIsNotDeliveryMode() {
       NotificationDeliveryCostDto dto = NotificationDeliveryCostDtoTestBuilder.builder()
               .withBaseCost(BaseCostDto.builder().paFee(100).sendFee(50).build())
               .withNotificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
               .withVat(22)
               .build();

       var result = calculator.calculateCosts(dto);

       assertEquals(0, result.getTotalCostWithVat());
       assertEquals(150, result.getBaseCost());
       assertEquals(0, result.getAnalogCost());
       assertEquals(22, result.getVat());
    }

    @Test
    void calculateCosts_ComputesTotalCostWithVatWhenFeePolicyIsDeliveryMode() {
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDtoTestBuilder.builder()
                .withBaseCost(BaseCostDto.builder().paFee(100).sendFee(50).build())
                .withFirstAnalogCost(FirstAnalogCostDto.builder().cost(200).build())
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withVat(10)
                .build();

       var result = calculator.calculateCosts(dto);

       int expectedAnalogWithVat = CostUtils.getCostWithVat(200, 10);
       assertEquals(150 + expectedAnalogWithVat, result.getTotalCostWithVat());
       assertEquals(expectedAnalogWithVat, result.getAnalogCostWithVat());
       assertEquals(150, result.getBaseCost());
       assertEquals(200, result.getAnalogCost());
       assertEquals(10, result.getVat());
    }

    @Test
    void analogCost_ReturnsSumOfAllAnalogCosts() {
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDtoTestBuilder.builder()
                .withBaseCost(BaseCostDto.builder().paFee(100).sendFee(50).build())
                .withFirstAnalogCost(FirstAnalogCostDto.builder().cost(40).build())
                .withSecondAnalogCost(SecondAnalogCostDto.builder().cost(20).build())
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withVat(10)
                .build();

       assertEquals(60, calculator.analogCost(dto));
    }

    @Test
    void analogCost_ReturnsZeroWhenAllAnalogCostsAreNull() {
       NotificationDeliveryCostDto dto = NotificationDeliveryCostDtoTestBuilder.builder()
               .withBaseCost(BaseCostDto.builder().paFee(100).sendFee(50).build())
               .build();

       assertEquals(0, calculator.analogCost(dto));
    }
}