package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.mapper.EntityToDtoNotificationDeliveryCostMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND;

@Component
@Slf4j
public class NotificationDeliveryCostDaoDynamo extends BaseDao implements NotificationDeliveryCostDao {

    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    DynamoDbAsyncClient dynamoDbAsyncClient;
    DynamoDbAsyncTable<NotificationDeliveryCostEntity> notificationDeliveryCostTable;
    private final EntityToDtoNotificationDeliveryCostMapper entityToDto;

    public NotificationDeliveryCostDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                             DynamoDbAsyncClient dynamoDbAsyncClient,
                                             PnNotificationCostServiceConfigs awsConfigs, EntityToDtoNotificationDeliveryCostMapper entityToDto) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.notificationDeliveryCostTable = dynamoDbEnhancedAsyncClient.table(awsConfigs.getNotificationDeliveryCostDao().getTableName(), TableSchema.fromBean(NotificationDeliveryCostEntity.class));
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.entityToDto = entityToDto;
    }

    /**
     * Il metodo si occupa di:
     * - prendere un determinato item dalla tabella in base alla chiave primaria composta da iun e recIndex
     *
     * @param iun,recIndex identificativi della notifica
     * @return oggetto di notifica con costi
     */
    @Override
    public Mono<NotificationDeliveryCostDto> getNotificationDeliveryCostItem(String iun, Integer recIndex) {
        GetItemEnhancedRequest getItemEnhancedRequest = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(iun, recIndex))
                .build();
        return Mono.defer(() -> Mono.fromFuture(notificationDeliveryCostTable.getItem(getItemEnhancedRequest)))
                .doOnNext(entity -> log.info("Retrieved item with iun: {}", entity.getPk()))
                .switchIfEmpty(Mono.error(new PnNotFoundException("Not Found", "No item found with iun: {} and recIndex: {}" + iun + recIndex, ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND)))
                .doOnError(e -> log.error("Error retrieving item with iun: {}", iun))
                .map(entityToDto::entity2Dto);
    }

}
