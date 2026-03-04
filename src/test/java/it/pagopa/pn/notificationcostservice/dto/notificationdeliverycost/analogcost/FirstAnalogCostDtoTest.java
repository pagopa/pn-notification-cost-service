package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost;

import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FirstAnalogCostDtoTest {

    @Test
    void buildsSuccessfullyWithAllFields() {
        FirstAnalogCostDto dto = FirstAnalogCostDto.builder()
                .cost(100)
                .productType("AR")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("AR", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithZeroCost() {
        FirstAnalogCostDto dto = FirstAnalogCostDto.builder()
                .cost(0)
                .productType("AR")
                .build();

        assertEquals(0, dto.getCost());
        assertEquals("AR", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithNullProductType() {
        FirstAnalogCostDto dto = FirstAnalogCostDto.builder()
                .cost(100)
                .productType(null)
                .build();

        assertEquals(100, dto.getCost());
        assertNull(dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithEmptyProductType() {
        FirstAnalogCostDto dto = FirstAnalogCostDto.builder()
                .cost(100)
                .productType("")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("", dto.getProductType());
    }

    @Test
    void throwsExceptionWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                FirstAnalogCostDto.builder()
                        .cost(null)
                        .productType("AR")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                FirstAnalogCostDto.builder()
                        .cost(-1)
                        .productType("AR")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }


    @Test
    void createsSuccessfullyUsingConstructorWithValidCost() {
        FirstAnalogCostDto dto = new FirstAnalogCostDto(200, "890");

        assertEquals(200, dto.getCost());
        assertEquals("890", dto.getProductType());
    }
}