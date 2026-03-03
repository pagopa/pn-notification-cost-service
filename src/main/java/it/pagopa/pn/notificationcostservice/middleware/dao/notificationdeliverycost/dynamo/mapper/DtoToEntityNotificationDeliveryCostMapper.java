package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.*;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DtoToEntityNotificationDeliveryCostMapper {
    public NotificationDeliveryCostEntity dto2Entity(NotificationDeliveryCostDto notificationDeliveryCostDto) {
        return NotificationDeliveryCostEntity.builder()
                .iun(notificationDeliveryCostDto.getIun())
                .recIndex(notificationDeliveryCostDto.getRecIndex())
                .recipientInternalId(notificationDeliveryCostDto.getRecipientInternalId())
                .notificationFeePolicy(notificationDeliveryCostDto.getNotificationFeePolicy())
                .isDeleted(notificationDeliveryCostDto.getIsDeleted())
                .lastUpdate(notificationDeliveryCostDto.getLastUpdate())
                .ttl(notificationDeliveryCostDto.getTtl())
                .senderInternalId(notificationDeliveryCostDto.getSenderInternalId())
                .pagoPaIntMode(notificationDeliveryCostDto.getPagoPaIntMode())
                .vat(notificationDeliveryCostDto.getVat())
                .firstAnalogCost(mapFirstAnalogCost(notificationDeliveryCostDto.getFirstAnalogCost()))
                .baseCost(mapBaseCost(notificationDeliveryCostDto))
                .secondAnalogCost(mapSecondAnalogCost(notificationDeliveryCostDto.getSecondAnalogCost()))
                .simpleRegisteredLetterCost(mapSimpleRegisteredLetterCost(notificationDeliveryCostDto.getSimpleRegisteredLetterCost()))
                .build();
    }

    private FirstAnalogCost mapFirstAnalogCost(FirstAnalogCostDto dto) {
        if(Objects.isNull(dto)){
            return null;
        }
        return FirstAnalogCost.builder()
                .cost(dto.getCost())
                .productType(dto.getProductType())
                .build();
    }

    private BaseCost mapBaseCost(NotificationDeliveryCostDto dto){
        return BaseCost.builder()
                .paFee(dto.getBaseCost().getPaFee())
                .sendFee(dto.getBaseCost().getSendFee())
                .build();
    }

    private SecondAnalogCost mapSecondAnalogCost(SecondAnalogCostDto dto) {
        if(Objects.isNull(dto)){
            return null;
        }
        return SecondAnalogCost.builder()
                .cost(dto.getCost())
                .productType(dto.getProductType())
                .build();
    }

    private SimpleRegisteredLetterCost mapSimpleRegisteredLetterCost(SimpleRegisteredLetterCostDto dto) {
        if(Objects.isNull(dto)){
            return null;
        }
        return SimpleRegisteredLetterCost.builder()
                .cost(dto.getCost())
                .productType(dto.getProductType())
                .build();
    }
}