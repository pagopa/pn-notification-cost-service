package it.pagopa.pn.notificationcostservice.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PnDbConflictExceptionTest {
    @Test
    void pnPnDbConflictExceptionTest() {
        PnDbConflictException exception = new PnDbConflictException("Conflict");
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("Conflict", exception.getMessage());
    }
}
