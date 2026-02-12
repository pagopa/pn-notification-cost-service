package it.pagopa.pn.notificationcostservice.utils;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
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
        Integer baseCost = 100;
        Integer firstAnalogCost = 200;
        Integer secondAnalogCost = 300;
        Integer simpleRegisteredLetterCost = 150;
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        // Calcolo atteso: baseCost + (firstAnalog + 22%) + (secondAnalog + 22%) + (simpleRegistered + 22%)
        // 100 + (200*1.22) + (300*1.22) + (150*1.22) = 100 + 244 + 366 + 183 = 893
        Integer expectedTotalCost = 893;

        Integer totalCost = CostUtils.getTotalCost(baseCost, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(expectedTotalCost, totalCost);
    }

    @Test
    void getTotalCostWithFlatRatePolicy() {
        // Test con policy FLAT_RATE - deve restituire 0
        Integer baseCost = 100;
        Integer firstAnalogCost = 200;
        Integer secondAnalogCost = 300;
        Integer simpleRegisteredLetterCost = 150;
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.FLAT_RATE;

        Integer totalCost = CostUtils.getTotalCost(baseCost, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(0, totalCost);
    }

    @Test
    void getTotalCostWithNullBaseCost() {
        // Test con baseCost null - deve restituire 0
        Integer baseCost = null;
        Integer firstAnalogCost = 200;
        Integer secondAnalogCost = 300;
        Integer simpleRegisteredLetterCost = 150;
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        Integer totalCost = CostUtils.getTotalCost(baseCost, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(0, totalCost);
    }

    @Test
    void getTotalCostWithNullAnalogCosts() {
        // Test con costi analogici null - devono essere trattati come 0
        Integer baseCost = 100;
        Integer firstAnalogCost = null;
        Integer secondAnalogCost = null;
        Integer simpleRegisteredLetterCost = null;
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        // Calcolo atteso: solo baseCost = 100
        Integer expectedTotalCost = 100;

        Integer totalCost = CostUtils.getTotalCost(baseCost, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(expectedTotalCost, totalCost);
    }

    @Test
    void getTotalCostWithPartialNullCosts() {
        // Test con alcuni costi null
        Integer baseCost = 100;
        Integer firstAnalogCost = 200;
        Integer secondAnalogCost = null;
        Integer simpleRegisteredLetterCost = 150;
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        // Calcolo atteso: 100 + (200*1.22) + 0 + (150*1.22) = 100 + 244 + 0 + 183 = 527
        Integer expectedTotalCost = 527;

        Integer totalCost = CostUtils.getTotalCost(baseCost, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(expectedTotalCost, totalCost);
    }

    @Test
    void getTotalCostWithZeroVat() {
        // Test con IVA al 0%
        Integer baseCost = 100;
        Integer firstAnalogCost = 200;
        Integer secondAnalogCost = 300;
        Integer simpleRegisteredLetterCost = 150;
        Integer vat = 0;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        // Calcolo atteso: 100 + 200 + 300 + 150 = 750
        Integer expectedTotalCost = 750;

        Integer totalCost = CostUtils.getTotalCost(baseCost, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(expectedTotalCost, totalCost);
    }

    @Test
    void getTotalCostWithRounding() {
        // Test per verificare l'arrotondamento
        Integer baseCost = 100;
        Integer firstAnalogCost = 436; // con 22% IVA = 532 (arrotondato)
        Integer secondAnalogCost = 397; // con 22% IVA = 484 (arrotondato)
        Integer simpleRegisteredLetterCost = 969; // con 22% IVA = 1182 (arrotondato)
        Integer vat = 22;
        NotificationFeePolicy policy = NotificationFeePolicy.DELIVERY_MODE;

        // Calcolo atteso: 100 + 532 + 484 + 1182 = 2298
        Integer expectedTotalCost = 2298;

        Integer totalCost = CostUtils.getTotalCost(baseCost, firstAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat, policy);

        Assertions.assertEquals(expectedTotalCost, totalCost);
    }
}