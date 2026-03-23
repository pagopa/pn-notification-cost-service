package it.pagopa.pn.notificationcostservice.model.cost;

import lombok.Getter;

@Getter
public enum CostUpdatePhaseInt {
    VALIDATION("VALIDATION");
    private final String value;

    CostUpdatePhaseInt(String value) {
        this.value = value;
    }
}
