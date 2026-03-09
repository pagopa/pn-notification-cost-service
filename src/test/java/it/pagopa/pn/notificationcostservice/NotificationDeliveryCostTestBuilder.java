package it.pagopa.pn.notificationcostservice;

import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;

import java.time.Instant;

/**
 * Test builder for NotificationDeliveryCost.
 * Provides a fluent API to create instances of NotificationDeliveryCostDto with default values for testing purposes.
 * Allows overriding specific fields as needed for different test scenarios.
 */
public class NotificationDeliveryCostTestBuilder {
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

    public static NotificationDeliveryCostTestBuilder builder() {
        return new NotificationDeliveryCostTestBuilder();
    }

    public NotificationDeliveryCostTestBuilder withIun(String iun) {
        this.iun = iun;
        return this;
    }

    public NotificationDeliveryCostTestBuilder withRecIndex(int recIndex) {
        this.recIndex = recIndex;
        return this;
    }

    public NotificationDeliveryCostTestBuilder withBaseCost(BaseCost baseCost) {
        this.baseCost = baseCost;
        return this;
    }

    public NotificationDeliveryCostTestBuilder withNotificationFeePolicy(NotificationFeePolicy notificationFeePolicy) {
        this.notificationFeePolicy = notificationFeePolicy;
        return this;
    }

    public NotificationDeliveryCostTestBuilder withPagoPaIntMode(PagoPaIntMode pagoPaIntMode) {
        this.pagoPaIntMode = pagoPaIntMode;
        return this;
    }

    public NotificationDeliveryCostTestBuilder withVat(int vat) {
        this.vat = vat;
        return this;
    }

    public NotificationDeliveryCostTestBuilder withFirstAnalogCost(FirstAnalogCost firstAnalogCost) {
        this.firstAnalogCost = firstAnalogCost;
        return this;
    }

    public NotificationDeliveryCostTestBuilder withSecondAnalogCost(SecondAnalogCost secondAnalogCost) {
        this.secondAnalogCost = secondAnalogCost;
        return this;
    }

    public NotificationDeliveryCostTestBuilder withSimpleRegisteredLetterCost(SimpleRegisteredLetterCost simpleRegisteredLetterCost) {
        this.simpleRegisteredLetterCost = simpleRegisteredLetterCost;
        return this;
    }

    public NotificationDeliveryCostTestBuilder withIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
        return this;
    }

    public NotificationDeliveryCost build() {
        if(iun == null) {
            iun = "test-iun";
        }

        if(baseCost == null) {
            baseCost = BaseCost.builder().sendFee(100).paFee(50).build();
        }

        if(notificationFeePolicy == null) {
            notificationFeePolicy = NotificationFeePolicy.DELIVERY_MODE;
        }

        if(pagoPaIntMode == null) {
            pagoPaIntMode = PagoPaIntMode.SYNC;
        }

        return NotificationDeliveryCost.builder()
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
