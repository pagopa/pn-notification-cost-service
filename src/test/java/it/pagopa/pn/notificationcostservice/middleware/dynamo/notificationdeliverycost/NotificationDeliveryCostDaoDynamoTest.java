package it.pagopa.pn.notificationcostservice.middleware.dynamo.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.exception.PnDbConflictException;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.NotificationDeliveryCostDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost.DtoToEntityNotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost.EntityToDtoNotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationDeliveryCostDaoDynamoTest {

    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Mock
    private PnNotificationCostServiceConfigs configs;

    @Mock
    private PnNotificationCostServiceConfigs.NotificationDeliveryCostTable notificationDeliveryCostDao;

    @Mock
    private DynamoDbAsyncTable<NotificationDeliveryCostEntity> mockTable;

    @Mock
    private EntityToDtoNotificationDeliveryCostMapper entityToDtoMapper;

    private NotificationDeliveryCostDaoDynamo dao;
    @Mock
    private DtoToEntityNotificationDeliveryCostMapper dtoToEntityNotificationDeliveryCostMapper;


    @BeforeEach
    void setup() {
        when(notificationDeliveryCostDao.getTableName()).thenReturn("NotificationDeliveryCost");
        when(configs.getNotificationDeliveryCostTable()).thenReturn(notificationDeliveryCostDao);
        when(dynamoDbEnhancedAsyncClient.table(
                any(String.class),
                any(TableSchema.class)
        )).thenReturn(mockTable);

        dao = new NotificationDeliveryCostDaoDynamo(
                dynamoDbEnhancedAsyncClient,
                configs,
                entityToDtoMapper,
                dtoToEntityNotificationDeliveryCostMapper
        );
    }


    @Test
    void testGetNotificationDeliveryCostItem_Success() {
        // Arrange
        String iun = "test-iun-123";
        Integer recIndex = 0;

        BaseCost baseCost = BaseCost.builder()
                .paFee(2)
                .sendFee(10)
                .build();

        NotificationDeliveryCostEntity entity = newNotificationDeliveryCostEntity(iun, recIndex);
        NotificationDeliveryCost expectedDto = NotificationDeliveryCost.builder()
                .iun(iun)
                .recIndex(recIndex)
                .baseCost(baseCost)
                .vat(0)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .isDeleted(false)
                .firstAnalogCost(FirstAnalogCost.builder()
                        .cost(50)
                        .productType("AR")
                        .build())
                .secondAnalogCost(SecondAnalogCost.builder()
                        .cost(30)
                        .productType("890")
                        .build())
                .simpleRegisteredLetterCost(null)
                .recipientInternalId("recipientInternalId")
                .lastUpdate(entity.getLastUpdate())
                .ttl(10000L)
                .build();

        when(mockTable.getItem(any(GetItemEnhancedRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(entity));
        when(entityToDtoMapper.entity2Dto(entity)).thenReturn(expectedDto);

        // Act
        Mono<NotificationDeliveryCost> result = dao.getNotificationDeliveryCostItem(iun, recIndex);

        // Assert
        StepVerifier.create(result)
                .expectNext(expectedDto)
                .verifyComplete();

        verify(mockTable, times(1)).getItem(any(GetItemEnhancedRequest.class));
        verify(entityToDtoMapper, times(1)).entity2Dto(entity);
    }

    @Test
    void testGetNotificationDeliveryCostItem_NotFound() {
        // Arrange
        String iun = "non-existent-iun";
        Integer recIndex = 0;

        when(mockTable.getItem(any(GetItemEnhancedRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Mono<NotificationDeliveryCost> result = dao.getNotificationDeliveryCostItem(iun, recIndex);

        // Assert
        StepVerifier.create(result)
                .expectError(PnNotFoundException.class)
                .verify();

        verify(mockTable, times(1)).getItem(any(GetItemEnhancedRequest.class));
        verify(entityToDtoMapper, never()).entity2Dto(any());
    }

    @Test
    void testGetNotificationDeliveryCostItem_DynamoDbError() {
        // Arrange
        String iun = "test-iun-123";
        Integer recIndex = 0;

        CompletableFuture<NotificationDeliveryCostEntity> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("DynamoDB error"));

        when(mockTable.getItem(any(GetItemEnhancedRequest.class)))
                .thenReturn(failedFuture);

        // Act
        Mono<NotificationDeliveryCost> result = dao.getNotificationDeliveryCostItem(iun, recIndex);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(mockTable, times(1)).getItem(any(GetItemEnhancedRequest.class));
    }

    private static NotificationDeliveryCostEntity newNotificationDeliveryCostEntity(String iun, Integer recIndex) {
        return NotificationDeliveryCostEntity.builder()
                .iun(iun)
                .recIndex(recIndex)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .baseCost(newBaseCost())
                .vat(0)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .isDeleted(false)
                .lastUpdate(Instant.now())
                .ttl(10000L)
                .firstAnalogCost(FirstAnalogCostEntity.builder()
                        .cost(50)
                        .productType("AR")
                        .build())
                .secondAnalogCost(SecondAnalogCostEntity.builder()
                        .cost(30)
                        .productType("890")
                        .build())
                .simpleRegisteredLetterCost(null)
                .recipientInternalId("recipientInternalId")
                .build();
    }

    @Test
    void putIfAbsent_nullList_returnsEmpty() {
        StepVerifier.create(dao.putIfAbsent(null))
                .verifyComplete();

        verifyNoInteractions(mockTable);
        verifyNoInteractions(dtoToEntityNotificationDeliveryCostMapper);
    }

    @Test
    void putIfAbsent_emptyList_returnsEmpty() {
        StepVerifier.create(dao.putIfAbsent(List.of()))
                .verifyComplete();

        verifyNoInteractions(mockTable);
        verifyNoInteractions(dtoToEntityNotificationDeliveryCostMapper);
    }

    @Test
    void putIfAbsent_success() {
        String iun = "test-iun-123";
        int recIndex = 0;

        NotificationDeliveryCost notification = NotificationDeliveryCostTestBuilder.builder()
                .withIun(iun)
                .withRecIndex(recIndex)
                .build();

        NotificationDeliveryCostEntity entity = newNotificationDeliveryCostEntity(iun, recIndex);

        when(dtoToEntityNotificationDeliveryCostMapper.dto2Entity(notification))
                .thenReturn(entity);
        when(mockTable.putItem(any(PutItemEnhancedRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        Mono<Void> result = dao.putIfAbsent(List.of(notification));

        StepVerifier.create(result)
                .verifyComplete();

        ArgumentCaptor<PutItemEnhancedRequest<NotificationDeliveryCostEntity>> captor =
                ArgumentCaptor.forClass((Class) PutItemEnhancedRequest.class);

        verify(dtoToEntityNotificationDeliveryCostMapper, times(1)).dto2Entity(notification);
        verify(mockTable, times(1)).putItem(captor.capture());

        PutItemEnhancedRequest<NotificationDeliveryCostEntity> request = captor.getValue();
        assertThat(request.item()).isEqualTo(entity);
        assertThat(request.conditionExpression()).isNotNull();
        assertThat(request.conditionExpression().expression()).isEqualTo("attribute_not_exists(sk)");
    }

    @Test
    void putIfAbsent_conditionalCheckFailed_throwsPnDbConflictException() {
        String iun = "test-iun-123";
        int recIndex = 0;

        NotificationDeliveryCost notification = NotificationDeliveryCostTestBuilder.builder()
                .withIun(iun)
                .withRecIndex(recIndex)
                .build();

        NotificationDeliveryCostEntity entity = newNotificationDeliveryCostEntity(iun, recIndex);

        when(dtoToEntityNotificationDeliveryCostMapper.dto2Entity(notification))
                .thenReturn(entity);

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(
                ConditionalCheckFailedException.builder()
                        .message("Condition failed")
                        .build()
        );

        when(mockTable.putItem(any(PutItemEnhancedRequest.class)))
                .thenReturn(failedFuture);

        StepVerifier.create(dao.putIfAbsent(List.of(notification)))
                .expectError(PnDbConflictException.class)
                .verify();

        verify(dtoToEntityNotificationDeliveryCostMapper, times(1)).dto2Entity(notification);
        verify(mockTable, times(1)).putItem(any(PutItemEnhancedRequest.class));
    }

    @Test
    void putIfAbsent_multipleNotifications_success() {
        NotificationDeliveryCost n1 = NotificationDeliveryCostTestBuilder.builder()
                .withIun("iun-1")
                .withRecIndex(0)
                .build();

        NotificationDeliveryCost n2 = NotificationDeliveryCostTestBuilder.builder()
                .withIun("iun-2")
                .withRecIndex(1)
                .build();

        NotificationDeliveryCostEntity e1 = newNotificationDeliveryCostEntity("iun-1", 0);
        NotificationDeliveryCostEntity e2 = newNotificationDeliveryCostEntity("iun-2", 1);

        when(dtoToEntityNotificationDeliveryCostMapper.dto2Entity(n1)).thenReturn(e1);
        when(dtoToEntityNotificationDeliveryCostMapper.dto2Entity(n2)).thenReturn(e2);
        when(mockTable.putItem(any(PutItemEnhancedRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null))
                .thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(dao.putIfAbsent(List.of(n1, n2)))
                .verifyComplete();

        verify(dtoToEntityNotificationDeliveryCostMapper).dto2Entity(n1);
        verify(dtoToEntityNotificationDeliveryCostMapper).dto2Entity(n2);
        verify(mockTable, times(2)).putItem(any(PutItemEnhancedRequest.class));
    }


    private static BaseCostEntity newBaseCost() {
        return BaseCostEntity.builder()
                .paFee(2)
                .sendFee(10)
                .build();
    }
}
