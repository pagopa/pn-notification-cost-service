package it.pagopa.pn.notificationcostservice.utils;

import org.junit.jupiter.api.Assertions;
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
    })
    void getCostWithVatTest(Integer cost, Integer vat, Integer expectedCostWithVat) {

        Integer costWithVat = CostUtils.getCostWithVat(cost, vat);

        Assertions.assertEquals(expectedCostWithVat, costWithVat);
    }
}