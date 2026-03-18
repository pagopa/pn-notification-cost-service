package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EntityToDtoNotificationDeliveryCostMapper {
    public NotificationDeliveryCost entity2Dto(NotificationDeliveryCostEntity entity) {
        return NotificationDeliveryCost.builder()
                .iun(entity.getIun())
                .recIndex(entity.getRecIndex())
                .firstAnalogCost(mapFirstAnalogCost(entity.getFirstAnalogCost()))
                .simpleRegisteredLetterCost(mapSimpleRegisteredLetterCost(entity.getSimpleRegisteredLetterCost()))
                .notificationFeePolicy(entity.getNotificationFeePolicy())
                .recipientInternalId(entity.getRecipientInternalId())
                .secondAnalogCost(mapSecondAnalogCost(entity.getSecondAnalogCost()))
                .isDeleted(entity.getIsDeleted())
                .ttl(entity.getTtl())
                .vat(entity.getVat())
                .senderInternalId(entity.getSenderInternalId())
                .pagoPaIntMode(entity.getPagoPaIntMode())
                .lastUpdate(entity.getLastUpdate())
                .baseCost(mapBaseCost(entity))
                .build();
    }

    private FirstAnalogCost mapFirstAnalogCost(FirstAnalogCostEntity entity) {
        if(Objects.isNull(entity)){
            return null;
        }
        return FirstAnalogCost.builder()
                .cost(entity.getCost())
                .productType(entity.getProductType())
                .build();
    }

    private BaseCost mapBaseCost(NotificationDeliveryCostEntity entity){
        return BaseCost.builder()
                .paFee(entity.getBaseCost().getPaFee())
                .sendFee(entity.getBaseCost().getSendFee())
                .build();
    }

    private SecondAnalogCost mapSecondAnalogCost(SecondAnalogCostEntity entity) {
        if(Objects.isNull(entity)){
            return null;
        }
        return SecondAnalogCost.builder()
                .cost(entity.getCost())
                .productType(entity.getProductType())
                .build();
    }

    private SimpleRegisteredLetterCost mapSimpleRegisteredLetterCost(SimpleRegisteredLetterCostEntity entity) {
        if(Objects.isNull(entity)){
            return null;
        }
        return SimpleRegisteredLetterCost.builder()
                .cost(entity.getCost())
                .productType(entity.getProductType())
                .build();
    }
}