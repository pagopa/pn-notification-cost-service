package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import it.pagopa.pn.notificationcostservice.exception.PnDomainObjectValidationException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.notificationcostservice.utils.DomainValidationUtils.validateNonNullableField;
import static it.pagopa.pn.notificationcostservice.utils.DomainValidationUtils.validatePositiveIntField;

@Data
@EqualsAndHashCode
@ToString
public class NotificationDeliveryCostDto {
    private String iun;
    private int recIndex;
    private String recipientInternalId;
    private String senderInternalId;
    private BaseCostDto baseCost;
    private FirstAnalogCostDto firstAnalogCost;
    private SecondAnalogCostDto secondAnalogCost;
    private SimpleRegisteredLetterCostDto simpleRegisteredLetterCost;
    private Boolean isDeleted;
    private NotificationFeePolicy notificationFeePolicy;
    private PagoPaIntMode pagoPaIntMode;
    private int vat;
    private Instant lastUpdate;
    private Long ttl;

    @Builder
    private NotificationDeliveryCostDto(String iun, int recIndex, String recipientInternalId, String senderInternalId, BaseCostDto baseCost, FirstAnalogCostDto firstAnalogCost, SecondAnalogCostDto secondAnalogCost, SimpleRegisteredLetterCostDto simpleRegisteredLetterCost, Boolean isDeleted, NotificationFeePolicy notificationFeePolicy, PagoPaIntMode pagoPaIntMode, int vat, Instant lastUpdate, Long ttl) {
        List<String> violations = new ArrayList<>();
        validateNonNullableField(iun, "iun", violations);
        validateNonNullableField(baseCost, "baseCost", violations);
        validateNonNullableField(notificationFeePolicy, "notificationFeePolicy", violations);
        validateNonNullableField(pagoPaIntMode, "pagoPaIntMode", violations);
        validateAnalogCosts(firstAnalogCost, simpleRegisteredLetterCost, violations);
        validatePositiveIntField(vat, "vat", violations);

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

    private void validateAnalogCosts(FirstAnalogCostDto firstAnalogCost, SimpleRegisteredLetterCostDto simpleRegisteredLetterCost, List<String> violations) {
        if(Objects.nonNull(firstAnalogCost) && Objects.nonNull(simpleRegisteredLetterCost)) {
            violations.add("Only one between firstAnalogCost and simpleRegisteredLetterCost can be set");
        }
    }
}
