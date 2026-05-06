package it.pagopa.pn.notificationcostservice.model.updatenotification;

import lombok.*;

import java.time.Instant;

@Data
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNotificationCostRequestInt {
    private Integer notificationStepCost;
    private Instant eventTimestamp;
    private Instant eventStorageTimestamp;
    private Instant notificationSentAt;
    private UpdateCostPhaseInt updateCostPhase;
}
