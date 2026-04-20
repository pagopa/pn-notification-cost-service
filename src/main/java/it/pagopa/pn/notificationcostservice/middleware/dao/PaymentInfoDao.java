package it.pagopa.pn.notificationcostservice.middleware.dao;

import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaymentInfoDao {
    Mono<PaymentInfo> getPaymentInfoByIuv(String iuv);
    Mono<Void> updateItemIfNotExistsOrMatch(List<PaymentInfo> payments);
    Mono<Void> deleteItemsByIun(String iun);
}
