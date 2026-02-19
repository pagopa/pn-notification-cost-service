package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationDeliveryCostDto {
    private String iun;
    private Integer recIndex;
    private String recipientInternalId;
    private String senderInternalId;
    private BaseCostDto baseCost;
    private FirstAnalogCostDto firstAnalogCost;
    private SecondAnalogCostDto secondAnalogCost;
    private SimpleRegisteredLetterCostDto simpleRegisteredLetterCost;
    private Boolean isDeleted;
    private Integer sendFee;
    private Integer paFee;
    private NotificationFeePolicy notificationFeePolicy;
    private PagoPaIntMode pagoPaIntMode;
    private Integer vat;
    private Instant lastUpdate;
    private Long ttl;
}
