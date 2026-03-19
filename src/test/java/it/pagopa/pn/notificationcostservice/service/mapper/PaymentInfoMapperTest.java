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
        RecipientCostDataDto firstRecipient = new RecipientCostDataDto()
                .recIndex(0)
                .recipientInternalId("recipient-1")
                .senderInternalId("sender-1")
                .payments(List.of(new PaymentDataDto("IUV-1", true)))
                .baseCost(150)
                .sendFee(100)
                .paFee(50)
                .notificationFeePolicy(NotificationFeePolicyDto.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntModeDto.SYNC)
                .vat(22);

        RecipientCostDataDto secondRecipient = new RecipientCostDataDto()
                .recIndex(1)
                .recipientInternalId("recipient-2")
                .senderInternalId("sender-2")
                .payments(List.of(new PaymentDataDto("IUV-2", false)))
                .baseCost(150)
                .sendFee(100)
                .paFee(50)
                .notificationFeePolicy(NotificationFeePolicyDto.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntModeDto.SYNC)
                .vat(22);

        NewNotificationCostRequestDto request = new NewNotificationCostRequestDto()
                .costRecipients(List.of(firstRecipient, secondRecipient));

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
        RecipientCostDataDto recipient = new RecipientCostDataDto()
                .recIndex(0)
                .recipientInternalId("recipient-1")
                .senderInternalId("sender-1")
                .payments(null)
                .baseCost(150)
                .sendFee(100)
                .paFee(50)
                .notificationFeePolicy(NotificationFeePolicyDto.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntModeDto.SYNC)
                .vat(22);

        NewNotificationCostRequestDto request = new NewNotificationCostRequestDto()
                .costRecipients(List.of(recipient));

        assertThatThrownBy(() -> mapper.mapDtoToPaymentInfo("IUN-123", request))
                .isInstanceOf(NullPointerException.class);
    }
}
