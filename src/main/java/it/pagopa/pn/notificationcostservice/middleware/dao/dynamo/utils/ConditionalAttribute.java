package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.utils;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public sealed interface ConditionalAttribute {

    /** Valore da scrivere con SET. */
    AttributeValue value();

    /** Il record esistente deve contenere esattamente questo valore. */
    record Exact(AttributeValue value) implements ConditionalAttribute {}

    /**
     * Il record esistente deve contenere questo valore. Se l'attributo manca
     * (record precedente all'introduzione del campo) viene accettato solo se
     * il valore richiesto coincide con quello implicito dell'assenza.
     */
    record TreatMissingAs(AttributeValue value, AttributeValue assumedWhenMissing)
            implements ConditionalAttribute {}

    static ConditionalAttribute exact(AttributeValue value) {
        return new Exact(value);
    }

    static ConditionalAttribute treatMissingAs(AttributeValue value, AttributeValue assumedWhenMissing) {
        return new TreatMissingAs(value, assumedWhenMissing);
    }
}