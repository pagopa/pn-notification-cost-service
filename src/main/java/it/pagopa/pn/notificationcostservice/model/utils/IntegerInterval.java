package it.pagopa.pn.notificationcostservice.model.utils;

import lombok.Getter;
@Getter
public class IntegerInterval {

    private final int min;
    private final int max;

    public IntegerInterval(int min, int max) {
        this.min = min;
        this.max = max;

        if (this.min > this.max) {
            throw new IllegalArgumentException("min value must be less than or equal to max value");
        }
    }
}