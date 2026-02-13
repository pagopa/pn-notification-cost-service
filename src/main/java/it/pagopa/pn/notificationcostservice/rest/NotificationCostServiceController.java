package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponse;
import it.pagopa.pn.notificationcostservice.mapper.NotificationCostRecipientMapper;
import it.pagopa.pn.notificationcostservice.service.PaymentCostService;
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

    private final PaymentCostService paymentCostService;
    private final NotificationCostRecipientMapper mapper;

    @Override
    public Mono<ResponseEntity<NotificationCostRecipientResponse>> notificationCostRecipient(String iun,
                                                                                             Integer recIndex,
                                                                                             final ServerWebExchange exchange) {
        log.info("Start notificationCostRecipient iun={} recIndex={}", iun, recIndex);
        return paymentCostService.getNotificationCostRecipient(iun, recIndex)
                .map(cost -> ResponseEntity.ok(mapper.dtoResponse2Response(cost)))
                .doOnSuccess(response -> log.info("Successfully completed notificationCostRecipient for iun={}", iun))
                .doOnError(e -> log.error("Failed notificationCostRecipient for iun={}: {}", iun, e.getMessage()));
    }

}
