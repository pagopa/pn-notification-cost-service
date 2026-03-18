package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRequestDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.RequestAcceptedDto;
import it.pagopa.pn.notificationcostservice.model.ValidationStatus;
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
    public Mono<ResponseEntity<NotificationCostRecipientResponseDto>> notificationCostRecipient(String iun,
                                                                                             Integer recIndex,
                                                                                             final ServerWebExchange exchange) {
        return notificationCostService.getNotificationCostRecipient(iun, recIndex)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<RequestAcceptedDto>> initializeNotificationCost(String iun,
                                                                               Mono<NotificationCostRequestDto> notificationCostRequestDto,
                                                                               ServerWebExchange exchange) {
        return notificationCostRequestDto
                .map(notificationCostRequestMapper::fromDto)
                .flatMap(request -> notificationCostService.saveNotificationCost(iun, request))
                .thenReturn(ResponseEntity.accepted().body(
                        new RequestAcceptedDto().status(ValidationStatus.OK.name())
                ));
    }

}
