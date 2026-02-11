package it.pagopa.pn.notificationcostservice.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class PnNotFoundExceptionTest {
    @Test
    void pnNotFoundExceptionConstructorTest() {
        PnNotFoundException exception = new PnNotFoundException("Not Found","Not found","");

        Assertions.assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND.value());
    }
}
