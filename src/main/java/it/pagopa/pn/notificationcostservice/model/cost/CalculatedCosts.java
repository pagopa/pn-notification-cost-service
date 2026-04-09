package it.pagopa.pn.notificationcostservice.model.cost;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CalculatedCosts {
    private int totalCostWithVat;
    private int analogCost;
    private int analogCostWithVat;
    private int baseCost;
    private int vat;
}
