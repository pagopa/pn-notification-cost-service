package it.pagopa.pn.notificationcostservice.middleware.dao;

import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import reactor.core.publisher.Mono;

public interface NotificationDeliveryCostDao {
    Mono<NotificationDeliveryCost> getNotificationDeliveryCostItem(String iun, Integer recIndex);
    Mono<NotificationDeliveryCostEntity> updateNotificationDeliveryCostNotNull(NotificationDeliveryCostEntity notificationDeliveryCosts);
}
