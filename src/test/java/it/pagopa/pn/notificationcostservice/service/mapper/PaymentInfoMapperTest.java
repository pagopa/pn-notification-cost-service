package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.model.CommunicationType;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PaymentInfoMapperTest {
    private final PaymentInfoMapper mapper = new PaymentInfoMapper();

    @Test
    @DisplayName("mapDtoToLegalPaymentInfo mappa più recipients")
    void shouldMapDtoToLegalPaymentInfo() {
        RecipientCostDataDto recipient1 = new RecipientCostDataDto()
                .recIndex(0)
                .recipientInternalId("recipient-1")
                .payments(List.of(new PaymentDataDto("IUV-1", true)));

        RecipientCostDataDto recipient2 = new RecipientCostDataDto()
                .recIndex(1)
                .recipientInternalId("recipient-2")
                .payments(List.of(new PaymentDataDto("IUV-2", false)));

        NewNotificationCostRequestDto request = new NewNotificationCostRequestDto()
                .senderPaId("sender-1")
                .senderTaxId("taxId")
                .paFee(50)
                .notificationFeePolicy(NotificationFeePolicyDto.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntModeDto.SYNC)
                .vat(22)
                .costRecipients(List.of(recipient1, recipient2));


        List<PaymentInfo> result = mapper.mapDtoToLegalPaymentInfo("IUN-123", request);

        assertThat(result).hasSize(2);

        assertThat(result.getFirst().getIun()).isEqualTo("IUN-123");
        assertThat(result.getFirst().getRecIndex()).isEqualTo(0);
        assertThat(result.getFirst().getIuv()).isEqualTo("IUV-1");
        assertThat(result.getFirst().getApplyCost()).isTrue();
        assertThat(result.getFirst().getCommunicationType()).isEqualTo(CommunicationType.LEGAL);

        assertThat(result.get(1).getIun()).isEqualTo("IUN-123");
        assertThat(result.get(1).getRecIndex()).isEqualTo(1);
        assertThat(result.get(1).getIuv()).isEqualTo("IUV-2");
        assertThat(result.get(1).getApplyCost()).isFalse();
        assertThat(result.get(1).getCommunicationType()).isEqualTo(CommunicationType.LEGAL);
    }

    @Test
    @DisplayName("mapDtoToLegalPaymentInfo restituisce lista vuota se non ci sono pagamenti nel recipient")
    void mapDtoToLegalPaymentInfoShouldReturnEmptyListWhenThereAreNoPayments() {
        RecipientCostDataDto recipient1 = new RecipientCostDataDto()
                .recIndex(0)
                .recipientInternalId("recipient-1")
                .payments(null);

        NewNotificationCostRequestDto request = new NewNotificationCostRequestDto()
                .senderPaId("sender-1")
                .senderTaxId("taxId")
                .paFee(50)
                .notificationFeePolicy(NotificationFeePolicyDto.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntModeDto.SYNC)
                .vat(22)
                .costRecipients(List.of(recipient1));

        List<PaymentInfo> result = mapper.mapDtoToLegalPaymentInfo("IUN-123", request);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }


    @Test
    @DisplayName("mapDtoToLegalPaymentInfo lancia NullPointerException se dto è null")
    void shouldThrowWhenDtoIsNullInMapDtoToLegalPaymentInfo() {
        assertThatThrownBy(() -> mapper.mapDtoToLegalPaymentInfo("IUN-123", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("mapDtoToInformalPaymentInfo mappa più recipients")
    void shouldMapDtoToInformalPaymentInfo() {
        InformalRecipientCostDataDto recipient1 = new InformalRecipientCostDataDto()
                .recIndex(0)
                .recipientInternalId("recipient-1")
                .payments(List.of(new InformalPaymentDataDto("IUV-1")));

        InformalRecipientCostDataDto recipient2 = new InformalRecipientCostDataDto()
                .recIndex(1)
                .recipientInternalId("recipient-2")
                .payments(List.of(new InformalPaymentDataDto("IUV-2")));

        NewInformalNotificationCostRequestDto request = new NewInformalNotificationCostRequestDto()
                .senderPaId("sender-1")
                .senderTaxId("taxId")
                .costRecipients(List.of(recipient1, recipient2));


        List<PaymentInfo> result = mapper.mapDtoToInformalPaymentInfo("IUN-123", request);

        assertThat(result).hasSize(2);

        assertThat(result.getFirst().getIun()).isEqualTo("IUN-123");
        assertThat(result.getFirst().getRecIndex()).isEqualTo(0);
        assertThat(result.getFirst().getIuv()).isEqualTo("IUV-1");
        assertThat(result.getFirst().getApplyCost()).isNull();
        assertThat(result.getFirst().getCommunicationType()).isEqualTo(CommunicationType.INFORMAL);

        assertThat(result.get(1).getIun()).isEqualTo("IUN-123");
        assertThat(result.get(1).getRecIndex()).isEqualTo(1);
        assertThat(result.get(1).getIuv()).isEqualTo("IUV-2");
        assertThat(result.get(1).getApplyCost()).isNull();
        assertThat(result.get(1).getCommunicationType()).isEqualTo(CommunicationType.INFORMAL);
    }

    @Test
    @DisplayName("mapDtoToInformalPaymentInfo restituisce lista vuota se non ci sono pagamenti nel recipient")
    void mapDtoToInformalPaymentInfoShouldReturnEmptyListWhenThereAreNoPayments() {
        InformalRecipientCostDataDto recipient1 = new InformalRecipientCostDataDto()
                .recIndex(0)
                .recipientInternalId("recipient-1")
                .payments(null);

        NewInformalNotificationCostRequestDto request = new NewInformalNotificationCostRequestDto()
                .senderPaId("sender-1")
                .senderTaxId("taxId")
                .costRecipients(List.of(recipient1));

        List<PaymentInfo> result = mapper.mapDtoToInformalPaymentInfo("IUN-123", request);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("mapDtoToInformalPaymentInfo lancia NullPointerException se dto è null")
    void shouldThrowWhenDtoIsNullInMapDtoToInformalPaymentInfo() {
        assertThatThrownBy(() -> mapper.mapDtoToInformalPaymentInfo("IUN-123", null))
                .isInstanceOf(NullPointerException.class);
    }


}
