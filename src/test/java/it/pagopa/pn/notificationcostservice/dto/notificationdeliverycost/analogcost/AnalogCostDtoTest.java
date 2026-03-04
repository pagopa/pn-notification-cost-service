package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost;

import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalogCostDtoTest {

    @Test
    void createsSuccessfullyWithZeroCost() {
        AnalogCostDto dto = new AnalogCostDto(0, "AR");

        assertEquals(0, dto.getCost());
        assertEquals("AR", dto.getProductType());
    }

    @Test
    void createsSuccessfullyWithPositiveCost() {
        AnalogCostDto dto = new AnalogCostDto(100, "890");

        assertEquals(100, dto.getCost());
        assertEquals("890", dto.getProductType());
    }

    @Test
    void createsSuccessfullyWithNullProductType() {
        AnalogCostDto dto = new AnalogCostDto(100, null);

        assertEquals(100, dto.getCost());
        assertNull(dto.getProductType());
    }

    @Test
    void createsSuccessfullyWithEmptyProductType() {
        AnalogCostDto dto = new AnalogCostDto(100, "");

        assertEquals(100, dto.getCost());
        assertEquals("", dto.getProductType());
    }

    @Test
    void throwsExceptionWhenCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new AnalogCostDto(null, "AR")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }

    @Test
    void throwsExceptionWhenCostIsNegative() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                new AnalogCostDto(-1, "AR")
        );

        assertTrue(exception.getMessage().contains("cost"));
    }
}