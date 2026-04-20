package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseCostEntity {
    public static final String COL_SEND_FEE = "sendFee";
    public static final String COL_PA_FEE = "paFee";

    private Integer sendFee;
    private Integer paFee;
}
