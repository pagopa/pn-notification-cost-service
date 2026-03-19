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
        RecipientCostDataDto firstRecipient = new RecipientCostDataDto(
                0,
                "recipient-1",
                "sender-1",
                150,
                100,
                50,
                NotificationFeePolicyDto.DELIVERY_MODE,
                PagoPaIntModeDto.SYNC,
                22
        ).payments(List.of(
                new PaymentDataDto("IUV-1", true),
                new PaymentDataDto("IUV-2", false)
        ));

        RecipientCostDataDto secondRecipient = new RecipientCostDataDto(
                1,
                "recipient-2",
                "sender-2",
                200,
                120,
                80,
                NotificationFeePolicyDto.FLAT_RATE,
                PagoPaIntModeDto.ASYNC,
                10
        ).payments(List.of(
                new PaymentDataDto("IUV-3", true)
        ));

        NewNotificationCostRequestDto request = new NewNotificationCostRequestDto()
                .costRecipients(List.of(firstRecipient, secondRecipient));

        List<PaymentInfo> result = mapper.mapDtoToPaymentInfo("IUN-123", request);

        assertThat(result).hasSize(3);

        assertThat(result.getFirst().getIun()).isEqualTo("IUN-123");
        assertThat(result.getFirst().getRecIndex()).isEqualTo(0);
        assertThat(result.get(0).getIuv()).isEqualTo("IUV-1");
        assertThat(result.get(0).isApplyCost()).isTrue();

        assertThat(result.get(1).getIun()).isEqualTo("IUN-123");
        assertThat(result.get(1).getRecIndex()).isEqualTo(0);
        assertThat(result.get(1).getIuv()).isEqualTo("IUV-2");
        assertThat(result.get(1).isApplyCost()).isFalse();

        assertThat(result.get(2).getIun()).isEqualTo("IUN-123");
        assertThat(result.get(2).getRecIndex()).isEqualTo(1);
        assertThat(result.get(2).getIuv()).isEqualTo("IUV-3");
        assertThat(result.get(2).isApplyCost()).isTrue();
    }

    @Test
    @DisplayName("mapDtoToPaymentInfo restituisce lista vuota se tutti i recipient hanno payments vuoti")
    void shouldReturnEmptyPaymentInfoListWhenPaymentsAreEmpty() {
        RecipientCostDataDto recipient = new RecipientCostDataDto(
                0,
                "recipient-1",
                "sender-1",
                150,
                100,
                50,
                NotificationFeePolicyDto.DELIVERY_MODE,
                PagoPaIntModeDto.SYNC,
                22
        ).payments(List.of());

        NewNotificationCostRequestDto request = new NewNotificationCostRequestDto()
                .costRecipients(List.of(recipient));

        List<PaymentInfo> result = mapper.mapDtoToPaymentInfo("IUN-123", request);

        assertThat(result).isEmpty();
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
        RecipientCostDataDto recipient = new RecipientCostDataDto(
                0,
                "recipient-1",
                "sender-1",
                150,
                100,
                50,
                NotificationFeePolicyDto.DELIVERY_MODE,
                PagoPaIntModeDto.SYNC,
                22
        );
        recipient.setPayments(null);

        NewNotificationCostRequestDto request = new NewNotificationCostRequestDto()
                .costRecipients(List.of(recipient));

        assertThatThrownBy(() -> mapper.mapDtoToPaymentInfo("IUN-123", request))
                .isInstanceOf(NullPointerException.class);
    }
}
