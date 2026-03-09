package it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost;

import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleRegisteredLetterCostDtoTest {

    @Test
    void buildsSuccessfullyWithAllFields() {
        SimpleRegisteredLetterCost dto = SimpleRegisteredLetterCost.builder()
                .cost(100)
                .productType("RS")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("RS", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithZeroCost() {
        SimpleRegisteredLetterCost dto = SimpleRegisteredLetterCost.builder()
                .cost(0)
                .productType("RS")
                .build();

        assertEquals(0, dto.getCost());
        assertEquals("RS", dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithNullProductType() {
        SimpleRegisteredLetterCost dto = SimpleRegisteredLetterCost.builder()
                .cost(100)
                .productType(null)
                .build();

        assertEquals(100, dto.getCost());
        assertNull(dto.getProductType());
    }

    @Test
    void buildsSuccessfullyWithEmptyProductType() {
        SimpleRegisteredLetterCost dto = SimpleRegisteredLetterCost.builder()
                .cost(100)
                .productType("")
                .build();

        assertEquals(100, dto.getCost());
        assertEquals("", dto.getProductType());
    }

    @Test
    void throwsExceptionWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                SimpleRegisteredLetterCost.builder()
                        .cost(null)
                        .productType("RS")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                SimpleRegisteredLetterCost.builder()
                        .cost(-1)
                        .productType("RS")
                        .build()
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void createsSuccessfullyUsingConstructorWithValidCost() {
        SimpleRegisteredLetterCost dto = new SimpleRegisteredLetterCost(200, "RS");

        assertEquals(200, dto.getCost());
        assertEquals("RS", dto.getProductType());
    }

    @Test
    void throwsExceptionUsingConstructorWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new SimpleRegisteredLetterCost(null, "RS")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionUsingConstructorWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new SimpleRegisteredLetterCost(-50, "RS")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }
}

