package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import it.pagopa.pn.api.dto.events.CommunicationType;
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
    private CommunicationType communicationType;
}
