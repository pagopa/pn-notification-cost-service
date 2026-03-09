package it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost;

import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FirstAnalogCostTest {

    @Test
    void buildsSuccessfullyWithAllFields() {
        FirstAnalogCost dto = FirstAnalogCost.builder()
                .cost(100)
                .productType("AR")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("AR", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithZeroCost() {
        FirstAnalogCost dto = FirstAnalogCost.builder()
                .cost(0)
                .productType("AR")
                .build();

        assertEquals(0, dto.getCost());
        assertEquals("AR", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithNullProductType() {
        FirstAnalogCost dto = FirstAnalogCost.builder()
                .cost(100)
                .productType(null)
                .build();

        assertEquals(100, dto.getCost());
        assertNull(dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithEmptyProductType() {
        FirstAnalogCost dto = FirstAnalogCost.builder()
                .cost(100)
                .productType("")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("", dto.getProductType());
    }

    @Test
    void throwsExceptionWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                FirstAnalogCost.builder()
                        .cost(null)
                        .productType("AR")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                FirstAnalogCost.builder()
                        .cost(-1)
                        .productType("AR")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }


    @Test
    void createsSuccessfullyUsingConstructorWithValidCost() {
        FirstAnalogCost dto = new FirstAnalogCost(200, "890");

        assertEquals(200, dto.getCost());
        assertEquals("890", dto.getProductType());
    }
}