package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost;

import lombok.*;
import lombok.experimental.SuperBuilder;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder(toBuilder = true)
public class AnalogCostDto {
    private Integer cost;
    private String productType;
}
