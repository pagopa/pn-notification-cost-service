package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDetailsDto;
import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDto;
import it.pagopa.pn.notificationcostservice.dto.cost.analogcost.AnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.cost.basecost.BaseCostDetailsDto;
import it.pagopa.pn.notificationcostservice.dto.cost.basecost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.notificationcostservice.utils.CostUtils.getCostWithVat;

@Component
public class NotificationDeliveryCostMapper {

    public NotificationCostRecipientResponseDto mapDtoToResponseDto(NotificationDeliveryCostDto dto,Integer totalCost){
        return NotificationCostRecipientResponseDto.builder()
                .totalCost(mapTotalCost(dto,totalCost))
                .build();
    }

    private TotalCostDto mapTotalCost(NotificationDeliveryCostDto dto,Integer totalCost){
        return TotalCostDto.builder()
                .cost(totalCost)
                .details(mapTotalCostDetails(dto))
                .build();
    }

    private TotalCostDetailsDto mapTotalCostDetails(NotificationDeliveryCostDto dto){
        return TotalCostDetailsDto.builder()
                .baseCost(mapBaseCost(dto))
                .secondAnalogCost(mapSecondAnalogCost(dto))
                .firstAnalogCost(mapFirstAnalogCost(dto))
                .simpleRegisteredLetterCost(mapSimpleRegisteredLetterCost(dto))
                .vat(dto.getVat())
                .notificationFeePolicy(dto.getNotificationFeePolicy())
                .build();
    }

    private BaseCostDto mapBaseCost(NotificationDeliveryCostDto dto){
        return BaseCostDto.builder()
                .cost(dto.getBaseCost())
                .details(mapBaseCostDetails(dto))
                .build();
    }

    private BaseCostDetailsDto mapBaseCostDetails(NotificationDeliveryCostDto dto){
        return BaseCostDetailsDto.builder()
                .paFee(dto.getPaFee())
                .sendFee(dto.getSendFee())
                .build();
    }

    private AnalogCostDto mapFirstAnalogCost(NotificationDeliveryCostDto dto){
        return AnalogCostDto.builder()
//                .productType()
                .cost(getCostWithVat(dto.getFirstAnalogCost(), dto.getVat()))
                .build();
    }

    private AnalogCostDto mapSecondAnalogCost(NotificationDeliveryCostDto dto){
        return AnalogCostDto.builder()
//                .productType()
                .cost(getCostWithVat(dto.getFirstAnalogCost(), dto.getVat()))
                .build();
    }

    private AnalogCostDto mapSimpleRegisteredLetterCost(NotificationDeliveryCostDto dto){
        return AnalogCostDto.builder()
//                .productType()
                .cost(getCostWithVat(dto.getSimpleRegisteredLetterCost(), dto.getVat()))
                .build();
    }

}
