package it.pagopa.pn.notificationcostservice.service;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponse;
import reactor.core.publisher.Mono;

public interface NotificationCostService {
    Mono<NotificationCostRecipientResponse> getNotificationCostRecipient(String iun, Integer recIndex);
}
