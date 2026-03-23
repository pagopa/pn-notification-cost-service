package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostEntity;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DtoToEntityNotificationDeliveryCostMapper {
    public NotificationDeliveryCostEntity dto2Entity(NotificationDeliveryCost notificationDeliveryCost) {
        return NotificationDeliveryCostEntity.builder()
                .iun(notificationDeliveryCost.getIun())
                .recIndex(notificationDeliveryCost.getRecIndex())
                .recipientInternalId(notificationDeliveryCost.getRecipientInternalId())
                .notificationFeePolicy(notificationDeliveryCost.getNotificationFeePolicy())
                .isDeleted(notificationDeliveryCost.getIsDeleted())
                .lastUpdate(notificationDeliveryCost.getLastUpdate())
                .ttl(notificationDeliveryCost.getTtl())
                .senderInternalId(notificationDeliveryCost.getSenderInternalId())
                .pagoPaIntMode(notificationDeliveryCost.getPagoPaIntMode())
                .vat(notificationDeliveryCost.getVat())
                .firstAnalogCost(mapFirstAnalogCost(notificationDeliveryCost.getFirstAnalogCost()))
                .baseCost(mapBaseCost(notificationDeliveryCost))
                .secondAnalogCost(mapSecondAnalogCost(notificationDeliveryCost.getSecondAnalogCost()))
                .simpleRegisteredLetterCost(mapSimpleRegisteredLetterCost(notificationDeliveryCost.getSimpleRegisteredLetterCost()))
                .build();
    }

    private FirstAnalogCostEntity mapFirstAnalogCost(FirstAnalogCost dto) {
        if(Objects.isNull(dto)){
            return null;
        }
        return FirstAnalogCostEntity.builder()
                .cost(dto.getCost())
                .productType(dto.getProductType())
                .build();
    }

    private BaseCostEntity mapBaseCost(NotificationDeliveryCost dto){
        return BaseCostEntity.builder()
                .paFee(dto.getBaseCost().getPaFee())
                .sendFee(dto.getBaseCost().getSendFee())
                .build();
    }

    private SecondAnalogCostEntity mapSecondAnalogCost(SecondAnalogCost dto) {
        if(Objects.isNull(dto)){
            return null;
        }
        return SecondAnalogCostEntity.builder()
                .cost(dto.getCost())
                .productType(dto.getProductType())
                .build();
    }

    private SimpleRegisteredLetterCostEntity mapSimpleRegisteredLetterCost(SimpleRegisteredLetterCost dto) {
        if(Objects.isNull(dto)){
            return null;
        }
        return SimpleRegisteredLetterCostEntity.builder()
                .cost(dto.getCost())
                .productType(dto.getProductType())
                .build();
    }
}