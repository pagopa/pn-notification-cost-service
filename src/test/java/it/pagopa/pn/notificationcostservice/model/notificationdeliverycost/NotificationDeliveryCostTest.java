package it.pagopa.pn.notificationcostservice.model.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDeliveryCostTest {

    @Test
    void buildsSuccessfullyWithAllRequiredFields() {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();

        NotificationDeliveryCost dto = NotificationDeliveryCost.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(22)
                .senderTaxId("taxId")
                .senderPaId("senderPaId")
                .lastUpdate(Instant.now())
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
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();

        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCost.builder()
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
                NotificationDeliveryCost.builder()
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
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();

        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCost.builder()
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
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();

        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCost.builder()
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
                NotificationDeliveryCost.builder()
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
    void throwsExceptionWhenBothFirstAnalogCostAndSimpleRegisteredLetterCostAreSetWithNonZeroCost() {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();
        FirstAnalogCost firstAnalogCost = FirstAnalogCost.builder().cost(200).productType("AR").build();
        SimpleRegisteredLetterCost simpleRegisteredLetterCost = SimpleRegisteredLetterCost.builder().cost(150).productType("RS").build();

        PnDomainObjectValidationException exception = assertThrows(PnDomainObjectValidationException.class, () ->
                NotificationDeliveryCost.builder()
                        .iun("IUN-TEST-123")
                        .recIndex(0)
                        .baseCost(baseCost)
                        .firstAnalogCost(firstAnalogCost)
                        .simpleRegisteredLetterCost(simpleRegisteredLetterCost)
                        .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                        .pagoPaIntMode(PagoPaIntMode.ASYNC)
                        .vat(22)
                        .senderPaId("paId")
                        .senderTaxId("taxId")
                        .build()
        );

        assertTrue(exception.getMessage().contains("can both be set only when both costs are 0"));
    }

    @Test
    void buildsSuccessfullyWhenBothFirstAnalogCostAndSimpleRegisteredLetterCostAreSetWithZeroCost() {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();
        FirstAnalogCost firstAnalogCost = FirstAnalogCost.builder().cost(0).productType("AR").build();
        SimpleRegisteredLetterCost simpleRegisteredLetterCost = SimpleRegisteredLetterCost.builder().cost(0).productType("RS").build();

        NotificationDeliveryCost dto = NotificationDeliveryCost.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .firstAnalogCost(firstAnalogCost)
                .simpleRegisteredLetterCost(simpleRegisteredLetterCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(22)
                .lastUpdate(Instant.now())
                .senderPaId("paId")
                .senderTaxId("taxId")
                .build();

        assertNotNull(dto);
        assertEquals(firstAnalogCost, dto.getFirstAnalogCost());
        assertEquals(simpleRegisteredLetterCost, dto.getSimpleRegisteredLetterCost());
    }

    @Test
    void buildsSuccessfullyWithFirstAnalogCostOnly() {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();
        FirstAnalogCost firstAnalogCost = FirstAnalogCost.builder().cost(200).productType("AR").build();

        NotificationDeliveryCost dto = NotificationDeliveryCost.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .firstAnalogCost(firstAnalogCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(22)
                .lastUpdate(Instant.now())
                .senderPaId("paId")
                .senderTaxId("taxId")
                .build();

        assertNotNull(dto);
        assertEquals(firstAnalogCost, dto.getFirstAnalogCost());
        assertNull(dto.getSimpleRegisteredLetterCost());
    }

    @Test
    void buildsSuccessfullyWithSimpleRegisteredLetterCostOnly() {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();
        SimpleRegisteredLetterCost simpleRegisteredLetterCost = SimpleRegisteredLetterCost.builder().cost(150).productType("RS").build();

        NotificationDeliveryCost dto = NotificationDeliveryCost.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .simpleRegisteredLetterCost(simpleRegisteredLetterCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(22)
                .lastUpdate(Instant.now())
                .senderPaId("paId")
                .senderTaxId("taxId")
                .build();

        assertNotNull(dto);
        assertNull(dto.getFirstAnalogCost());
        assertEquals(simpleRegisteredLetterCost, dto.getSimpleRegisteredLetterCost());
    }

    @Test
    void buildsSuccessfullyWithoutAnalogCosts() {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();

        NotificationDeliveryCost dto = NotificationDeliveryCost.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
                .pagoPaIntMode(PagoPaIntMode.NONE)
                .vat(22)
                .lastUpdate(Instant.now())
                .senderPaId("paId")
                .senderTaxId("taxId")
                .build();

        assertNotNull(dto);
        assertNull(dto.getFirstAnalogCost());
        assertNull(dto.getSimpleRegisteredLetterCost());
        assertNull(dto.getSecondAnalogCost());
    }

    @Test
    void buildsSuccessfullyWithSecondAnalogCostAndFirstAnalogCost() {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();
        FirstAnalogCost firstAnalogCost = FirstAnalogCost.builder().cost(200).productType("AR").build();
        SecondAnalogCost secondAnalogCost = SecondAnalogCost.builder().cost(300).productType("890").build();

        NotificationDeliveryCost dto = NotificationDeliveryCost.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .firstAnalogCost(firstAnalogCost)
                .secondAnalogCost(secondAnalogCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(22)
                .lastUpdate(Instant.now())
                .senderPaId("paId")
                .senderTaxId("taxId")
                .build();

        assertNotNull(dto);
        assertEquals(firstAnalogCost, dto.getFirstAnalogCost());
        assertEquals(secondAnalogCost, dto.getSecondAnalogCost());
        assertNull(dto.getSimpleRegisteredLetterCost());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 22, 100})
    void buildsSuccessfullyWhenVatIsWithinRange(int vat) {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();
        NotificationDeliveryCost dto = NotificationDeliveryCost.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(vat)
                .lastUpdate(Instant.now())
                .senderPaId("paId")
                .senderTaxId("taxId")
                .build();
        assertNotNull(dto);
        assertEquals(vat, dto.getVat());
    }

    @Test
    void throwsExceptionWhenVatIsLessThanMinimum() {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();
        NotificationDeliveryCost.NotificationDeliveryCostBuilder builder = NotificationDeliveryCost.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(-1);
        PnDomainObjectValidationException exception = assertThrows(
                PnDomainObjectValidationException.class,
                builder::build
        );

        assertTrue(exception.getMessage().contains("Field vat cannot be less than 0"));
    }

    @Test
    void throwsExceptionWhenVatIsGreaterThanMaximum() {
        BaseCost baseCost = BaseCost.builder().paFee(100).sendFee(50).build();
        NotificationDeliveryCost.NotificationDeliveryCostBuilder builder = NotificationDeliveryCost.builder()
                .iun("IUN-TEST-123")
                .recIndex(0)
                .baseCost(baseCost)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .vat(101);
        PnDomainObjectValidationException exception = assertThrows(
                PnDomainObjectValidationException.class,
                builder::build
        );

        assertTrue(exception.getMessage().contains("Field vat cannot be greater than 100"));
    }
}