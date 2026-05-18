package it.pagopa.pn.notificationcostservice.model.cost;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationCostUpdate {
    private String iun;
    private Integer recIndex;
    private Integer cost;
    private String productType;
    private CostUpdatePhaseInt costUpdatePhase;
    private Instant elementTimestamp;
    private boolean invalidationFlow;
}
