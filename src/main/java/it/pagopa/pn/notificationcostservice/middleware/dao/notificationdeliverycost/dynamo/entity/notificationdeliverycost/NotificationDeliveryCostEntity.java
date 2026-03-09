package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostEntity;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NotificationDeliveryCostEntity {

    public static final String COL_PK = "pk";
    public static final String COL_SK = "sk";
    public static final String COL_RECIPIENT_INTERNAL_ID = "recipientInternalId";
    public static final String COL_SENDER_INTERNAL_ID = "senderInternalId";
    public static final String COL_BASE_COST = "baseCost";
    public static final String COL_FIRST_ANALOG_COST = "firstAnalogCost";
    public static final String COL_SECOND_ANALOG_COST = "secondAnalogCost";
    public static final String COL_SIMPLE_REGISTERED_LETTER_COST = "simpleRegisteredLetterCost";
    public static final String COL_IS_DELETED = "isDeleted";
    public static final String COL_SEND_FEE = "sendFee";
    public static final String COL_PA_FEE = "paFee";
    public static final String COL_NOTIFICATION_FEE_POLICY = "notificationFeePolicy";
    public static final String COL_PAGO_PA_INT_MODE = "pagoPaIntMode";
    public static final String COL_VAT = "vat";
    public static final String COL_LAST_UPDATE = "lastUpdate";
    public static final String COL_TTL = "ttl";


    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)}))
    private String iun;

    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))
    private Integer recIndex;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_RECIPIENT_INTERNAL_ID)}))
    private String recipientInternalId;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_SENDER_INTERNAL_ID)}))
    private String senderInternalId;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_BASE_COST)}))
    private BaseCostEntity baseCost;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_FIRST_ANALOG_COST)}))
    private FirstAnalogCostEntity firstAnalogCost;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_SECOND_ANALOG_COST)}))
    private SecondAnalogCostEntity secondAnalogCost;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_SIMPLE_REGISTERED_LETTER_COST)}))
    private SimpleRegisteredLetterCostEntity simpleRegisteredLetterCost;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_IS_DELETED)}))
    private Boolean isDeleted;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_NOTIFICATION_FEE_POLICY)}))
    private NotificationFeePolicy notificationFeePolicy;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_PAGO_PA_INT_MODE)}))
    private PagoPaIntMode pagoPaIntMode;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_VAT)}))
    private Integer vat;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_LAST_UPDATE)}))
    private Instant lastUpdate;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;
}
