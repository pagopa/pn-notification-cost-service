package it.pagopa.pn.notificationcostservice.middleware.dynamo.notificationdeliverycost;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.NotificationDeliveryCostDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCostEntity;
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
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryCostDaoDynamoTest {

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

    @BeforeEach
    void setup() {
        when(notificationDeliveryCostDao.getTableName()).thenReturn("NotificationDeliveryCost");
        when(configs.getNotificationDeliveryCostTable()).thenReturn(notificationDeliveryCostDao);
        when(dynamoDbEnhancedAsyncClient.table(
                anyString(),
                ArgumentMatchers.<TableSchema<NotificationDeliveryCostEntity>>any()
        )).thenReturn(mockTable);

        dao = new NotificationDeliveryCostDaoDynamo(
                dynamoDbEnhancedAsyncClient,
                configs,
                entityToDtoMapper
        );
    }

    @Test
    void testGetNotificationDeliveryCostItem_Success() {
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
                .lastUpdate(Instant.now())
                .senderPaId("paId")
                .senderTaxId("taxId")
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

    @Test
    void updateNotificationDeliveryCostNotNull_dynamoDbError() {
        NotificationDeliveryCostEntity entity = newNotificationDeliveryCostEntity("test-iun-123", 0);

        CompletableFuture<NotificationDeliveryCostEntity> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("DynamoDB update error"));

        when(mockTable.updateItem(
                ArgumentMatchers.<UpdateItemEnhancedRequest<NotificationDeliveryCostEntity>>any()
        )).thenReturn(failedFuture);

        StepVerifier.create(dao.updateNotificationDeliveryCostNotNull(entity))
                .expectError(RuntimeException.class)
                .verify();

        verify(mockTable, times(1)).updateItem(
                ArgumentMatchers.<UpdateItemEnhancedRequest<NotificationDeliveryCostEntity>>any()
        );
    }

    @Test
    void updateNotificationDeliveryCostNotNull_success() {
        String iun = "test-iun-123";
        int recIndex = 0;

        NotificationDeliveryCostEntity entity = newNotificationDeliveryCostEntity(iun, recIndex);

        when(mockTable.updateItem(
                ArgumentMatchers.<UpdateItemEnhancedRequest<NotificationDeliveryCostEntity>>any()
        )).thenReturn(CompletableFuture.completedFuture(entity));

        StepVerifier.create(dao.updateNotificationDeliveryCostNotNull(entity))
                .expectNext(entity)
                .verifyComplete();

        verify(mockTable, times(1)).updateItem(
                ArgumentMatchers.<UpdateItemEnhancedRequest<NotificationDeliveryCostEntity>>argThat(
                        request -> request != null && entity.equals(request.item())
                )
        );
    }

    @Test
    void updateNotificationDeliveryCostNotNull_multipleNotifications_success() {
        NotificationDeliveryCostEntity e1 = newNotificationDeliveryCostEntity("iun-1", 0);
        NotificationDeliveryCostEntity e2 = newNotificationDeliveryCostEntity("iun-2", 1);

        when(mockTable.updateItem(
                ArgumentMatchers.<UpdateItemEnhancedRequest<NotificationDeliveryCostEntity>>any()
        ))
                .thenReturn(CompletableFuture.completedFuture(e1))
                .thenReturn(CompletableFuture.completedFuture(e2));

        StepVerifier.create(Flux.concat(
                        dao.updateNotificationDeliveryCostNotNull(e1),
                        dao.updateNotificationDeliveryCostNotNull(e2)
                ))
                .expectNext(e1)
                .expectNext(e2)
                .verifyComplete();

        verify(mockTable, times(2)).updateItem(
                ArgumentMatchers.<UpdateItemEnhancedRequest<NotificationDeliveryCostEntity>>any()
        );
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
                .lastUpdate(Instant.now())
                .senderPaId("paId")
                .senderTaxId("taxId")
                .build();
    }

    private static BaseCostEntity newBaseCost() {
        return BaseCostEntity.builder()
                .paFee(2)
                .sendFee(10)
                .build();
    }
}
