package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationDeliveryCostDto {
    private String iun;
    private Integer recIndex;
    private String recipientInternalId;
    private Integer baseCost;
    private Integer firstAnalogCost;
    private Integer secondAnalogCost;
    private Integer simpleRegisteredLetterCost;
    private Boolean isRefused;
    private Boolean isCancelled;
    private Instant refinementDate;
    private Instant notificationViewDate;
    private Integer sendFee;
    private Integer paFee;
    private NotificationFeePolicy notificationFeePolicy;
    private PagoPaIntMode pagoPaIntMode;
    private Integer vat;
    private Instant lastUpdate;
    private Long ttl;
}
