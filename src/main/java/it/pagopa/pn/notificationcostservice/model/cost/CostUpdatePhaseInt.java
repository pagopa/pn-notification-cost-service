package it.pagopa.pn.notificationcostservice.model.cost;

import lombok.Getter;

@Getter
public enum CostUpdatePhaseInt {
    VALIDATION("VALIDATION"),
    SEND_ANALOG_DOMICILE_ATTEMPT_0("SEND_ANALOG_DOMICILE_ATTEMPT_0"),
    SEND_ANALOG_DOMICILE_ATTEMPT_1("SEND_ANALOG_DOMICILE_ATTEMPT_1"),
    SEND_SIMPLE_REGISTERED_LETTER("SEND_SIMPLE_REGISTERED_LETTER"),
    REQUEST_REFUSED("REQUEST_REFUSED"),
    NOTIFICATION_CANCELLED("NOTIFICATION_CANCELLED");
    private final String value;

    CostUpdatePhaseInt(String value) {
        this.value = value;
    }
}
