package it.pagopa.pn.notificationcostservice.dto.cost;

import it.pagopa.pn.notificationcostservice.dto.cost.analogcost.AnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.cost.basecost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
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
    private AnalogCostDto firstAnalogCost;
    private AnalogCostDto secondAnalogCost;
    private AnalogCostDto simpleRegisteredLetterCost;
    private Integer vat;
    private NotificationFeePolicy notificationFeePolicy;
}