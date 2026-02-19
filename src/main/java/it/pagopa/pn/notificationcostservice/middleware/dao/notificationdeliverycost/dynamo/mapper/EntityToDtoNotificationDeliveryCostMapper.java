package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoNotificationDeliveryCostMapper {
    public NotificationDeliveryCostDto entity2Dto(NotificationDeliveryCostEntity entity) {
        return NotificationDeliveryCostDto.builder()
                .iun(entity.getIun())
                .recIndex(entity.getRecIndex())
                .firstAnalogCost(mapFirstAnalogCost(entity.getFirstAnalogCost()))
                .simpleRegisteredLetterCost(mapSimpleRegisteredLetterCost(entity.getSimpleRegisteredLetterCost()))
                .notificationFeePolicy(entity.getNotificationFeePolicy())
                .paFee(entity.getPaFee())
                .recipientInternalId(entity.getRecipientInternalId())
                .secondAnalogCost(mapSecondAnalogCost(entity.getSecondAnalogCost()))
                .isDeleted(entity.getIsDeleted())
                .ttl(entity.getTtl())
                .vat(entity.getVat())
                .sendFee(entity.getSendFee())
                .senderInternalId(entity.getSenderInternalId())
                .pagoPaIntMode(entity.getPagoPaIntMode())
                .lastUpdate(entity.getLastUpdate())
                .baseCost(mapBaseCost(entity.getBaseCost()))
                .build();
    }

    private FirstAnalogCostDto mapFirstAnalogCost(FirstAnalogCost entity) {
        if (entity == null) return null;
        return FirstAnalogCostDto.builder()
                .cost(entity.getCost())
                .productType(entity.getProductType())
                .build();
    }

    private BaseCostDto mapBaseCost(BaseCost entity){
        if (entity == null) return null;
        return BaseCostDto.builder()
                .paFee(entity.getPaFee())
                .sendFee(entity.getSendFee())
                .build();
    }

    private SecondAnalogCostDto mapSecondAnalogCost(SecondAnalogCost entity) {
        if (entity == null) return null;
        return SecondAnalogCostDto.builder()
                .cost(entity.getCost())
                .productType(entity.getProductType())
                .build();
    }

    private SimpleRegisteredLetterCostDto mapSimpleRegisteredLetterCost(SimpleRegisteredLetterCost entity) {
        if (entity == null) return null;
        return SimpleRegisteredLetterCostDto.builder()
                .cost(entity.getCost())
                .productType(entity.getProductType())
                .build();
    }
}