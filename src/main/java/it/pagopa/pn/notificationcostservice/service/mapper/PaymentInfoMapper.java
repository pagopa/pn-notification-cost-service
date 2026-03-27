package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NewNotificationCostRequestDto;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentInfoMapper {
    public List<PaymentInfo> mapDtoToPaymentInfo(String iun, NewNotificationCostRequestDto dto) {
        return dto.getCostRecipients().stream()
                .flatMap(recipient -> recipient.getPayments().stream()
                        .map(payment -> PaymentInfo.builder()
                                .iun(iun)
                                .recIndex(recipient.getRecIndex())
                                .iuv(payment.getIuv())
                                .applyCost(payment.getApplyCost())
                                .build()))
                .toList();
    }
}
