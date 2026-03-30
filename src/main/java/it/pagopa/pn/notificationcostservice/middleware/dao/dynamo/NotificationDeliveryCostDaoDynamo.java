package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost.EntityToDtoNotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

import java.util.concurrent.CompletableFuture;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND;

@Component
@Slf4j
public class NotificationDeliveryCostDaoDynamo extends BaseDao implements NotificationDeliveryCostDao {

    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    DynamoDbAsyncTable<NotificationDeliveryCostEntity> notificationDeliveryCostTable;
    EntityToDtoNotificationDeliveryCostMapper entityToDto;

    public NotificationDeliveryCostDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                             PnNotificationCostServiceConfigs awsConfigs, EntityToDtoNotificationDeliveryCostMapper entityToDto) {
        this.notificationDeliveryCostTable = dynamoDbEnhancedAsyncClient.table(awsConfigs.getNotificationDeliveryCostTable().getTableName(), TableSchema.fromBean(NotificationDeliveryCostEntity.class));
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.entityToDto = entityToDto;
    }

    @Override
    public Mono<Page<NotificationDeliveryCostEntity>> getAllByIun(String iun) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder()
                .partitionValue(iun)
                .build());

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        return Flux.from(notificationDeliveryCostTable.query(queryEnhancedRequest).flatMapIterable(Page::items))
                .collectList()
                .map(Page::create);
    }

    /**
     * Il metodo si occupa di:
     * - prendere un determinato item dalla tabella 'pn-NotificationDeliveryCost' in base alla chiave primaria composta da iun
     *
     * @param iun,recIndex identificativi della notifica
     * @return oggetto di notifica con costi
     */
    @Override
    public Mono<NotificationDeliveryCost> getNotificationDeliveryCostItem(String iun, Integer recIndex) {
        return Mono.fromFuture(retrieveItem(iun, recIndex))
                .switchIfEmpty(Mono.error(() -> new PnNotFoundException(
                        "Not Found",
                        "No item found with iun: " + iun + " and recIndex: " + recIndex,
                        ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND)))
                .map(entityToDto::entity2Dto)
                .doOnError(e -> log.error("Error retrieving item with iun: {}", iun, e));
    }
    /**
     * Il metodo si occupa di:
     * - effettuare l’update dei dati di pagamenti correlati alla notifica e del baseCost sulla tabella 'pn-NotificationDeliveryCost'
     *   Aggiornamento dell'entità, aggiornando solo i campi non impostati su null
     *
     * @param entity lista di pagamenti correlati alla notifica
     * @return void
     */
    @Override
    public Mono<NotificationDeliveryCostEntity> updateNotificationDeliveryCostNotNull(NotificationDeliveryCostEntity entity) {
        return Mono.fromFuture(notificationDeliveryCostTable.updateItem(updateItemEnhancedRequest(entity)))
                .doOnError(e -> log.error("Error putting item with iun: {} and recIndex:{}", entity.getIun(), entity.getRecIndex(), e));
    }

    private UpdateItemEnhancedRequest<NotificationDeliveryCostEntity> updateItemEnhancedRequest(NotificationDeliveryCostEntity entity) {
        return UpdateItemEnhancedRequest.builder(NotificationDeliveryCostEntity.class)
                .item(entity)
                .ignoreNulls(true)
                .build();
    }

    private CompletableFuture<NotificationDeliveryCostEntity> retrieveItem(String iun, Integer recIndex) {
        GetItemEnhancedRequest getItemEnhancedRequest = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(iun, recIndex))
                .build();
        return notificationDeliveryCostTable.getItem(getItemEnhancedRequest);
    }

}
