package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.NotificationDeliveryCostEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoNotificationDeliveryCostMapper {
    public NotificationDeliveryCostDto entity2Dto(NotificationDeliveryCostEntity entity) {
        return NotificationDeliveryCostDto.builder()
                .iun(entity.getIun())
                .recIndex(entity.getRecIndex())
                .firstAnalogCost(entity.getFirstAnalogCost())
                .simpleRegisteredLetterCost(entity.getSimpleRegisteredLetterCost())
                .notificationFeePolicy(entity.getNotificationFeePolicy())
                .notificationViewDate(entity.getNotificationViewDate())
                .paFee(entity.getPaFee())
                .recipientInternalId(entity.getRecipientInternalId())
                .secondAnalogCost(entity.getSecondAnalogCost())
                .isCancelled(entity.getIsCancelled())
                .ttl(entity.getTtl())
                .vat(entity.getVat())
                .sendFee(entity.getSendFee())
                .refinementDate(entity.getRefinementDate())
                .pagoPaIntMode(entity.getPagoPaIntMode())
                .lastUpdate(entity.getLastUpdate())
                .isRefused(entity.getIsRefused())
                .baseCost(entity.getBaseCost())
                .build();
    }
}
