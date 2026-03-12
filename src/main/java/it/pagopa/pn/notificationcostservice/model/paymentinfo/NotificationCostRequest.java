package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@Builder
public class NotificationCostRequest {
    private List<RecipientCostData> recipients;
}

