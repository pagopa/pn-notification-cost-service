package it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost;

import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecondAnalogCostTest {

    @Test
    void buildsSuccessfullyWithAllFields() {
        SecondAnalogCost dto = SecondAnalogCost.builder()
                .cost(100)
                .productType("890")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("890", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithZeroCost() {
        SecondAnalogCost dto = SecondAnalogCost.builder()
                .cost(0)
                .productType("890")
                .build();

        assertEquals(0, dto.getCost());
        assertEquals("890", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithLargeCost() {
        SecondAnalogCost dto = SecondAnalogCost.builder()
                .cost(Integer.MAX_VALUE)
                .productType("890")
                .build();

        assertEquals(Integer.MAX_VALUE, dto.getCost());
        assertEquals("890", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithNullProductType() {
        SecondAnalogCost dto = SecondAnalogCost.builder()
                .cost(100)
                .productType(null)
                .build();

        assertEquals(100, dto.getCost());
        assertNull(dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithEmptyProductType() {
        SecondAnalogCost dto = SecondAnalogCost.builder()
                .cost(100)
                .productType("")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("", dto.getProductType());
    }

    @Test
    void throwsExceptionWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                SecondAnalogCost.builder()
                        .cost(null)
                        .productType("890")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                SecondAnalogCost.builder()
                        .cost(-1)
                        .productType("890")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void createsSuccessfullyUsingConstructorWithValidCost() {
        SecondAnalogCost dto = new SecondAnalogCost(200, "890");

        assertEquals(200, dto.getCost());
        assertEquals("890", dto.getProductType());
    }

    @Test
    void throwsExceptionUsingConstructorWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new SecondAnalogCost(null, "890")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionUsingConstructorWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new SecondAnalogCost(-50, "890")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }
}

