package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PaymentInfoMapperTest {
    private final PaymentInfoMapper mapper = new PaymentInfoMapper();

    @Test
    @DisplayName("mapDtoToPaymentInfo mappa più recipients")
    void shouldMapDtoToPaymentInfo() {
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


        List<PaymentInfo> result = mapper.mapDtoToPaymentInfo("IUN-123", request);

        assertThat(result).hasSize(2);

        assertThat(result.getFirst().getIun()).isEqualTo("IUN-123");
        assertThat(result.getFirst().getRecIndex()).isEqualTo(0);
        assertThat(result.get(0).getIuv()).isEqualTo("IUV-1");
        assertThat(result.get(0).isApplyCost()).isTrue();

        assertThat(result.get(1).getIun()).isEqualTo("IUN-123");
        assertThat(result.get(1).getRecIndex()).isEqualTo(1);
        assertThat(result.get(1).getIuv()).isEqualTo("IUV-2");
        assertThat(result.get(1).isApplyCost()).isFalse();
    }


    @Test
    @DisplayName("mapDtoToPaymentInfo lancia NullPointerException se dto è null")
    void shouldThrowWhenDtoIsNullInMapDtoToPaymentInfo() {
        assertThatThrownBy(() -> mapper.mapDtoToPaymentInfo("IUN-123", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("mapDtoToPaymentInfo lancia NullPointerException se payments è null")
    void shouldThrowWhenPaymentsIsNull() {
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


        assertThatThrownBy(() -> mapper.mapDtoToPaymentInfo("IUN-123", request))
                .isInstanceOf(NullPointerException.class);
    }
}
