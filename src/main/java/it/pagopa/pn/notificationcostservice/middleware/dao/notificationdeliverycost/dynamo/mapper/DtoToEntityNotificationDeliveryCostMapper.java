package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.NotificationDeliveryCostEntity;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityNotificationDeliveryCostMapper {
    public NotificationDeliveryCostEntity dto2Entity(NotificationDeliveryCostDto notificationDeliveryCostDto){
        return NotificationDeliveryCostEntity.builder()
                .pk(notificationDeliveryCostDto.getIun())
                .sk(notificationDeliveryCostDto.getRecIndex())
                .recipientInternalId(notificationDeliveryCostDto.getRecipientInternalId())
                .notificationFeePolicy(notificationDeliveryCostDto.getNotificationFeePolicy())
                .notificationViewDate(notificationDeliveryCostDto.getNotificationViewDate())
                .paFee(notificationDeliveryCostDto.getPaFee())
                .isCancelled(notificationDeliveryCostDto.getIsCancelled())
                .lastUpdate(notificationDeliveryCostDto.getLastUpdate())
                .ttl(notificationDeliveryCostDto.getTtl())
                .refinementDate(notificationDeliveryCostDto.getRefinementDate())
                .sendFee(notificationDeliveryCostDto.getSendFee())
                .pagoPaIntMode(notificationDeliveryCostDto.getPagoPaIntMode())
                .vat(notificationDeliveryCostDto.getVat())
                .firstAnalogCost(notificationDeliveryCostDto.getFirstAnalogCost())
                .baseCost(notificationDeliveryCostDto.getBaseCost())
                .secondAnalogCost(notificationDeliveryCostDto.getSecondAnalogCost())
                .simpleRegisteredLetterCost(notificationDeliveryCostDto.getSimpleRegisteredLetterCost())
                .isRefused(notificationDeliveryCostDto.getIsRefused())
                .build();
    }
}
