package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost.DtoToEntityNotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost.EntityToDtoNotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND;

@Component
@Slf4j
public class NotificationDeliveryCostDaoDynamo extends BaseDao implements NotificationDeliveryCostDao {

    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    DynamoDbAsyncTable<NotificationDeliveryCostEntity> notificationDeliveryCostTable;
    EntityToDtoNotificationDeliveryCostMapper entityToDto;
    DtoToEntityNotificationDeliveryCostMapper dtoToEntityNotificationDeliveryCostMapper;

    public NotificationDeliveryCostDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                             PnNotificationCostServiceConfigs awsConfigs, EntityToDtoNotificationDeliveryCostMapper entityToDto,
                                             DtoToEntityNotificationDeliveryCostMapper dtoToEntityNotificationDeliveryCostMapper) {
        this.notificationDeliveryCostTable = dynamoDbEnhancedAsyncClient.table(awsConfigs.getNotificationDeliveryCostTable().getTableName(), TableSchema.fromBean(NotificationDeliveryCostEntity.class));
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.entityToDto = entityToDto;
        this.dtoToEntityNotificationDeliveryCostMapper = dtoToEntityNotificationDeliveryCostMapper;
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
     * - effettuare l’update dei dati di pagamenti correlati alla notifica e del baseCost sulla tabella 'pn-NotificationDeliveryCost'.
     *
     * @param notificationDeliveryCosts lista di pagamenti correlati alla notifica
     * @return void
     */
    @Override
    public Mono<Void> putIfAbsent(List<NotificationDeliveryCost> notificationDeliveryCosts) {
        if (notificationDeliveryCosts == null || notificationDeliveryCosts.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(notificationDeliveryCosts)
                .map(dtoToEntityNotificationDeliveryCostMapper::dto2Entity)
                .flatMap(this::putIfAbsent)
                .then();

    }

    private Mono<Void> putIfAbsent(NotificationDeliveryCostEntity entity) {
        return Mono.fromFuture(notificationDeliveryCostTable.putItem(putItemEnhancedRequest(entity)))
                .doOnError(e -> log.error("Error putting item with iun: {} and recIndex:{}", entity.getIun(), entity.getRecIndex(), e))
                .onErrorMap(ConditionalCheckFailedException.class,
                        t ->
                                new PnIdConflictException(duplicatedErrors(entity.getIun(), entity.getRecIndex(), entity)));
    }

    private Map<String, String> duplicatedErrors(String iunDuplicated, Integer recIndexDuplicated, NotificationDeliveryCostEntity entity) {
        Map<String, String> duplicatedErrors = new HashMap<>();
        if (Objects.nonNull(iunDuplicated) && Objects.nonNull(recIndexDuplicated)) {
            String keyValueError= "pk: "+entity.getIun()+" sk: "+ entity.getRecIndex();
            duplicatedErrors.put("Duplicated notification delivery cost with key", keyValueError);
        }
        return duplicatedErrors;
    }

    private PutItemEnhancedRequest<NotificationDeliveryCostEntity> putItemEnhancedRequest(NotificationDeliveryCostEntity entity) {
        return PutItemEnhancedRequest.builder(NotificationDeliveryCostEntity.class)
                .item(entity)
                .conditionExpression(
                        Expression.builder()
                                .expression("attribute_not_exists(" + NotificationDeliveryCostEntity.COL_PK + ") " + "&& " +
                                        "attribute_not_exists(" + NotificationDeliveryCostEntity.COL_SK + ")")
                                .build()
                )
                .build();
    }

    private CompletableFuture<NotificationDeliveryCostEntity> retrieveItem(String iun, Integer recIndex) {
        GetItemEnhancedRequest getItemEnhancedRequest = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(iun, recIndex))
                .build();
        return notificationDeliveryCostTable.getItem(getItemEnhancedRequest);
    }

}
