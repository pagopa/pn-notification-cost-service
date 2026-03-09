package it.pagopa.pn.notificationcostservice.service;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import reactor.core.publisher.Mono;

public interface NotificationCostService {
    Mono<NotificationCostRecipientResponseDto> getNotificationCostRecipient(String iun, Integer recIndex);
}
