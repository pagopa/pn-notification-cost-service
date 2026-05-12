package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.api.PaperCostApi;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogUpdateCostPhaseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NewNotificationCostRequestDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostPaymentResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.PaperCostToInvalidateDto;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.model.ValidationStatus;
import it.pagopa.pn.notificationcostservice.service.NotificationCostService;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.service.mapper.PaymentInfoMapper;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.notificationcostservice.utils.PaymentUtils.composeIuv;

@RestController
@AllArgsConstructor
@CustomLog
public class NotificationCostServiceController implements NotificationCostRecipientApi, PaperCostApi {

    private static final String REC_INDEX_PREFIX = "RECINDEX_";
    private static final int INVALIDATED_COST = 0;

    private final NotificationCostService notificationCostService;
    private final NotificationCostUpdaterService notificationCostUpdaterService;
    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final NotificationDeliveryCostMapper mapper;
    private final PaymentInfoMapper paymentInfoMapper;

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
    public Mono<ResponseEntity<NotificationCostPaymentResponseDto>> getNotificationCostByPayment(String creditorTaxId, String noticeCode,  final ServerWebExchange exchange) {
        return notificationCostService.getNotificationCostPaymentInfo(composeIuv(creditorTaxId, noticeCode))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> invalidatePaperCost(String iun,
                                                          Mono<PaperCostToInvalidateDto> paperCostToInvalidateDto,
                                                          ServerWebExchange exchange) {
        return paperCostToInvalidateDto
                .doOnNext(request -> log.info("Starting paper cost invalidation for rework flow, iun={}, recIndex={}, costPhases={}",
                        iun,
                        request.getRecIndex(),
                        request.getCostPhases()))
                .flatMapMany(request -> Flux.fromIterable(getCostPhasesOrEmpty(request))
                        .map(costPhase -> mapInvalidateRequestToNotificationCostUpdate(iun, request.getRecIndex(), costPhase))
                        .concatMap(update -> checkNotificationDeliveryCostExists(update)
                                .then(Mono.defer(() -> notificationCostUpdaterService.updateCostByPhase(update)))))
                .then(Mono.fromSupplier(() -> {
                    log.info("Completed paper cost invalidation for rework flow, iun={}", iun);
                    return ResponseEntity.noContent().build();
                }));
    }

    private Mono<Void> checkNotificationDeliveryCostExists(NotificationCostUpdate update) {
        return notificationDeliveryCostDao.getNotificationDeliveryCostItem(update.getIun(), update.getRecIndex())
                .doOnNext(cost -> log.debug("NotificationDeliveryCost found before invalidation update, iun={}, recIndex={}, phase={}",
                        update.getIun(),
                        update.getRecIndex(),
                        update.getCostUpdatePhase()))
                .then();
    }

    private NotificationCostUpdate mapInvalidateRequestToNotificationCostUpdate(String iun,
                                                                                String recIndex,
                                                                                AnalogUpdateCostPhaseDto costPhase) {
        return NotificationCostUpdate.builder()
                .iun(iun)
                .recIndex(parseRecIndex(recIndex))
                .cost(INVALIDATED_COST)
                .productType(null)
                .costUpdatePhase(CostUpdatePhaseInt.valueOf(costPhase.name()))
                .elementTimestamp(Instant.now())
                .invalidationFlow(true)
                .build();
    }

    private Integer parseRecIndex(String recIndex) {
        return Integer.parseInt(recIndex.replace(REC_INDEX_PREFIX, ""));
    }

    private List<AnalogUpdateCostPhaseDto> getCostPhasesOrEmpty(PaperCostToInvalidateDto request) {
        return request.getCostPhases() == null ? List.of() : request.getCostPhases();
    }
}
