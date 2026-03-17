package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import lombok.Getter;

@Getter
public enum NotificationCostInitializationEventType {

    NOTIFICATION_COST_INITIALIZATION("notification_cost_init");

    private final String value;
    NotificationCostInitializationEventType(String value) {
        this.value = value;
    }

}
