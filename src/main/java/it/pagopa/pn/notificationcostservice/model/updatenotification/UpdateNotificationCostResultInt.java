package it.pagopa.pn.notificationcostservice.model.updatenotification;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class UpdateNotificationCostResultInt {
    private int recIndex;
    private String iuv;
    private CommunicationResultGroupInt result;
}