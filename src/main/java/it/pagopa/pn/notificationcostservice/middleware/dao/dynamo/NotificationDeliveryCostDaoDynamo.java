package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost.EntityToDtoNotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND;
import static it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity.COL_PA_FEE;
import static it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity.COL_SEND_FEE;
import static it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity.*;

@Component
@Slf4j
public class NotificationDeliveryCostDaoDynamo extends BaseDao implements NotificationDeliveryCostDao {

    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    DynamoDbAsyncTable<NotificationDeliveryCostEntity> notificationDeliveryCostTable;
    EntityToDtoNotificationDeliveryCostMapper entityToDto;

    public NotificationDeliveryCostDaoDynamo(
            DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
            DynamoDbAsyncClient dynamoDbAsyncClient,
            PnNotificationCostServiceConfigs awsConfigs,
            EntityToDtoNotificationDeliveryCostMapper entityToDto
    ) {
        super(dynamoDbAsyncClient, awsConfigs.getNotificationDeliveryCostTable().getTableName());
        this.notificationDeliveryCostTable = dynamoDbEnhancedAsyncClient.table(awsConfigs.getNotificationDeliveryCostTable().getTableName(), TableSchema.fromBean(NotificationDeliveryCostEntity.class));
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.entityToDto = entityToDto;
    }

    @Override
    public Flux<NotificationDeliveryCostEntity> getAllByIun(String iun) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder()
                .partitionValue(iun)
                .build());

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        return Flux.from(notificationDeliveryCostTable.query(queryEnhancedRequest).flatMapIterable(Page::items));
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
        GetItemEnhancedRequest getItemEnhancedRequest = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(iun, recIndex))
                .build();
        return Mono.fromFuture(notificationDeliveryCostTable.getItem(getItemEnhancedRequest))
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

    /**
     * Esegue un aggiornamento condizionale del costo base per l'elemento di notification delivery cost fornito.
     * <p>
     * L'aggiornamento ha successo solo se l'elemento non esiste già, oppure se tutti i campi specificati utilizzati nella condizione corrispondono
     * attualmente ai valori forniti in {@code entity}.
     * Ciò garantisce un comportamento idempotente per invocazioni ripetute con lo stesso payload,
     * prevenendo al contempo che il costo di base venga sovrascritto quando l'elemento memorizzato contiene dati diversi.
     *
     * @param entity l'entità notification delivery cost contenente la chiave, i campi da confrontare,
     * e i valori dei costi di base da aggiornare
     * @return L'entità aggiornata di notification delivery cost se l'aggiornamento ha successo, altrimenti un errore che indica che la condizione non è stata soddisfatta
     */
    public Mono<NotificationDeliveryCostEntity> updateBaseCostIfNotExistsOrMatch(NotificationDeliveryCostEntity entity) {
        Map<String, AttributeValue> keyAttributes = Map.of(
                COL_PK, AttributeValue.builder().s(entity.getIun()).build(),
                COL_SK, AttributeValue.builder().n(Integer.toString(entity.getRecIndex())).build()
        );

        Map<String, AttributeValue> flatAttributes = Map.of(
                COL_VAT, AttributeValue.builder().n(String.valueOf(entity.getVat())).build(),
                COL_NOTIFICATION_FEE_POLICY, AttributeValue.builder().s(entity.getNotificationFeePolicy().name()).build(),
                COL_PAGO_PA_INT_MODE, AttributeValue.builder().s(entity.getPagoPaIntMode().name()).build(),
                COL_SENDER_PA_ID, AttributeValue.builder().s(entity.getSenderPaId()).build(),
                COL_SENDER_TAX_ID, AttributeValue.builder().s(entity.getSenderTaxId()).build(),
                COL_RECIPIENT_INTERNAL_ID, AttributeValue.builder().s(entity.getRecipientInternalId()).build()
        );

        Map<String, AttributeValue> baseCostFields = new LinkedHashMap<>();
        baseCostFields.put(COL_SEND_FEE, AttributeValue.builder().n(String.valueOf(entity.getBaseCost().getSendFee())).build());
        baseCostFields.put(COL_PA_FEE, AttributeValue.builder().n(String.valueOf(entity.getBaseCost().getPaFee())).build());

        Map<String, Map<String, AttributeValue>> mapAttrs = new LinkedHashMap<>();
        mapAttrs.put(COL_BASE_COST, baseCostFields);

        Map<String, AttributeValue> setOnlyFlatAttributes = Map.of(
                COL_LAST_UPDATE, AttributeValue.builder().s(Instant.now().toString()).build()
        );
        return this.updateIfMatchOrNotExists(keyAttributes, flatAttributes, mapAttrs, setOnlyFlatAttributes)
                .map(this::mapFromAttributeValue);
    }

    private NotificationDeliveryCostEntity mapFromAttributeValue(Map<String, AttributeValue> response) {
        NotificationDeliveryCostEntity entity = new NotificationDeliveryCostEntity();
        entity.setIun(response.get(COL_PK).s());
        entity.setRecIndex(Integer.parseInt(response.get(COL_SK).n()));
        entity.setVat(Integer.parseInt(response.get(COL_VAT).n()));
        entity.setNotificationFeePolicy(NotificationFeePolicy.valueOf(response.get(COL_NOTIFICATION_FEE_POLICY).s()));
        entity.setPagoPaIntMode(PagoPaIntMode.valueOf(response.get(COL_PAGO_PA_INT_MODE).s()));
        entity.setSenderPaId(response.get(COL_SENDER_PA_ID).s());
        entity.setSenderTaxId(response.get(COL_SENDER_TAX_ID).s());
        entity.setRecipientInternalId(response.get(COL_RECIPIENT_INTERNAL_ID).s());
        entity.setLastUpdate(Instant.parse(response.get(NotificationDeliveryCostEntity.COL_LAST_UPDATE).s()));

        Map<String, AttributeValue> baseCostMap = response.get(COL_BASE_COST).m();
        BaseCostEntity baseCost = new BaseCostEntity();
        baseCost.setSendFee(Integer.parseInt(baseCostMap.get(COL_SEND_FEE).n()));
        baseCost.setPaFee(Integer.parseInt(baseCostMap.get(COL_PA_FEE).n()));
        entity.setBaseCost(baseCost);

        return entity;
    }

}
