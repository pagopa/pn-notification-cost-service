package it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost;

import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.notificationcostservice.utils.DomainValidationUtils.validatePositiveIntField;

@ToString
@Data
@NoArgsConstructor
public class AnalogCost {
    private int cost;
    private String productType;

    public AnalogCost(Integer cost, String productType) {
        List<String> violations = new ArrayList<>();
        validatePositiveIntField(cost, "cost", violations);
        if(!violations.isEmpty()) {
             throw new PnDomainObjectValidationException(violations, this.getClass().getName());
        }
        this.cost = cost;
        this.productType = productType;
    }
}
