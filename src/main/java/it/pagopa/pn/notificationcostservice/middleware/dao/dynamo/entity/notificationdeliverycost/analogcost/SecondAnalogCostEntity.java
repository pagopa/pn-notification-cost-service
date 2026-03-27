package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@EqualsAndHashCode(callSuper = true)
@DynamoDbBean
@Data
@SuperBuilder
public class SecondAnalogCostEntity extends AnalogCostEntity {

    public SecondAnalogCostEntity() {
        super();
    }
}

