package it.pagopa.pn.notificationcostservice.utils;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CostUtilsTest {

    @ParameterizedTest
    @CsvSource(value = {
            "436, 22, 532", // Round Up Test
            "397, 22, 484", // Round Down Test
            "1000, 22, 1220", // 22% VAT
            "1500, 10, 1650", // 10% VAT
            "1500, 0, 1500", // Test with 0% VAT
            "1500, NULL, 0", // Test with null VAT
            "NULL, 22, 0" // Test with null cost
    }, nullValues = {"NULL"})
    void getCostWithVatTest(Integer cost, Integer vat, Integer expectedCostWithVat) {

        Integer costWithVat = CostUtils.getCostWithVat(cost, vat);

        Assertions.assertEquals(expectedCostWithVat, costWithVat);
    }

    @Test
    void getTotalCostWithDeliveryModePolicy() {
        // Test con policy DELIVERY_MODE - calcola il costo totale con IVA applicata
        BaseCostDto baseCostDto = BaseCostDto.builder()
                .sendFee(50)
                .paFee(50)
                .build();
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder()
                .cost(200)
                .build();
        SecondAnalogCostDto secondAnalogCost = SecondAnalogCostDto.builder()
                .cost(300)
                .build();
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = SimpleRegisteredLetterCostDto.builder()
                .cost(150)
                .build();
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        // Calcolo atteso: baseCost(50+50) + (firstAnalog + 22%) + (secondAnalog + 22%) + (simpleRegistered + 22%)
        // 100 + (200*1.22) + (300*1.22) + (150*1.22) = 100 + 244 + 366 + 183 = 893
        Integer expectedTotalCost = 893;

        Integer totalCost = CostUtils.getTotalCost(baseCostDto, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(expectedTotalCost, totalCost);
    }

    @Test
    void getTotalCostWithFlatRatePolicy() {
        // Test con policy FLAT_RATE - deve restituire 0
        BaseCostDto baseCostDto = BaseCostDto.builder()
                .sendFee(50)
                .paFee(50)
                .build();
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder()
                .cost(200)
                .build();
        SecondAnalogCostDto secondAnalogCost = SecondAnalogCostDto.builder()
                .cost(300)
                .build();
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = SimpleRegisteredLetterCostDto.builder()
                .cost(150)
                .build();
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.FLAT_RATE;

        Integer totalCost = CostUtils.getTotalCost(baseCostDto, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(0, totalCost);
    }

    @Test
    void getTotalCostWithNullBaseCost() {
        // Test con baseCost null - deve lanciare un'eccezione
        BaseCostDto baseCostDto = null;
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder()
                .cost(200)
                .build();
        SecondAnalogCostDto secondAnalogCost = SecondAnalogCostDto.builder()
                .cost(300)
                .build();
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = SimpleRegisteredLetterCostDto.builder()
                .cost(150)
                .build();
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        Assertions.assertThrows(PnNotFoundException.class, () ->
            CostUtils.getTotalCost(baseCostDto, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy)
        );
    }

    @Test
    void getTotalCostWithNullAnalogCosts() {
        // Test con costi analogici null - deve calcolare solo il base cost
        BaseCostDto baseCostDto = BaseCostDto.builder()
                .sendFee(50)
                .paFee(50)
                .build();
        FirstAnalogCostDto firstAnalogCost = null;
        SecondAnalogCostDto secondAnalogCost = null;
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = null;
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        Integer totalCost = CostUtils.getTotalCost(baseCostDto, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(100, totalCost);
    }

    @Test
    void getTotalCostWithPartialNullCosts() {
        // Test con alcuni costi null
        BaseCostDto baseCostDto = BaseCostDto.builder()
                .sendFee(50)
                .paFee(50)
                .build();
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder()
                .cost(200)
                .build();
        SecondAnalogCostDto secondAnalogCost = null;
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = SimpleRegisteredLetterCostDto.builder()
                .cost(150)
                .build();
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        Integer totalCost = CostUtils.getTotalCost(baseCostDto, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(527, totalCost);
    }

    @Test
    void getTotalCostWithZeroVat() {
        // Test con IVA al 0%
        BaseCostDto baseCostDto = BaseCostDto.builder()
                .sendFee(50)
                .paFee(50)
                .build();
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder()
                .cost(200)
                .build();
        SecondAnalogCostDto secondAnalogCost = SecondAnalogCostDto.builder()
                .cost(300)
                .build();
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = SimpleRegisteredLetterCostDto.builder()
                .cost(150)
                .build();
        Integer vat = 0;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        // Calcolo atteso: 100 + 200 + 300 + 150 = 750
        Integer expectedTotalCost = 750;

        Integer totalCost = CostUtils.getTotalCost(baseCostDto, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(expectedTotalCost, totalCost);
    }

    @Test
    void getTotalCostWithRounding() {
        // Test per verificare l'arrotondamento
        BaseCostDto baseCostDto = BaseCostDto.builder()
                .sendFee(50)
                .paFee(50)
                .build();
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder()
                .cost(436) // con 22% IVA = 532 (arrotondato)
                .build();
        SecondAnalogCostDto secondAnalogCost = SecondAnalogCostDto.builder()
                .cost(397) // con 22% IVA = 484 (arrotondato)
                .build();
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = SimpleRegisteredLetterCostDto.builder()
                .cost(969) // con 22% IVA = 1182 (arrotondato)
                .build();
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        // Calcolo atteso: 100 + 532 + 484 + 1182 = 2298
        Integer expectedTotalCost = 2298;

        Integer totalCost = CostUtils.getTotalCost(baseCostDto, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(expectedTotalCost, totalCost);
    }

    @Test
    void getTotalCostWithNullBaseFees() {
        // Test con paFee e sendFee null - deve lanciare un'eccezione
        BaseCostDto baseCostDto = BaseCostDto.builder()
                .sendFee(null)
                .paFee(null)
                .build();
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder()
                .cost(200)
                .build();
        SecondAnalogCostDto secondAnalogCost = SecondAnalogCostDto.builder()
                .cost(300)
                .build();
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = SimpleRegisteredLetterCostDto.builder()
                .cost(150)
                .build();
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        Assertions.assertThrows(PnNotFoundException.class, () ->
            CostUtils.getTotalCost(baseCostDto, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy)
        );
    }
}