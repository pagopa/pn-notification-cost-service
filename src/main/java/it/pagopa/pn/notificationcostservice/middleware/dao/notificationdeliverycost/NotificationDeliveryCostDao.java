package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import reactor.core.publisher.Mono;

public interface NotificationDeliveryCostDao {
    Mono<NotificationDeliveryCostDto> getNotificationDeliveryCostItem(String iun, Integer recIndex);
}
