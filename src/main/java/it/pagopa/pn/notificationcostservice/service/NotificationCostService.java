package it.pagopa.pn.notificationcostservice.service;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NotificationCostService {
    Mono<NotificationCostRecipientResponseDto> getNotificationCostRecipient(String iun, Integer recIndex);
    Mono<Void> saveNotificationCost(String iun,List<NotificationDeliveryCost> notificationCosts, List<PaymentInfo> payments);
}
