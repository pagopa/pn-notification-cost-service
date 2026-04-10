package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostPaymentResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.utils.NotificationCostEventBuilder;
import it.pagopa.pn.notificationcostservice.model.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import it.pagopa.pn.notificationcostservice.service.CostCalculator;
import it.pagopa.pn.notificationcostservice.service.NotificationCostService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_DELETED;
import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_PAYMENTINFO_NOTFOUND;

@Slf4j
@AllArgsConstructor
@Service
public class NotificationCostServiceImpl implements NotificationCostService {
    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final PaymentInfoDao paymentInfoDao;
    private final NotificationDeliveryCostMapper notificationDeliveryCostMapper;
    private final CostCalculator costCalculator;
    private final MomProducer<NotificationCostInitializationEvent> notificationCostInitialization;

    @Override
    public Mono<NotificationCostRecipientResponseDto> getNotificationCostRecipient(String iun, Integer recIndex) {
        log.info("Start to get notification cost recipient for iun: {} and recIndex: {}", iun, recIndex);
        return getNotificationDeliveryCost(iun, recIndex)
                .map(dto -> {
                    CalculatedCosts calculatedCosts = costCalculator.calculateCosts(dto);
                    return notificationDeliveryCostMapper.mapToNotificationCostRecipientResponse(dto, calculatedCosts);
                })
                .doOnError(e -> log.error("Error processing cost recipient for iun: {} and recIndex: {}", iun, recIndex, e));
    }

    @Override
    public Mono<Void> saveNotificationCost(String iun, List<NotificationDeliveryCost> notificationCosts, List<PaymentInfo> payments) {
        log.info("Start to save notification cost for iun: {}", iun);
        return Mono.fromRunnable(() -> {
                    var event = NotificationCostEventBuilder.buildNotificationCostEvent(iun, notificationCosts, payments);
                    notificationCostInitialization.push(event);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.info("NotificationCostInit event sent successfully for iun: {}", iun))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(1000))
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying event push for iun: {}. Attempt: {}", iun, retrySignal.totalRetries() + 1)
                        )
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            Throwable lastExceptionInRetry = retrySignal.failure();
                            log.warn("Retries exhausted {}, with last Exception: {}", retrySignal.totalRetries(), lastExceptionInRetry.getMessage());
                            return lastExceptionInRetry;
                        })
                )
                .then();
    }

    @Override
    public Mono<NotificationCostPaymentResponseDto> getNotificationCostPaymentInfo(String iuv) {
        return paymentInfoDao.getPaymentInfoByIuv(iuv)
                .switchIfEmpty(Mono.error(new PnNotFoundException("Not Found",
                        "PaymentInfo with iuv: " + iuv + " not found",
                        ERROR_CODE_PAYMENTINFO_NOTFOUND)))
                .flatMap(paymentInfo -> {
                    String iun = paymentInfo.getIun();
                    Integer recIndex = paymentInfo.getRecIndex();
                    log.info("Payment info retrieved for iuv: {}, iun: {}, recIndex: {}", iuv, iun, recIndex);
                    return getNotificationDeliveryCost(iun, recIndex)
                            .map(dto -> {
                                CalculatedCosts calculatedCosts = costCalculator.calculateCosts(dto, paymentInfo.isApplyCost());
                                return notificationDeliveryCostMapper.mapToNotificationCostPaymentResponse(dto, calculatedCosts);
                            });
                })
                .doOnError(e -> log.error("Error processing cost payment info for iuv: {}", iuv, e));
    }

    private Mono<NotificationDeliveryCost> getNotificationDeliveryCost(String iun, Integer recIndex) {
        return notificationDeliveryCostDao.getNotificationDeliveryCostItem(iun, recIndex)
                .map(dto -> {
                    if (Boolean.TRUE.equals(dto.getIsDeleted())) {
                        log.info("Notification with iun: {} and recIndex: {} is deleted", iun, recIndex);
                        throw new PnNotFoundException("Not Found",
                                "Notification with iun: " + dto.getIun() + " and recIndex: " + dto.getRecIndex() + " is deleted",
                                ERROR_CODE_NOTIFICATIONDELIVERYCOST_DELETED);
                    }
                    log.info("Item retrieved for iun: {} and recIndex: {}", iun, recIndex);
                    return dto;
                });
    }
}