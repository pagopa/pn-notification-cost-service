package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRequestDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.PaymentDataDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.RecipientCostDataDto;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.NotificationCostRequest;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentData;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.RecipientCostData;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class NotificationCostRequestMapper {

    public NotificationCostRequest fromDto(NotificationCostRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return NotificationCostRequest.builder()
                .recipients(dto.getRecipients().stream()
                        .filter(Objects::nonNull)
                        .map(this::toRecipientCostData)
                        .collect(Collectors.toList()))
                .build();
    }

    private RecipientCostData toRecipientCostData(RecipientCostDataDto dto) {
        if (dto == null) {
            return null;
        }

        return RecipientCostData.builder()
                .recIndex(dto.getRecIndex())
                .recipientInternalId(dto.getRecipientInternalId())
                .senderInternalId(dto.getSenderInternalId())
                .payments(dto.getPayments().stream()
                        .filter(Objects::nonNull)
                        .map(this::toPaymentData)
                        .collect(Collectors.toList()))
                .baseCost(dto.getBaseCost())
                .sendFee(dto.getSendFee())
                .paFee(dto.getPaFee())
                .notificationFeePolicy(NotificationFeePolicy.valueOf(Objects.requireNonNull(dto.getNotificationFeePolicy()).getValue()))
                .pagoPaIntMode(PagoPaIntMode.valueOf(Objects.requireNonNull(dto.getPagoPaIntMode()).getValue()))
                .vat(dto.getVat())
                .build();
    }

    private PaymentData toPaymentData(PaymentDataDto dto) {
        if (dto == null) {
            return null;
        }

        return PaymentData.builder()
                .iuv(Objects.requireNonNull(dto.getIuv()))
                .applyCost(dto.getApplyCost())
                .build();
    }
}
