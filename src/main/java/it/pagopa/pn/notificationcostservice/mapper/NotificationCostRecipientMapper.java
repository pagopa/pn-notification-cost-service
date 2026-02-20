package it.pagopa.pn.notificationcostservice.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponse;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.TotalCost;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.TotalCostDetails;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.BaseCost;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.BaseCostDetails;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogCost;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.AnalogCostDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCostRecipientMapper {

    public NotificationCostRecipientResponse dtoResponse2Response(NotificationCostRecipientResponseDto dto) {
        if (dto == null) {
            return null;
        }

        NotificationCostRecipientResponse response = new NotificationCostRecipientResponse();

        // Map totalCost
        if (dto.getTotalCost() != null) {
            TotalCost totalCost = new TotalCost();
            totalCost.setCost(dto.getTotalCost().getCost());

            // Map details
            if (dto.getTotalCost().getDetails() != null) {
                TotalCostDetails details = new TotalCostDetails();

                // Map baseCost
                details.setBaseCost(mapBaseCost(dto.getTotalCost().getDetails().getBaseCost()));

                // Map firstAnalogCost
                details.setFirstAnalogCost(mapAnalogCost(dto.getTotalCost().getDetails().getFirstAnalogCost()));

                // Map secondAnalogCost
                details.setSecondAnalogCost(mapAnalogCost(dto.getTotalCost().getDetails().getSecondAnalogCost()));

                // Map simpleRegisteredLetterCost
                details.setSimpleRegisteredLetterCost(mapAnalogCost(dto.getTotalCost().getDetails().getSimpleRegisteredLetterCost()));

                // Map vat
                details.setVat(dto.getTotalCost().getDetails().getVat());

                // Map notificationFeePolicy enum
                if (dto.getTotalCost().getDetails().getNotificationFeePolicy() != null) {
                    details.setNotificationFeePolicy(TotalCostDetails.NotificationFeePolicyEnum.fromValue(
                        dto.getTotalCost().getDetails().getNotificationFeePolicy().name()));
                }

                totalCost.setDetails(details);
            }

            response.setTotalCost(totalCost);
        }

        // Map pagoPaIntMode enum
        if (dto.getPagoPaIntMode() != null) {
            response.setPagoPaIntMode(NotificationCostRecipientResponse.PagoPaIntModeEnum.fromValue(
                dto.getPagoPaIntMode().name()));
        }
        if(dto.getLastUpdate() != null) {
            response.setLastUpdate(Date.from(dto.getLastUpdate()));
        }

        return response;
    }

    private BaseCost mapBaseCost(BaseCostDto baseCostDto) {
        if (baseCostDto == null) {
            return null;
        }

        BaseCost baseCost = new BaseCost();
        Integer paFee = baseCostDto.getPaFee();
        Integer sendFee = baseCostDto.getSendFee();
        if (paFee != null && sendFee != null) {
            int baseCostValue = paFee + sendFee;
            baseCost.setCost(baseCostValue);
        }

        BaseCostDetails details = new BaseCostDetails();
        details.setPaFee(paFee);
        details.setSendFee(sendFee);
        baseCost.setDetails(details);

        return baseCost;
    }

    private AnalogCost mapAnalogCost(AnalogCostDto analogCostDto) {
        if (analogCostDto == null) {
            return null;
        }

        AnalogCost analogCost = new AnalogCost();
        analogCost.setCost(analogCostDto.getCost());
        analogCost.setProductType(analogCostDto.getProductType());

        return analogCost;
    }

}
