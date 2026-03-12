package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.NotificationCostRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class NotificationCostRequestMapperTest {

    private NotificationCostRequestMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NotificationCostRequestMapper();
    }

    @Test
    void fromDtoTest() {
        NotificationCostRequestDto dto = new NotificationCostRequestDto();
        RecipientCostDataDto recipientCostDataDto = getRecipientCostDataDto();
        dto.setRecipients(Collections.singletonList(recipientCostDataDto));

        NotificationCostRequest result = mapper.fromDto(dto);

        assertNotNull(result);
        assertEquals(1, result.getRecipients().size());
        assertEquals(1, result.getRecipients().getFirst().getRecIndex());
        assertEquals("recipientInternalId", result.getRecipients().getFirst().getRecipientInternalId());
        assertEquals("senderInternalId", result.getRecipients().getFirst().getSenderInternalId());
        assertEquals(1, result.getRecipients().getFirst().getPayments().size());
        assertEquals("iuv", result.getRecipients().getFirst().getPayments().getFirst().getIuv());
        assertTrue(result.getRecipients().getFirst().getApplyCost());
        assertEquals(100, result.getRecipients().getFirst().getBaseCost());
        assertEquals(10, result.getRecipients().getFirst().getSendFee());
        assertEquals(1, result.getRecipients().getFirst().getPaFee());
        assertEquals(NotificationFeePolicy.FLAT_RATE, result.getRecipients().getFirst().getNotificationFeePolicy());
        assertEquals(PagoPaIntMode.ASYNC, result.getRecipients().getFirst().getPagoPaIntMode());
        assertEquals(22, result.getRecipients().getFirst().getVat());
    }

    private static @NotNull RecipientCostDataDto getRecipientCostDataDto() {
        RecipientCostDataDto recipientCostDataDto = new RecipientCostDataDto();
        recipientCostDataDto.setRecIndex(1);
        recipientCostDataDto.setRecipientInternalId("recipientInternalId");
        recipientCostDataDto.setSenderInternalId("senderInternalId");
        PaymentDataDto paymentDataDto = new PaymentDataDto();
        paymentDataDto.setIuv("iuv");
        recipientCostDataDto.setPayments(Collections.singletonList(paymentDataDto));
        recipientCostDataDto.setApplyCost(true);
        recipientCostDataDto.setBaseCost(100);
        recipientCostDataDto.setSendFee(10);
        recipientCostDataDto.setPaFee(1);
        recipientCostDataDto.setNotificationFeePolicy(NotificationFeePolicyDto.FLAT_RATE);
        recipientCostDataDto.setPagoPaIntMode(PagoPaIntModeDto.ASYNC);
        recipientCostDataDto.setVat(22);
        return recipientCostDataDto;
    }

    @Test
    void fromDtoNullTest() {
        NotificationCostRequest result = mapper.fromDto(null);
        assertNull(result);
    }

    @Test
    void fromDtoWithNullRecipientTest() {
        NotificationCostRequestDto dto = new NotificationCostRequestDto();
        dto.setRecipients(Collections.singletonList(null));
        NotificationCostRequest result = mapper.fromDto(dto);
        assertNotNull(result);
        assertEquals(0, result.getRecipients().size());
    }

    @Test
    void fromDtoWithNullPaymentTest() {
        NotificationCostRequestDto dto = new NotificationCostRequestDto();
        RecipientCostDataDto recipientCostDataDto = new RecipientCostDataDto();
        recipientCostDataDto.setPayments(Collections.singletonList(null));
        recipientCostDataDto.setNotificationFeePolicy(NotificationFeePolicyDto.FLAT_RATE);
        recipientCostDataDto.setPagoPaIntMode(PagoPaIntModeDto.ASYNC);
        dto.setRecipients(Collections.singletonList(recipientCostDataDto));
        NotificationCostRequest result = mapper.fromDto(dto);
        assertNotNull(result);
        assertEquals(1, result.getRecipients().size());
        assertNotNull(result.getRecipients().getFirst());
        assertEquals(0, result.getRecipients().getFirst().getPayments().size());
    }
}
