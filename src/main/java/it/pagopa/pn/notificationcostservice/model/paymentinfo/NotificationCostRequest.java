package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import lombok.*;

import java.util.List;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCostRequest {
    private List<RecipientCostData> recipients;
}