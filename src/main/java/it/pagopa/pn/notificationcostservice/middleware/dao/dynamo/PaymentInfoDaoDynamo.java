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
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.List;

@Component
@Slf4j
public class PaymentInfoDaoDynamo extends BaseDao implements PaymentInfoDao {

    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    DynamoDbAsyncTable<PaymentInfoEntity> paymentInfoEntityDynamoTable;
    DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfo;

    public PaymentInfoDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                             PnNotificationCostServiceConfigs awsConfigs, DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfo) {
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
    @Override
    public Mono<Void> updateItem(List<PaymentInfo> payments) {
        if (payments == null || payments.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(payments)
                .map(dtoToEntityPaymentInfo::dtoToEntity)
                .flatMap(this::updateItem)
                .then();
    }

    private Mono<PaymentInfoEntity> updateItem(PaymentInfoEntity entity) {
        return Mono.fromFuture(paymentInfoEntityDynamoTable.updateItem(createUpdateItemEnhancedRequest(entity)))
                .doOnError(e -> log.error("Error updating item with IUV: {}", entity.getIuv(), e));
    }

    private UpdateItemEnhancedRequest<PaymentInfoEntity> createUpdateItemEnhancedRequest(PaymentInfoEntity entity) {
        return UpdateItemEnhancedRequest.builder(PaymentInfoEntity.class)
                .item(entity)
                .build();
    }
}