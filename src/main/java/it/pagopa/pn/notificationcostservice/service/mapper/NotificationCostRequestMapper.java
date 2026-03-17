package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRequestDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.PaymentDataDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.RecipientCostDataDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotificationDeliveryCostBadRequestException;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.NotificationCostRequest;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentData;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.RecipientCostData;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_BAD_REQUEST;

@Component
public class NotificationCostRequestMapper {

    public NotificationCostRequest fromDto(NotificationCostRequestDto dto) {
        if (dto == null) {
            throw new PnNotificationDeliveryCostBadRequestException("request object cannot be null",
                    "request object cannot be null", ERROR_CODE_NOTIFICATIONDELIVERYCOST_BAD_REQUEST);
        }

        if (dto.getRecipients() == null || dto.getRecipients().isEmpty()) {
            throw new PnNotificationDeliveryCostBadRequestException("recipients list cannot be null or empty",
                    "recipients list cannot be null or empty", ERROR_CODE_NOTIFICATIONDELIVERYCOST_BAD_REQUEST);
        }

        return NotificationCostRequest.builder()
                .recipients(dto.getRecipients().stream()
                        .map(this::toRecipientCostData)
                        .toList())
                .build();
    }

    private RecipientCostData toRecipientCostData(RecipientCostDataDto dto) {
        if (dto == null) {
            throw new PnNotificationDeliveryCostBadRequestException("recipient data cannot be null",
                    "recipient data cannot be null", ERROR_CODE_NOTIFICATIONDELIVERYCOST_BAD_REQUEST);
        }
        return RecipientCostData.builder()
                .recIndex(dto.getRecIndex())
                .recipientInternalId(dto.getRecipientInternalId())
                .senderInternalId(dto.getSenderInternalId())
                .payments(dto.getPayments() == null
                        ? java.util.Collections.emptyList()
                        : dto.getPayments().stream()
                        .map(this::toPaymentData)
                        .toList())
                .baseCost(dto.getBaseCost())
                .sendFee(dto.getSendFee())
                .paFee(dto.getPaFee())
                .notificationFeePolicy(NotificationFeePolicy.valueOf(dto.getNotificationFeePolicy().getValue()))
                .pagoPaIntMode(PagoPaIntMode.valueOf(dto.getPagoPaIntMode().getValue()))
                .vat(dto.getVat())
                .build();
    }

    private PaymentData toPaymentData(PaymentDataDto dto) {
        if (dto == null) return null;

        return PaymentData.builder()
                .iuv(dto.getIuv())
                .applyCost(dto.getApplyCost())
                .build();
    }
}
