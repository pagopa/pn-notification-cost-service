package it.pagopa.pn.notificationcostservice.model.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IntegerIntervalTest {
    @ParameterizedTest
    @CsvSource({
            "0, 100",
            "5, 5",
            "-10, 10"
    })
    void createsIntervalWhenMinIsLessThanOrEqualToMax(int min, int max) {
        IntegerInterval interval = new IntegerInterval(min, max);
        assertEquals(min, interval.min());
        assertEquals(max, interval.max());
    }

    @Test
    void throwsExceptionWhenMinIsGreaterThanMax() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerInterval(10, 5));
        assertEquals("min value must be less than or equal to max value", exception.getMessage());
    }
}
