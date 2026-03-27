package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import lombok.*;

@Data
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    private String iuv;
    private String iun;
    private Integer recIndex;
    private boolean applyCost;
}
