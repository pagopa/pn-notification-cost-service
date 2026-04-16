package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
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
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
@Component
@Slf4j
public class PaymentInfoDaoDynamo extends BaseDao implements PaymentInfoDao {

    private static final int DELETE_ITEMS_BY_IUN_MAX_CONCURRENCY = 10;

    private final DynamoDbAsyncTable<PaymentInfoEntity> paymentInfoEntityDynamoTable;
    private final EntityToDtoPaymentInfoMapper entityToDtoPaymentInfoMapper;
    private final DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfo;

    public PaymentInfoDaoDynamo(
            DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
            DynamoDbAsyncClient dynamoDbAsyncClient,
            PnNotificationCostServiceConfigs awsConfigs,
            DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfo,
            EntityToDtoPaymentInfoMapper entityToDtoPaymentInfoMapper
    ) {
        super(dynamoDbAsyncClient, awsConfigs.getPaymentInfoTable().getTableName());
        this.paymentInfoEntityDynamoTable = initializeTable(
                awsConfigs.getPaymentInfoTable().getTableName(),
                dynamoDbEnhancedAsyncClient
        );
        this.dtoToEntityPaymentInfo = dtoToEntityPaymentInfo;
        this.entityToDtoPaymentInfoMapper = entityToDtoPaymentInfoMapper;
    }

    @Override
    public Mono<Void> updateItemIfNotExistsOrMatch(List<PaymentInfo> payments) {
        return Flux.fromIterable(payments)
                .map(dtoToEntityPaymentInfo::dtoToEntity)
                .flatMap(this::createUpdateItemRequestAndPerformUpdate)
                .then();
    }

    @Override
    public Mono<PaymentInfo> getPaymentInfoByIuv(String iuv) {
        return Mono.fromFuture(paymentInfoEntityDynamoTable.getItem(r -> r.key(k -> k.partitionValue(iuv))))
                .map(entityToDtoPaymentInfoMapper::entityToDto);
    }

    private Mono<Void> createUpdateItemRequestAndPerformUpdate(PaymentInfoEntity entity) {
        Map<String, AttributeValue> keyAttributes = Map.of(
                PaymentInfoEntity.COL_PK, AttributeValue.builder().s(entity.getIuv()).build()
        );

        Map<String, AttributeValue> flatAttributes = Map.of(
                PaymentInfoEntity.COL_IUN, AttributeValue.builder().s(entity.getIun()).build(),
                PaymentInfoEntity.COL_REC_INDEX, AttributeValue.builder().n(String.valueOf(entity.getRecIndex())).build(),
                PaymentInfoEntity.COL_APPLY_COST, AttributeValue.builder().bool(entity.getApplyCost()).build()
        );

        return this.updateIfMatchOrNotExists(keyAttributes, flatAttributes, null,null)
                .then();
    }
    @Override
    public Mono<Void> deleteItemsByIun(String iun) {
        return getAllByIun(iun)
                .switchIfEmpty(Flux.<PaymentInfoEntity>empty()
                        .doOnSubscribe(s -> log.debug("No items found for IUN: {}", iun)))
                .flatMap(paymentInfoEntity ->
                                deleteItemByIuv(paymentInfoEntity.getIuv()),
                        DELETE_ITEMS_BY_IUN_MAX_CONCURRENCY)
                .then();
    }

    private Flux<PaymentInfoEntity> getAllByIun(String iun) {
        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(iun).build()))
                .build();

        return Flux.from(paymentInfoEntityDynamoTable.index(PaymentInfoEntity.IUN_GSI)
                .query(queryEnhancedRequest)
                .flatMapIterable(Page::items));
    }

    private Mono<Void> deleteItemByIuv(String iuv) {
        return Mono.fromFuture(paymentInfoEntityDynamoTable.deleteItem(Key.builder().partitionValue(iuv).build()))
                .then()
                .doOnError(e -> log.error("Error deleting item with IUV: {}", iuv, e));
    }

    private DynamoDbAsyncTable<PaymentInfoEntity> initializeTable(String tableName,
                                                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        StaticTableSchema<PaymentInfoEntity> schemaTable = StaticTableSchema.builder(PaymentInfoEntity.class)
                .newItemSupplier(PaymentInfoEntity::new)
                .addAttribute(String.class, a -> a.name(PaymentInfoEntity.COL_PK)
                        .getter(PaymentInfoEntity::getIuv)
                        .setter(PaymentInfoEntity::setIuv)
                        .tags(primaryPartitionKey()))
                .addAttribute(Integer.class, a -> a.name(PaymentInfoEntity.COL_REC_INDEX)
                        .getter(PaymentInfoEntity::getRecIndex)
                        .setter(PaymentInfoEntity::setRecIndex))
                .addAttribute(Boolean.class, a -> a.name(PaymentInfoEntity.COL_APPLY_COST)
                        .getter(PaymentInfoEntity::getApplyCost)
                        .setter(PaymentInfoEntity::setApplyCost))
                .addAttribute(String.class, a -> a.name(PaymentInfoEntity.COL_IUN)
                        .getter(PaymentInfoEntity::getIun)
                        .setter(PaymentInfoEntity::setIun)
                        .tags(secondaryPartitionKey(PaymentInfoEntity.IUN_GSI)))
                .build();

        return dynamoDbEnhancedAsyncClient.table(tableName, schemaTable);
    }
}