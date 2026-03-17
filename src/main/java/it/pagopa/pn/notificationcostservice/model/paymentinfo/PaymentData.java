package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import lombok.*;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PaymentData {
    private String iuv;
    private Boolean applyCost;
}