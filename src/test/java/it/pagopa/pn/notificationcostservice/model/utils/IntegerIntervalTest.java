package it.pagopa.pn.notificationcostservice.model.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegerIntervalTest {
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

    @ParameterizedTest
    @CsvSource({
            "10, 5",
            "1, 0",
            "0, -1"
    })
    void throwsExceptionWhenMinIsGreaterThanMax(int min, int max) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerInterval(min, max));
        assertEquals("Invalid interval: min (" + min + ") cannot be greater than max (" + max + ")", exception.getMessage());
    }
}
