package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponse;
import it.pagopa.pn.notificationcostservice.service.NotificationCostService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@CustomLog
public class NotificationCostServiceController implements NotificationCostRecipientApi {

    private final NotificationCostService notificationCostService;

    @Override
    public Mono<ResponseEntity<NotificationCostRecipientResponse>> notificationCostRecipient(String iun,
                                                                                             Integer recIndex,
                                                                                             final ServerWebExchange exchange) {
        return notificationCostService.getNotificationCostRecipient(iun, recIndex)
                .map(ResponseEntity::ok);
    }

}
