package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDeliveryCostDtoTest {

    @Test
    void buildsSuccessfullyWithAllRequiredFields() {
        BaseCostDto baseCost = BaseCostDto.builder().paFee(100).sendFee(50).build();

        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(22)
                .build();

        assertNotNull(dto);
        assertEquals("IUN-TEST-123", dto.getIun());
        assertEquals(0, dto.getRecIndex());
        assertEquals(baseCost, dto.getBaseCost());
        assertEquals(NotificationFeePolicy.DELIVERY_MODE, dto.getNotificationFeePolicy());
        assertEquals(PagoPaIntMode.ASYNC, dto.getPagoPaIntMode());
        assertEquals(22, dto.getVat());
    }

    @Test
    void throwsExceptionWhenIunIsNull() {
        BaseCostDto baseCost = BaseCostDto.builder().paFee(100).sendFee(50).build();

        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCostDto.builder()
                        .iun(null)
                        .recIndex(0)
                        .baseCost(baseCost)
                        .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                        .pagoPaIntMode(PagoPaIntMode.ASYNC)
                        .vat(22)
                        .build()
        );

        assertTrue(exception.getMessage().contains("iun"));
    }

    @Test
    void throwsExceptionWhenBaseCostIsNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCostDto.builder()
                        .iun("IUN-TEST-123")
                        .recIndex(0)
                        .baseCost(null)
                        .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                        .pagoPaIntMode(PagoPaIntMode.ASYNC)
                        .vat(22)
                        .build()
        );

        assertTrue(exception.getMessage().contains("baseCost"));
    }

    @Test
    void throwsExceptionWhenNotificationFeePolicyIsNull() {
        BaseCostDto baseCost = BaseCostDto.builder().paFee(100).sendFee(50).build();

        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCostDto.builder()
                        .iun("IUN-TEST-123")
                        .recIndex(0)
                        .baseCost(baseCost)
                        .notificationFeePolicy(null)
                        .pagoPaIntMode(PagoPaIntMode.ASYNC)
                        .vat(22)
                        .build()
        );

        assertTrue(exception.getMessage().contains("notificationFeePolicy"));
    }

    @Test
    void throwsExceptionWhenPagoPaIntModeIsNull() {
        BaseCostDto baseCost = BaseCostDto.builder().paFee(100).sendFee(50).build();

        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCostDto.builder()
                        .iun("IUN-TEST-123")
                        .recIndex(0)
                        .baseCost(baseCost)
                        .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                        .pagoPaIntMode(null)
                        .vat(22)
                        .build()
        );

        assertTrue(exception.getMessage().contains("pagoPaIntMode"));
    }

    @Test
    void throwsExceptionWhenMultipleFieldsAreNull() {
        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCostDto.builder()
                        .iun(null)
                        .recIndex(0)
                        .baseCost(null)
                        .notificationFeePolicy(null)
                        .pagoPaIntMode(null)
                        .vat(22)
                        .build()
        );

        String message = exception.getMessage();
        assertTrue(message.contains("iun"));
        assertTrue(message.contains("baseCost"));
        assertTrue(message.contains("notificationFeePolicy"));
        assertTrue(message.contains("pagoPaIntMode"));
    }

    @Test
    void throwsExceptionWhenBothFirstAnalogCostAndSimpleRegisteredLetterCostAreSet() {
        BaseCostDto baseCost = BaseCostDto.builder().paFee(100).sendFee(50).build();
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder().cost(200).productType("AR").build();
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = SimpleRegisteredLetterCostDto.builder().cost(150).productType("RS").build();

        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCostDto.builder()
                        .iun("IUN-TEST-123")
                        .recIndex(0)
                        .baseCost(baseCost)
                        .firstAnalogCost(firstAnalogCost)
                        .simpleRegisteredLetterCost(simpleRegisteredLetterCost)
                        .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                        .pagoPaIntMode(PagoPaIntMode.ASYNC)
                        .vat(22)
                        .build()
        );

        assertTrue(exception.getMessage().contains("Only one between firstAnalogCost and simpleRegisteredLetterCost can be set"));
    }

    @Test
    void buildsSuccessfullyWithFirstAnalogCostOnly() {
        BaseCostDto baseCost = BaseCostDto.builder().paFee(100).sendFee(50).build();
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder().cost(200).productType("AR").build();

        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .firstAnalogCost(firstAnalogCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(22)
                .build();

        assertNotNull(dto);
        assertEquals(firstAnalogCost, dto.getFirstAnalogCost());
        assertNull(dto.getSimpleRegisteredLetterCost());
    }

    @Test
    void buildsSuccessfullyWithSimpleRegisteredLetterCostOnly() {
        BaseCostDto baseCost = BaseCostDto.builder().paFee(100).sendFee(50).build();
        SimpleRegisteredLetterCostDto simpleRegisteredLetterCost = SimpleRegisteredLetterCostDto.builder().cost(150).productType("RS").build();

        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .simpleRegisteredLetterCost(simpleRegisteredLetterCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(22)
                .build();

        assertNotNull(dto);
        assertNull(dto.getFirstAnalogCost());
        assertEquals(simpleRegisteredLetterCost, dto.getSimpleRegisteredLetterCost());
    }

    @Test
    void buildsSuccessfullyWithoutAnalogCosts() {
        BaseCostDto baseCost = BaseCostDto.builder().paFee(100).sendFee(50).build();

        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
                .pagoPaIntMode(PagoPaIntMode.NONE)
                .vat(22)
                .build();

        assertNotNull(dto);
        assertNull(dto.getFirstAnalogCost());
        assertNull(dto.getSimpleRegisteredLetterCost());
        assertNull(dto.getSecondAnalogCost());
    }

    @Test
    void buildsSuccessfullyWithSecondAnalogCostAndFirstAnalogCost() {
        BaseCostDto baseCost = BaseCostDto.builder().paFee(100).sendFee(50).build();
        FirstAnalogCostDto firstAnalogCost = FirstAnalogCostDto.builder().cost(200).productType("AR").build();
        SecondAnalogCostDto secondAnalogCost = SecondAnalogCostDto.builder().cost(300).productType("890").build();

        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .firstAnalogCost(firstAnalogCost)
                .secondAnalogCost(secondAnalogCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(22)
                .build();

        assertNotNull(dto);
        assertEquals(firstAnalogCost, dto.getFirstAnalogCost());
        assertEquals(secondAnalogCost, dto.getSecondAnalogCost());
        assertNull(dto.getSimpleRegisteredLetterCost());
    }
}