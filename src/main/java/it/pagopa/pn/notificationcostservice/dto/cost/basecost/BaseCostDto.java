package it.pagopa.pn.notificationcostservice.dto.cost.basecost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseCostDto {
    private Integer cost;
    private BaseCostDetailsDto details;
}
