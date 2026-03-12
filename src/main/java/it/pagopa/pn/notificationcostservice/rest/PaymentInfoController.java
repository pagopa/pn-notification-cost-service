package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.api.NotificationCostApi;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRequestDto;

import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@CustomLog
public class PaymentInfoController implements NotificationCostApi {

    @Override
    public Mono<ResponseEntity<String>> initializeNotificationCost(String iun,
                                                                   Mono<NotificationCostRequestDto> notificationCostRequestDto,
                                                                   ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok("ok"));
    }
}
