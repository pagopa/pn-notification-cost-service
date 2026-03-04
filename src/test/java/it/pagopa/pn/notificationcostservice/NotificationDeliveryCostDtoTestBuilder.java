package it.pagopa.pn.notificationcostservice;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;

import java.time.Instant;

/**
 * Test builder for NotificationDeliveryCostDto.
 * Provides a fluent API to create instances of NotificationDeliveryCostDto with default values for testing purposes.
 * Allows overriding specific fields as needed for different test scenarios.
 */
public class NotificationDeliveryCostDtoTestBuilder {
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

    public static NotificationDeliveryCostDtoTestBuilder builder() {
        return new NotificationDeliveryCostDtoTestBuilder();
    }

    public NotificationDeliveryCostDtoTestBuilder withIun(String iun) {
        this.iun = iun;
        return this;
    }

    public NotificationDeliveryCostDtoTestBuilder withRecIndex(int recIndex) {
        this.recIndex = recIndex;
        return this;
    }

    public NotificationDeliveryCostDtoTestBuilder withBaseCost(BaseCostDto baseCost) {
        this.baseCost = baseCost;
        return this;
    }

    public NotificationDeliveryCostDtoTestBuilder withNotificationFeePolicy(NotificationFeePolicy notificationFeePolicy) {
        this.notificationFeePolicy = notificationFeePolicy;
        return this;
    }

    public NotificationDeliveryCostDtoTestBuilder withPagoPaIntMode(PagoPaIntMode pagoPaIntMode) {
        this.pagoPaIntMode = pagoPaIntMode;
        return this;
    }

    public NotificationDeliveryCostDtoTestBuilder withVat(int vat) {
        this.vat = vat;
        return this;
    }

    public NotificationDeliveryCostDtoTestBuilder withFirstAnalogCost(FirstAnalogCostDto firstAnalogCost) {
        this.firstAnalogCost = firstAnalogCost;
        return this;
    }

    public NotificationDeliveryCostDtoTestBuilder withSecondAnalogCost(SecondAnalogCostDto secondAnalogCost) {
        this.secondAnalogCost = secondAnalogCost;
        return this;
    }

    public NotificationDeliveryCostDtoTestBuilder withSimpleRegisteredLetterCost(SimpleRegisteredLetterCostDto simpleRegisteredLetterCost) {
        this.simpleRegisteredLetterCost = simpleRegisteredLetterCost;
        return this;
    }

    public NotificationDeliveryCostDtoTestBuilder withIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
        return this;
    }

    public NotificationDeliveryCostDto build() {
        if(iun == null) {
            iun = "test-iun";
        }

        if(baseCost == null) {
            baseCost = BaseCostDto.builder().sendFee(100).paFee(50).build();
        }

        if(notificationFeePolicy == null) {
            notificationFeePolicy = NotificationFeePolicy.DELIVERY_MODE;
        }

        if(pagoPaIntMode == null) {
            pagoPaIntMode = PagoPaIntMode.SYNC;
        }

        return NotificationDeliveryCostDto.builder()
                .iun(iun)
                .recIndex(recIndex)
                .recipientInternalId(recipientInternalId)
                .senderInternalId(senderInternalId)
                .baseCost(baseCost)
                .firstAnalogCost(firstAnalogCost)
                .secondAnalogCost(secondAnalogCost)
                .simpleRegisteredLetterCost(simpleRegisteredLetterCost)
                .isDeleted(isDeleted)
                .notificationFeePolicy(notificationFeePolicy)
                .pagoPaIntMode(pagoPaIntMode)
                .vat(vat)
                .lastUpdate(lastUpdate)
                .ttl(ttl)
                .build();
    }
}
