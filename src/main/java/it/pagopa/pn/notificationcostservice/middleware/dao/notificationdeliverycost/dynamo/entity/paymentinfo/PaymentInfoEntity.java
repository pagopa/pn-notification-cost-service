package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.paymentinfo;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PaymentInfoEntity {
    public static final String COL_PK = "iuv";
    public static final String COL_REC_INDEX = "recIndex";
    public static final String COL_APPLY_COST = "applyCost";
    public static final String COL_IUN = "iun";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)}))
    private String iuv;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_REC_INDEX)}))
    private Integer recIndex;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_APPLY_COST)}))
    private Boolean applyCost;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_IUN)}))
    private String iun;
}
