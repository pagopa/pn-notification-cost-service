package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NewInformalNotificationCostRequestDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NewNotificationCostRequestDto;
import it.pagopa.pn.notificationcostservice.model.CommunicationType;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Component
public class PaymentInfoMapper {
    public List<PaymentInfo> mapDtoToLegalPaymentInfo(String iun, NewNotificationCostRequestDto dto) {
        return dto.getCostRecipients().stream()
                .flatMap(recipient -> {
                    if(recipient.getPayments() == null) {
                        return Stream.of();
                    }

                    return recipient.getPayments().stream()
                            .map(payment -> PaymentInfo.builder()
                                    .iun(iun)
                                    .recIndex(recipient.getRecIndex())
                                    .iuv(payment.getIuv())
                                    .applyCost(payment.getApplyCost())
                                    .communicationType(CommunicationType.LEGAL)
                                    .build());
                })
                .toList();
    }

    public List<PaymentInfo> mapDtoToInformalPaymentInfo(String iun, NewInformalNotificationCostRequestDto dto) {
        return dto.getCostRecipients().stream()
                .flatMap(recipient -> {
                    if(recipient.getPayments() == null) {
                        return Stream.of();
                    }

                    return recipient.getPayments().stream()
                            .map(payment -> PaymentInfo.builder()
                                    .iun(iun)
                                    .recIndex(recipient.getRecIndex())
                                    .iuv(payment.getIuv())
                                    .communicationType(CommunicationType.INFORMAL)
                                    .build());
                })
                .toList();
    }
}
