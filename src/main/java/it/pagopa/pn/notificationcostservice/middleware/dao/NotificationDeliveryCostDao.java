package it.pagopa.pn.notificationcostservice.middleware.dao;

import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NotificationDeliveryCostDao {
    Mono<NotificationDeliveryCost> getNotificationDeliveryCostItem(String iun, Integer recIndex);
    Mono<Void> putIfAbsent(List<NotificationDeliveryCost> notificationPayments);
}
