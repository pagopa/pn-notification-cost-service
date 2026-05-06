package it.pagopa.pn.notificationcostservice.model.updatenotification;

import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNotificationCostResponseInt {
    private List<UpdateNotificationCostResultInt> updateNotificationCostResultIntList;
}
