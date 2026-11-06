package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.eventbus.EventBridgeProducer;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class NoticePaymentEventHandler {

    private final PaymentInfoDao paymentInfoDao;
    private final EventBridgeProducer<PnNoticePaymentPaidEvent> producer;

    public Mono<Void> handle(PnNoticePaymentEvent.Payload payload) {
        log.debug("Handling PnNoticePaymentEvent with id={} having creditorTaxId={} and noticeCode={}", payload.getPaymentId(), payload.getCreditorTaxId(), payload.getNoticeCode());

        String iuv = payload.getCreditorTaxId()+"##"+payload.getNoticeCode();
        return getPaymentInfo(iuv)
                .switchIfEmpty(Mono.fromRunnable(() -> log.debug("PaymentInfo not found for iuv={}", iuv)))
                .flatMap(info -> this.handlePaymentInfo(payload, info))
                .doOnError(ex ->
                        log.error("Error processing handlePaymentInfo for iuv={}", payload.getCreditorTaxId()+"##"+payload.getNoticeCode(), ex)
                );
    }

    private Mono<PaymentInfo> getPaymentInfo(String pk) {
        return paymentInfoDao.getPaymentInfoByIuv(pk);
    }

    private Mono<Void> handlePaymentInfo(PnNoticePaymentEvent.Payload payload, PaymentInfo info) {
        log.info("PaymentInfo found for iuv={}, related to iun={} and recIndex={}. Sending PnNoticePaymentPaidEvent", info.getIuv(), info.getIun(), info.getRecIndex());
        return enrichEventWithNotificationInfo(payload, info)
                .flatMap(producer::sendEvent)
                .then();
    }

    private Mono<PnNoticePaymentPaidEvent> enrichEventWithNotificationInfo(PnNoticePaymentEvent.Payload payload, PaymentInfo info) {
        return Mono.just(PnNoticePaymentPaidEvent.builder()
                .payload(PnNoticePaymentPaidEvent.Payload.builder()
                .modelType(payload.getModelType())
                .iuv(payload.getIuv())
                .iur(payload.getIur())
                .noticeCode(payload.getNoticeCode())
                .creditorTaxId(payload.getCreditorTaxId())
                .creditorCompanyName(payload.getCreditorCompanyName())
                .paymentDateTime(payload.getPaymentDateTime())
                .dueDate(payload.getDueDate())
                .totalNotice(payload.getTotalNotice())
                .paymentSourceChannel(payload.getPaymentSourceChannel())
                .amount(payload.getAmount())
                .paymentMethod(payload.getPaymentMethod())
                .fee(payload.getFee())
                .paymentId(payload.getPaymentId())
                .pagopaPersistedTimestamp(payload.getPagopaPersistedTimestamp())
                .pagopaPublishedTimestamp(payload.getPagopaPublishedTimestamp())
                .sendIngestionTimestamp(payload.getSendIngestionTimestamp())
                .kafkaPartition(payload.getKafkaPartition())
                .communicationType(info.getCommunicationType().name())
                .iun(info.getIun())
                .recIndex(info.getRecIndex())
                .build()).build());
    }

}
