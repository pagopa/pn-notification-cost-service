package it.pagopa.pn.notificationcostservice.model.cost;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CalculatedCosts {
    private Integer totalCostWithVat;
    private Integer analogCost;
    private Integer analogCostWithVat;
    private Integer baseCost;
    private Integer vat;
}
