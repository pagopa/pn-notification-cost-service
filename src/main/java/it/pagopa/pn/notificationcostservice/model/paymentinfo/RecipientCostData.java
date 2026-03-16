package it.pagopa.pn.notificationcostservice.model.paymentinfo;

import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import lombok.*;

import java.util.List;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecipientCostData {
    private Integer recIndex;
    private String recipientInternalId;
    private String senderInternalId;
    private List<PaymentData> payments;
    private Integer baseCost;
    private Integer sendFee;
    private Integer paFee;
    private NotificationFeePolicy notificationFeePolicy;
    private PagoPaIntMode pagoPaIntMode;
    private Integer vat;
}
