package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost;

import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleRegisteredLetterCostDtoTest {

    @Test
    void buildsSuccessfullyWithAllFields() {
        SimpleRegisteredLetterCostDto dto = SimpleRegisteredLetterCostDto.builder()
                .cost(100)
                .productType("RS")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("RS", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithZeroCost() {
        SimpleRegisteredLetterCostDto dto = SimpleRegisteredLetterCostDto.builder()
                .cost(0)
                .productType("RS")
                .build();

        assertEquals(0, dto.getCost());
        assertEquals("RS", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithNullProductType() {
        SimpleRegisteredLetterCostDto dto = SimpleRegisteredLetterCostDto.builder()
                .cost(100)
                .productType(null)
                .build();

        assertEquals(100, dto.getCost());
        assertNull(dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithEmptyProductType() {
        SimpleRegisteredLetterCostDto dto = SimpleRegisteredLetterCostDto.builder()
                .cost(100)
                .productType("")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("", dto.getProductType());
    }

    @Test
    void throwsExceptionWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                SimpleRegisteredLetterCostDto.builder()
                        .cost(null)
                        .productType("RS")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                SimpleRegisteredLetterCostDto.builder()
                        .cost(-1)
                        .productType("RS")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void createsSuccessfullyUsingConstructorWithValidCost() {
        SimpleRegisteredLetterCostDto dto = new SimpleRegisteredLetterCostDto(200, "RS");

        assertEquals(200, dto.getCost());
        assertEquals("RS", dto.getProductType());
    }

    @Test
    void throwsExceptionUsingConstructorWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new SimpleRegisteredLetterCostDto(null, "RS")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionUsingConstructorWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new SimpleRegisteredLetterCostDto(-50, "RS")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }
}

