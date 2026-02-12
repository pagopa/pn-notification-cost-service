package it.pagopa.pn.notificationcostservice.service;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import reactor.core.publisher.Mono;

public interface PaymentCostService {
    Mono<NotificationCostRecipientResponseDto> getNotificationCostRecipient(String iun, Integer recIndex);
}
