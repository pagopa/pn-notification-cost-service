package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDto;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCostRecipientResponseDto {
    private TotalCostDto totalCost;
    private PagoPaIntMode pagoPaIntMode;
}
