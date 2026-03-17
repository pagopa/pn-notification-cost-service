package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.exception.PnNotificationDeliveryCostBadRequestException;
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
        var recipient = result.getRecipients().getFirst();
        assertEquals(1, recipient.getRecIndex());
        assertEquals("recipientInternalId", recipient.getRecipientInternalId());
        assertEquals(1, recipient.getPayments().size());
        assertEquals(NotificationFeePolicy.FLAT_RATE, recipient.getNotificationFeePolicy());
        assertEquals(PagoPaIntMode.ASYNC, recipient.getPagoPaIntMode());
    }

    @Test
    void fromDtoNullThrowsException() {
        assertThrows(PnNotificationDeliveryCostBadRequestException.class, () -> mapper.fromDto(null));
    }

    @Test
    void fromDtoWithNullRecipientsFieldThrowsNPE() {
        NotificationCostRequestDto dto = new NotificationCostRequestDto();
        dto.setRecipients(null);
        assertThrows(NullPointerException.class, () -> mapper.fromDto(dto));
    }

    @Test
    void fromDtoWithNullRecipientInListThrowsException() {
        NotificationCostRequestDto dto = new NotificationCostRequestDto();
        dto.setRecipients(Collections.singletonList(null));


        assertThrows(PnNotificationDeliveryCostBadRequestException.class, () -> mapper.fromDto(dto));
    }

    @Test
    void fromDtoWithNullPaymentsFieldThrowsNPE() {
        NotificationCostRequestDto dto = new NotificationCostRequestDto();
        RecipientCostDataDto recipientDto = getRecipientCostDataDto();
        recipientDto.setPayments(null);
        dto.setRecipients(Collections.singletonList(recipientDto));

        assertThrows(NullPointerException.class, () -> mapper.fromDto(dto));
    }

    @Test
    void fromDtoWithNullRequiredFieldThrowsNPE() {
        NotificationCostRequestDto dto = new NotificationCostRequestDto();
        RecipientCostDataDto recipientDto = getRecipientCostDataDto();
        recipientDto.setVat(null);
        dto.setRecipients(Collections.singletonList(recipientDto));

        NotificationCostRequest result = mapper.fromDto(dto);
        assertNull(result.getRecipients().getFirst().getVat());
    }

    @Test
    void fromDtoWithNullNotificationFeePolicyThrowsNPE() {
        NotificationCostRequestDto dto = new NotificationCostRequestDto();
        RecipientCostDataDto recipientDto = getRecipientCostDataDto();
        recipientDto.setNotificationFeePolicy(null);
        dto.setRecipients(Collections.singletonList(recipientDto));

        assertThrows(NullPointerException.class, () -> mapper.fromDto(dto));
    }

    private static @NotNull RecipientCostDataDto getRecipientCostDataDto() {
        RecipientCostDataDto dto = new RecipientCostDataDto();
        dto.setRecIndex(1);
        dto.setRecipientInternalId("recipientInternalId");
        dto.setSenderInternalId("senderInternalId");

        PaymentDataDto payment = new PaymentDataDto();
        payment.setIuv("iuv");
        payment.setApplyCost(true);

        dto.setPayments(Collections.singletonList(payment));
        dto.setBaseCost(100);
        dto.setSendFee(10);
        dto.setPaFee(1);
        dto.setNotificationFeePolicy(NotificationFeePolicyDto.FLAT_RATE);
        dto.setPagoPaIntMode(PagoPaIntModeDto.ASYNC);
        dto.setVat(22);
        return dto;
    }
}