package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDto;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCostRecipientResponseDto {
    private TotalCostDto totalCost;
    private PagoPaIntMode pagoPaIntMode;
    private Instant lastUpdate;
}
