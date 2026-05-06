package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.model.ValidationStatus;
import it.pagopa.pn.notificationcostservice.service.NotificationCostService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.service.mapper.PaymentInfoMapper;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.notificationcostservice.utils.PaymentUtils.composeIuv;

@RestController
@AllArgsConstructor
@CustomLog
public class NotificationCostServiceController implements NotificationCostRecipientApi {

    private final NotificationCostService notificationCostService;
    private final NotificationDeliveryCostMapper mapper;
    private final PaymentInfoMapper paymentInfoMapper;
    //private final UpdateNotificationCostMapper updateNotificationCostMapper;

    @Override
    public Mono<ResponseEntity<NotificationCostRecipientResponseDto>> getNotificationCost(String iun, Integer recIndex,
                                                                                          final ServerWebExchange exchange) {
        return notificationCostService.getNotificationCostRecipient(iun, recIndex)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<String>> initializeNotificationCost(String iun,
                                                                   Mono<NewNotificationCostRequestDto> notificationCostRequestDto,
                                                                   ServerWebExchange exchange) {
        return notificationCostRequestDto
                .flatMap(request -> notificationCostService.saveNotificationCost(iun, mapper.mapDtoToNotificationDeliveryCost(iun, request)
                        , paymentInfoMapper.mapDtoToPaymentInfo(iun, request)))
                .thenReturn(ResponseEntity.accepted().body(
                        ValidationStatus.OK.name())
                );
    }

    @Override
    public Mono<ResponseEntity<UpdateNotificationCostResponseDto>> updateNotificationCost(String iun, Mono<UpdateNotificationCostRequestDto> updateNotificationCostRequestDto, ServerWebExchange exchange) {
        //ToDo: L'API verrà implementata a partire dal task PN-19830
        return null;
    }


    @Override
    public Mono<ResponseEntity<NotificationCostPaymentResponseDto>> getNotificationCostByPayment(String creditorTaxId, String noticeCode,  final ServerWebExchange exchange) {
        return notificationCostService.getNotificationCostPaymentInfo(composeIuv(creditorTaxId, noticeCode))
                .map(ResponseEntity::ok);
    }
}
