package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import it.pagopa.pn.notificationcostservice.model.CommunicationType;
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
    private Boolean applyCost;
    private CommunicationType communicationType;

    /**
     * Returns the communication type, defaulting to LEGAL if it is null.
     *
     * @return the communication type
     */
    public CommunicationType getCommunicationType() {
        return this.communicationType == null ? CommunicationType.LEGAL : this.communicationType;
    }
}
