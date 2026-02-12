package it.pagopa.pn.notificationcostservice.dto.cost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotalCostDto {
    private Integer cost;
    private TotalCostDetailsDto details;
}
