package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import it.pagopa.pn.notificationcostservice.utils.CostUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CostCalculatorImplTest {
    private final CostCalculatorImpl calculator = new CostCalculatorImpl();

    @Test
    void calculateCosts_ReturnsZeroTotalCostWithVatWhenFeePolicyIsNotDeliveryMode() {
       NotificationDeliveryCostDto dto = new NotificationDeliveryCostDto();
       dto.setVat(22);
       BaseCostDto baseCostDto = new BaseCostDto();
       baseCostDto.setPaFee(100);
       baseCostDto.setSendFee(50);
       dto.setBaseCost(baseCostDto);
       dto.setNotificationFeePolicy(NotificationFeePolicy.FLAT_RATE);

       var result = calculator.calculateCosts(dto);

       assertEquals(0, result.getTotalCostWithVat());
       assertEquals(150, result.getBaseCost());
       assertEquals(0, result.getAnalogCost());
       assertEquals(22, result.getVat());
    }

    @Test
    void calculateCosts_ComputesTotalCostWithVatWhenFeePolicyIsDeliveryMode() {
       NotificationDeliveryCostDto dto = new NotificationDeliveryCostDto();
       dto.setVat(10);
       BaseCostDto baseCostDto = new BaseCostDto();
       baseCostDto.setPaFee(100);
       baseCostDto.setSendFee(50);
       dto.setBaseCost(baseCostDto);
       dto.setFirstAnalogCost(FirstAnalogCostDto.builder().cost(200).build());
       dto.setNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);

       var result = calculator.calculateCosts(dto);

       int expectedAnalogWithVat = CostUtils.getCostWithVat(200, 10);
       assertEquals(150 + expectedAnalogWithVat, result.getTotalCostWithVat());
       assertEquals(expectedAnalogWithVat, result.getAnalogCostWithVat());
       assertEquals(150, result.getBaseCost());
       assertEquals(200, result.getAnalogCost());
       assertEquals(10, result.getVat());
    }

    @Test
    void baseCost_ReturnsZeroWhenPaFeeAndSendFeeAreNull() {
       NotificationDeliveryCostDto dto = new NotificationDeliveryCostDto();
       dto.setBaseCost(new BaseCostDto());

       assertEquals(0, calculator.baseCost(dto.getBaseCost()));
    }

    @Test
    void analogCost_ReturnsSumOfAllAnalogCosts() {
       NotificationDeliveryCostDto dto = new NotificationDeliveryCostDto();
       dto.setFirstAnalogCost(FirstAnalogCostDto.builder().cost(10).build());
       dto.setSecondAnalogCost(SecondAnalogCostDto.builder().cost(20).build());
       dto.setSimpleRegisteredLetterCost(SimpleRegisteredLetterCostDto.builder().cost(30).build());

       assertEquals(60, calculator.analogCost(dto));
    }

    @Test
    void analogCost_ReturnsZeroWhenAllAnalogCostsAreNull() {
       NotificationDeliveryCostDto dto = new NotificationDeliveryCostDto();

       assertEquals(0, calculator.analogCost(dto));
    }
}