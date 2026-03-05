package it.pagopa.pn.notificationcostservice.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class PnNotificationDeliveryCostBadRequestExceptionTest {
    @Test
    void pnBadRequestExceptionConstructorTest() {
        PnNotificationDeliveryCostBadRequestException exception = new PnNotificationDeliveryCostBadRequestException("Bad Request","Bad Request","");

        Assertions.assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST.value());
    }
}
