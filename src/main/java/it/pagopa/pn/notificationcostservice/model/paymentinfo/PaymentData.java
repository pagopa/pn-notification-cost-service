package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import lombok.*;

@Data
@Builder
@ToString
public class PaymentData {
    @NonNull
    private String iuv;
}

