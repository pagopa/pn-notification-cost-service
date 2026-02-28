package it.pagopa.pn.notificationcostservice.dto.cost;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CalculatedCosts {
    private Integer totalCostWithVat;
    private Integer analogCost;
    private Integer baseCost;
    private Integer vat;
}
