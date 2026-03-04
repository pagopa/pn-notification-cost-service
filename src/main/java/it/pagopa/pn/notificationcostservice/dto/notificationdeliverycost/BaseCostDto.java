package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.notificationcostservice.utils.DomainValidationUtils.validatePositiveIntField;

@ToString
@Data
public class BaseCostDto {
    private int sendFee;
    private int paFee;

    @Builder
    public BaseCostDto(Integer sendFee, Integer paFee) {
        List<String> violations = new ArrayList<>();
        validatePositiveIntField(sendFee, "sendFee", violations);
        validatePositiveIntField(paFee, "paFee", violations);
        if (!violations.isEmpty()) {
            throw new PnDomainObjectValidationException(violations, this.getClass().getName());
        }

        this.sendFee = sendFee;
        this.paFee = paFee;
    }
}
