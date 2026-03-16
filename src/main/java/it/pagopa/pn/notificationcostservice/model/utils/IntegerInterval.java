package it.pagopa.pn.notificationcostservice.model.utils;

public record IntegerInterval(int min, int max) {

    public IntegerInterval(int min, int max) {
        this.min = min;
        this.max = max;

        if (this.min > this.max) {
            throw new IllegalArgumentException("min value must be less than or equal to max value");
        }
    }
}