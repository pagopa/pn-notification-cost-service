package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.exception.PnDbConflictException;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.paymentinfo.DtoToEntityPaymentInfoMapper;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.paymentinfo.EntityToDtoPaymentInfoMapper;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class PaymentInfoDaoDynamo extends BaseDao implements PaymentInfoDao {

    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    DynamoDbAsyncTable<PaymentInfoEntity> paymentInfoEntityDynamoTable;
    DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfo;
    EntityToDtoPaymentInfoMapper entityToDtoPaymentInfoMapper;

    public PaymentInfoDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                             PnNotificationCostServiceConfigs awsConfigs, DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfo,EntityToDtoPaymentInfoMapper entityToDtoPaymentInfoMapper) {
        this.paymentInfoEntityDynamoTable = dynamoDbEnhancedAsyncClient.table(awsConfigs.getPaymentInfoTable().getTableName(), TableSchema.fromBean(PaymentInfoEntity.class));
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.dtoToEntityPaymentInfo = dtoToEntityPaymentInfo;
        this.entityToDtoPaymentInfoMapper = entityToDtoPaymentInfoMapper;
    }

    @Override
    public Mono<List<PaymentInfo>> updateItem(List<PaymentInfo> payments) {
        if (payments == null || payments.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        return Flux.fromIterable(payments)
                .flatMap(paymentDto -> {
                    PaymentInfoEntity entity = dtoToEntityPaymentInfo.dtoToEntity(paymentDto);
                    return Mono.fromFuture(paymentInfoEntityDynamoTable.updateItem(createUpdateItemEnhancedRequest(entity)))
                            .map(entityToDtoPaymentInfoMapper::entityToDto)
                            .doOnError(e -> log.error("Error updating item with IUV: {}", entity.getIuv(), e))
                            .onErrorMap(ConditionalCheckFailedException.class, e -> new PnDbConflictException(e.getMessage()));
                })
                .collectList();
    }

    private UpdateItemEnhancedRequest<PaymentInfoEntity> createUpdateItemEnhancedRequest(PaymentInfoEntity entity) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#pk", "pk");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":pk", AttributeValue.builder().s(entity.getIuv()).build());

        return UpdateItemEnhancedRequest.builder(PaymentInfoEntity.class)
                .item(entity)
                .conditionExpression(expressionBuilder(expressionValues, expressionNames))
                .build();
    }
}
