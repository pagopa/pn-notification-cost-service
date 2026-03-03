package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost;

import lombok.*;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class BaseCostDto {
    private Integer sendFee;
    private Integer paFee;
}
