package it.pagopa.pn.notificationcostservice.model.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.model.utils.IntegerInterval;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import it.pagopa.pn.notificationcostservice.model.utils.ValueRange;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.notificationcostservice.utils.DomainValidationUtils.*;

@Data
@EqualsAndHashCode
@ToString
public class NotificationDeliveryCost {
    private String iun;
    private int recIndex;
    private String recipientInternalId;
    private String senderInternalId;
    private BaseCost baseCost;
    private FirstAnalogCost firstAnalogCost;
    private SecondAnalogCost secondAnalogCost;
    private SimpleRegisteredLetterCost simpleRegisteredLetterCost;
    private Boolean isDeleted;
    private NotificationFeePolicy notificationFeePolicy;
    private PagoPaIntMode pagoPaIntMode;
    private int vat;
    private Instant lastUpdate;
    private Long ttl;

    @Builder
    private NotificationDeliveryCost(String iun, int recIndex, String recipientInternalId, String senderInternalId, BaseCost baseCost, FirstAnalogCost firstAnalogCost, SecondAnalogCost secondAnalogCost, SimpleRegisteredLetterCost simpleRegisteredLetterCost, Boolean isDeleted, NotificationFeePolicy notificationFeePolicy, PagoPaIntMode pagoPaIntMode, int vat, Instant lastUpdate, Long ttl) {
        List<String> violations = new ArrayList<>();
        validateNonNullableField(iun, "iun", violations);
        validateNonNullableField(baseCost, "baseCost", violations);
        validateNonNullableField(notificationFeePolicy, "notificationFeePolicy", violations);
        validateNonNullableField(pagoPaIntMode, "pagoPaIntMode", violations);
        validateAnalogCosts(firstAnalogCost, simpleRegisteredLetterCost, violations);
        validateIntervalIntField(vat, "vat", violations, new IntegerInterval(ValueRange.MIN.getValue(),
                ValueRange.MAX.getValue()));

        if (!violations.isEmpty()) {
            throw new PnDomainObjectValidationException(violations, this.getClass().getName());
        }

        this.iun = iun;
        this.recIndex = recIndex;
        this.recipientInternalId = recipientInternalId;
        this.senderInternalId = senderInternalId;
        this.baseCost = baseCost;
        this.firstAnalogCost = firstAnalogCost;
        this.secondAnalogCost = secondAnalogCost;
        this.simpleRegisteredLetterCost = simpleRegisteredLetterCost;
        this.isDeleted = isDeleted;
        this.notificationFeePolicy = notificationFeePolicy;
        this.pagoPaIntMode = pagoPaIntMode;
        this.vat = vat;
        this.lastUpdate = lastUpdate;
        this.ttl = ttl;
    }

    private void validateAnalogCosts(FirstAnalogCost firstAnalogCost, SimpleRegisteredLetterCost simpleRegisteredLetterCost, List<String> violations) {
        if(Objects.nonNull(firstAnalogCost) && Objects.nonNull(simpleRegisteredLetterCost)) {
            violations.add("Only one between firstAnalogCost and simpleRegisteredLetterCost can be set");
        }
    }
}
