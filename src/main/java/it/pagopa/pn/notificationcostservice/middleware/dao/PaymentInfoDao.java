package it.pagopa.pn.notificationcostservice.middleware.dao;

import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaymentInfoDao {
    Mono<List<PaymentInfo>> updateItem(List<PaymentInfo> payments);
}
