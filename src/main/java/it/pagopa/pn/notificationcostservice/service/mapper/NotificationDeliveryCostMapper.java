package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.dto.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.AnalogCostDto;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class NotificationDeliveryCostMapper {

    public NotificationCostRecipientResponse mapDtoToResponse(NotificationDeliveryCostDto dto, CalculatedCosts calculatedCosts) {
        return NotificationCostRecipientResponse.builder()
                .totalCost(mapTotalCost(dto, calculatedCosts))
                .lastUpdate(dto.getLastUpdate())
                .pagoPaIntMode(dto.getPagoPaIntMode() != null ? PagoPaIntMode.fromValue(dto.getPagoPaIntMode().name()) : null)
                .build();
    }

    private TotalCost mapTotalCost(NotificationDeliveryCostDto dto, CalculatedCosts calculatedCosts) {
        return TotalCost.builder()
                .costWithVat(calculatedCosts.getTotalCostWithVat())
                .details(mapTotalCostDetails(dto, calculatedCosts))
                .build();
    }

    private TotalCostDetails mapTotalCostDetails(NotificationDeliveryCostDto dto, CalculatedCosts calculatedCosts) {
        AnalogCostDto firstAnalogCost = dto.getFirstAnalogCost() != null ? dto.getFirstAnalogCost() : dto.getSimpleRegisteredLetterCost();

        return TotalCostDetails.builder()
                .baseCost(mapBaseCost(dto, calculatedCosts))
                .firstAnalogCost(mapAnalogCost(firstAnalogCost))
                .secondAnalogCost(mapAnalogCost(dto.getSecondAnalogCost()))
                .vat(dto.getVat())
                .notificationFeePolicy(dto.getNotificationFeePolicy() != null ? NotificationFeePolicy.fromValue(dto.getNotificationFeePolicy().name()) : null)
                .build();
    }

    private BaseCost mapBaseCost(NotificationDeliveryCostDto dto, CalculatedCosts calculatedCosts) {
        return BaseCost.builder()
                .cost(calculatedCosts.getBaseCost())
                .details(mapBaseCostDetails(dto))
                .build();
    }

    private BaseCostDetails mapBaseCostDetails(NotificationDeliveryCostDto dto) {
        return BaseCostDetails.builder()
                .paFee(mapPaFee(dto.getPaFee()))
                .sendFee(mapSendFee(dto.getSendFee()))
                .build();
    }

    private PaFee mapPaFee(Integer paFee) {
        return PaFee.builder()
                .cost(paFee)
                .includedInVatBase(false)
                .build();
    }

    private SendFee mapSendFee(Integer sendFee) {
        return SendFee.builder()
                .cost(sendFee)
                .includedInVatBase(false)
                .build();
    }

    private AnalogCost mapAnalogCost(AnalogCostDto dto) {
        if(Objects.isNull(dto)){
            return null;
        }
        return AnalogCost.builder()
                .productType(dto.getProductType())
                .cost(dto.getCost())
                .includedInVatBase(true)
                .build();
    }
}
