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

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class NoticePaymentEventHandler {

    private final PaymentInfoDao paymentInfoDao;
    private final EventBridgeProducer<PnNoticePaymentPaidEvent> producer;

    public Mono<Void> handleNoticePaymentEvent(PnNoticePaymentEvent.Payload payload) {
        log.info("Handling NoticePaymentEvent for iuv={}", payload.getIuv());

        return getPaymentInfo(payload.getCreditorTaxId()+"##"+payload.getNoticeCode())
                .flatMap(info -> this.handlePaymentInfo(payload, info))
                .doOnError(ex ->
                        log.error("Error processing handlePaymentInfo for iuv={}", payload.getCreditorTaxId()+"##"+payload.getNoticeCode(), ex)
                );
    }

    private Mono<PaymentInfo> getPaymentInfo(String pk) {
        return paymentInfoDao.getPaymentInfoByIuv(pk);
    }

    private Mono<Void> handlePaymentInfo(PnNoticePaymentEvent.Payload payload, PaymentInfo info) {
        if(Objects.isNull(info)) {
            log.info("PaymentInfo not found for iuv={}", payload.getCreditorTaxId()+"##"+payload.getNoticeCode());
            return Mono.empty();
        }
        return updatePaymentInfoData(payload, info)
                .flatMap(producer::sendEvent)
                .then();
    }

    private Mono<PnNoticePaymentPaidEvent> updatePaymentInfoData(PnNoticePaymentEvent.Payload payload, PaymentInfo info) {
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
                .communicationType(info.getCommunicationType())
                .iun(info.getIun())
                .recIndex(info.getRecIndex())
                .build()).build());
    }

}
