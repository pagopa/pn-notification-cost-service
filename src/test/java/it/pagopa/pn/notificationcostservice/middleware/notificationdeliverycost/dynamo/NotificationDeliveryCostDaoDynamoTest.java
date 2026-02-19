package it.pagopa.pn.notificationcostservice.middleware.notificationdeliverycost.dynamo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.NotificationDeliveryCostDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.mapper.EntityToDtoNotificationDeliveryCostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationDeliveryCostDaoDynamoTest {

    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Mock
    private PnNotificationCostServiceConfigs configs;

    @Mock
    private PnNotificationCostServiceConfigs.NotificationDeliveryCostDao notificationDeliveryCostDao;

    @Mock
    private DynamoDbAsyncTable<NotificationDeliveryCostEntity> mockTable;

    @Mock
    private EntityToDtoNotificationDeliveryCostMapper entityToDtoMapper;

    private NotificationDeliveryCostDaoDynamo dao;

    @BeforeEach
    void setup() {
        when(notificationDeliveryCostDao.getTableName()).thenReturn("NotificationDeliveryCost");
        when(configs.getNotificationDeliveryCostDao()).thenReturn(notificationDeliveryCostDao);
        when(dynamoDbEnhancedAsyncClient.table(
                any(String.class),
                any(TableSchema.class)
        )).thenReturn(mockTable);

        dao = new NotificationDeliveryCostDaoDynamo(
                dynamoDbEnhancedAsyncClient,
                configs,
                entityToDtoMapper
        );
    }

    @Test
    void testGetNotificationDeliveryCostItem_Success() {
        // Arrange
        String iun = "test-iun-123";
        Integer recIndex = 0;

        BaseCostDto baseCostDto = BaseCostDto.builder()
                .paFee(2)
                .sendFee(10)
                .build();

        NotificationDeliveryCostEntity entity = newNotificationDeliveryCostEntity(iun, recIndex);
        NotificationDeliveryCostDto expectedDto = NotificationDeliveryCostDto.builder()
                .iun(iun)
                .recIndex(recIndex)
                .baseCost(baseCostDto)
                .vat(0)
                .sendFee(10)
                .paFee(2)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .isDeleted(false)
                .firstAnalogCost(FirstAnalogCostDto.builder()
                        .cost(50)
                        .productType("AR")
                        .build())
                .secondAnalogCost(SecondAnalogCostDto.builder()
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
        Mono<NotificationDeliveryCostDto> result = dao.getNotificationDeliveryCostItem(iun, recIndex);

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
        Mono<NotificationDeliveryCostDto> result = dao.getNotificationDeliveryCostItem(iun, recIndex);

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
        Mono<NotificationDeliveryCostDto> result = dao.getNotificationDeliveryCostItem(iun, recIndex);

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
                .sendFee(10)
                .paFee(2)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .isDeleted(false)
                .lastUpdate(Instant.now())
                .ttl(10000L)
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
                .build();
    }

    private static BaseCost newBaseCost() {
        return BaseCost.builder()
                .paFee(2)
                .sendFee(10)
                .build();
    }
}
