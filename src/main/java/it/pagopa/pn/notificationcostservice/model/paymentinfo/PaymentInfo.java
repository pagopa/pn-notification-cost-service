package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
@Builder
public class PaymentInfo {
    private String iuv;
    private String iun;
    private Integer recIndex;
    private Boolean applyCost;
}
