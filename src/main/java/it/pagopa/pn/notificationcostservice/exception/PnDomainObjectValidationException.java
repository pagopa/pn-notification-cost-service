package it.pagopa.pn.notificationcostservice.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONCOSTSERVICE_DOMAINOBJECTVALIDATION;

@Getter
public class PnDomainObjectValidationException extends PnRuntimeException {
    private final List<String> violations;
    public PnDomainObjectValidationException(List<String> violations, String className) {
        super(
            className + " validation failed for sequent reasons: " + String.join(", ", violations),
            "Domain object validation failed",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ERROR_CODE_NOTIFICATIONCOSTSERVICE_DOMAINOBJECTVALIDATION,
            null,
            null
        );
        this.violations = violations;
    }
}
