package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@EqualsAndHashCode(callSuper = true)
@DynamoDbBean
@Data
@ToString(callSuper = true)
@SuperBuilder
public class SimpleRegisteredLetterCostEntity extends AnalogCostEntity {

    public SimpleRegisteredLetterCostEntity() {
        super();
    }
}
