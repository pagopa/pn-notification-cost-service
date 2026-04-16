package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.paymentinfo.DtoToEntityPaymentInfoMapper;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class PaymentInfoDaoDynamo extends BaseDao implements PaymentInfoDao {

    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    DynamoDbAsyncTable<PaymentInfoEntity> paymentInfoEntityDynamoTable;
    DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfo;

    public PaymentInfoDaoDynamo(
            DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
            DynamoDbAsyncClient dynamoDbAsyncClient,
            PnNotificationCostServiceConfigs awsConfigs,
            DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfo
    ) {
        super(dynamoDbAsyncClient, awsConfigs.getPaymentInfoTable().getTableName());
        this.paymentInfoEntityDynamoTable = dynamoDbEnhancedAsyncClient.table(awsConfigs.getPaymentInfoTable().getTableName(), TableSchema.fromBean(PaymentInfoEntity.class));
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.dtoToEntityPaymentInfo = dtoToEntityPaymentInfo;
    }

    /**
     * Il metodo si occupa di:
     * - effettuare l’update dei dati di pagamenti correlati agli IUV sulla tabella 'pn-PaymentInfo'
     *
     * @param payments lista di pagamenti
     * @return void
     */
    public Mono<Void> updateItemIfNotExistsOrMatch(List<PaymentInfo> payments) {
        if (payments == null || payments.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(payments)
                .map(dtoToEntityPaymentInfo::dtoToEntity)
                .flatMap(this::createUpdateItemRequestAndPerformUpdate)
                .then();
    }

    private Mono<Void> createUpdateItemRequestAndPerformUpdate(PaymentInfoEntity entity) {
        Map<String, AttributeValue> keyAttributes = Map.of(
                PaymentInfoEntity.COL_PK, AttributeValue.builder().s(entity.getIuv()).build()
        );

        Map<String, AttributeValue> flatAttributes = Map.of(
                PaymentInfoEntity.COL_IUN, AttributeValue.builder().s(entity.getIun()).build(),
                PaymentInfoEntity.COL_REC_INDEX, AttributeValue.builder().n(String.valueOf(entity.getRecIndex())).build(),
                PaymentInfoEntity.COL_APPLY_COST, AttributeValue.builder().bool(entity.isApplyCost()).build()
        );

        return this.updateIfMatchOrNotExists(keyAttributes, flatAttributes, null)
                .then();
    }
}