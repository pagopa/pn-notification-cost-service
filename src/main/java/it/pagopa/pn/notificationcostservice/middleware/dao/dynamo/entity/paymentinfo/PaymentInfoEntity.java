package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentInfoEntity {
    public static final String COL_PK = "pk";
    public static final String COL_REC_INDEX = "recIndex";
    public static final String COL_APPLY_COST = "applyCost";
    public static final String COL_IUN = "iun";

    private String iuv;
    private Integer recIndex;
    private boolean applyCost;
    private String iun;

    @DynamoDbPartitionKey
    @DynamoDbAttribute(COL_PK)
    public String getIuv() {
        return iuv;
    }

    @DynamoDbAttribute(COL_IUN)
    public String getIun() {
        return iun;
    }

    @DynamoDbAttribute(COL_REC_INDEX)
    public Integer getRecIndex() {
        return recIndex;
    }

    @DynamoDbAttribute(COL_APPLY_COST)
    public boolean getApplyCost() {
        return applyCost;
    }
}
