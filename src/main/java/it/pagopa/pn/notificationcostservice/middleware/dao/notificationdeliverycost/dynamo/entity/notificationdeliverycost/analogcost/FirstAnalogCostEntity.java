package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost;

import lombok.*;
import lombok.experimental.SuperBuilder;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@EqualsAndHashCode(callSuper = true)
@DynamoDbBean
@Data
@SuperBuilder
public class FirstAnalogCostEntity extends AnalogCostEntity {

    public FirstAnalogCostEntity() {
        super();
    }
}
