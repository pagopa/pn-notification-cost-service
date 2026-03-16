package it.pagopa.pn.notificationcostservice.model.utils;

public record IntegerInterval(int min, int max) {

    public IntegerInterval {
        if (min > max) {
            throw new IllegalArgumentException(
                    String.format("Invalid interval: min (%d) cannot be greater than max (%d)", min, max));
        }
    }
}