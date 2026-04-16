package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseCostEntity {
    public static final String COL_SEND_FEE = "sendFee";
    public static final String COL_PA_FEE = "paFee";

    private Integer sendFee;
    private Integer paFee;

    @DynamoDbAttribute(COL_SEND_FEE)
    public Integer getSendFee() {
        return sendFee;
    }

    @DynamoDbAttribute(COL_PA_FEE)
    public Integer getPaFee() {
        return paFee;
    }
}
