package it.pagopa.pn.notificationcostservice.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

public class PnNotificationDeliveryCostBadRequestException extends PnRuntimeException {
    public PnNotificationDeliveryCostBadRequestException(@NotNull String message, @NotNull String description, @NotNull String errorcode) {
        super(message, description, HttpStatus.BAD_REQUEST.value(), errorcode, null, null);
    }
}
