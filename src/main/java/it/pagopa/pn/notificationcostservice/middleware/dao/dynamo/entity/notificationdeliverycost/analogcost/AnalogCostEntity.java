package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class AnalogCostEntity {

    public static final String COL_COST = "cost";
    public static final String COL_PRODUCT_TYPE = "productType";

    private Integer cost;
    private String productType;

    @DynamoDbAttribute(COL_PRODUCT_TYPE)
    public String getProductType() {
        return productType;
    }

    @DynamoDbAttribute(COL_COST)
    public Integer getCost() {
        return cost;
    }
}
