package it.pagopa.pn.notificationcostservice.dto.cost;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotalCostDetailsDto {
    private BaseCostDto baseCost;
    private FirstAnalogCostDto firstAnalogCost;
    private SecondAnalogCostDto secondAnalogCost;
    private SimpleRegisteredLetterCostDto simpleRegisteredLetterCost;
    private Integer vat;
    private NotificationFeePolicy notificationFeePolicy;
}