package it.pagopa.pn.notificationcostservice.model.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ValueRange {
    /*
     * Min and Max represent the range of values
     */
    MIN(0),
    MAX(100);

    private final int value;
}
