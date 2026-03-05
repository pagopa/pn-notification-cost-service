package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost;

import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecondAnalogCostDtoTest {

    @Test
    void buildsSuccessfullyWithAllFields() {
        SecondAnalogCostDto dto = SecondAnalogCostDto.builder()
                .cost(100)
                .productType("890")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("890", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithZeroCost() {
        SecondAnalogCostDto dto = SecondAnalogCostDto.builder()
                .cost(0)
                .productType("890")
                .build();

        assertEquals(0, dto.getCost());
        assertEquals("890", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithLargeCost() {
        SecondAnalogCostDto dto = SecondAnalogCostDto.builder()
                .cost(Integer.MAX_VALUE)
                .productType("890")
                .build();

        assertEquals(Integer.MAX_VALUE, dto.getCost());
        assertEquals("890", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithNullProductType() {
        SecondAnalogCostDto dto = SecondAnalogCostDto.builder()
                .cost(100)
                .productType(null)
                .build();

        assertEquals(100, dto.getCost());
        assertNull(dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithEmptyProductType() {
        SecondAnalogCostDto dto = SecondAnalogCostDto.builder()
                .cost(100)
                .productType("")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("", dto.getProductType());
    }

    @Test
    void throwsExceptionWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                SecondAnalogCostDto.builder()
                        .cost(null)
                        .productType("890")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                SecondAnalogCostDto.builder()
                        .cost(-1)
                        .productType("890")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void createsSuccessfullyUsingConstructorWithValidCost() {
        SecondAnalogCostDto dto = new SecondAnalogCostDto(200, "890");

        assertEquals(200, dto.getCost());
        assertEquals("890", dto.getProductType());
    }

    @Test
    void throwsExceptionUsingConstructorWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new SecondAnalogCostDto(null, "890")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionUsingConstructorWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new SecondAnalogCostDto(-50, "890")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }
}

