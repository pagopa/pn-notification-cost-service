package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDetailsDto;
import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static it.pagopa.pn.notificationcostservice.utils.CostUtils.getCostWithVat;

@Component
public class NotificationDeliveryCostMapper {

    public NotificationCostRecipientResponseDto mapDtoToResponseDto(NotificationDeliveryCostDto dto, Integer totalCost) {
        return NotificationCostRecipientResponseDto.builder()
                .totalCost(mapTotalCost(dto, totalCost))
                .lastUpdate(dto.getLastUpdate())
                .pagoPaIntMode(dto.getPagoPaIntMode())
                .build();
    }

    private TotalCostDto mapTotalCost(NotificationDeliveryCostDto dto, Integer totalCost) {
        return TotalCostDto.builder()
                .cost(totalCost)
                .details(mapTotalCostDetails(dto))
                .build();
    }

    private TotalCostDetailsDto mapTotalCostDetails(NotificationDeliveryCostDto dto) {
        return TotalCostDetailsDto.builder()
                .baseCost(mapBaseCost(dto))
                .secondAnalogCost(mapSecondAnalogCost(dto))
                .firstAnalogCost(mapFirstAnalogCost(dto))
                .simpleRegisteredLetterCost(mapSimpleRegisteredLetterCost(dto))
                .vat(dto.getVat())
                .notificationFeePolicy(dto.getNotificationFeePolicy())
                .build();
    }

    private BaseCostDto mapBaseCost(NotificationDeliveryCostDto dto) {
        return BaseCostDto.builder()
                .paFee(dto.getPaFee())
                .sendFee(dto.getSendFee())
                .build();
    }


    private FirstAnalogCostDto mapFirstAnalogCost(NotificationDeliveryCostDto dto) {
        if(Objects.isNull(dto.getFirstAnalogCost())){
            return null;
        }
        return FirstAnalogCostDto.builder()
                .productType(dto.getFirstAnalogCost().getProductType())
                .cost(getCostWithVat(dto.getFirstAnalogCost().getCost(), dto.getVat()))
                .build();
    }

    private SecondAnalogCostDto mapSecondAnalogCost(NotificationDeliveryCostDto dto) {
        if(Objects.isNull(dto.getSecondAnalogCost())){
            return null;
        }
        return SecondAnalogCostDto.builder()
                .productType(dto.getSecondAnalogCost().getProductType())
                .cost(getCostWithVat(dto.getSecondAnalogCost().getCost(), dto.getVat()))
                .build();
    }

    private SimpleRegisteredLetterCostDto mapSimpleRegisteredLetterCost(NotificationDeliveryCostDto dto) {
        if(Objects.isNull(dto.getSimpleRegisteredLetterCost())){
            return null;
        }
        return SimpleRegisteredLetterCostDto.builder()
                .productType(dto.getSimpleRegisteredLetterCost().getProductType())
                .cost(getCostWithVat(dto.getSimpleRegisteredLetterCost().getCost(), dto.getVat()))
                .build();
    }
}
