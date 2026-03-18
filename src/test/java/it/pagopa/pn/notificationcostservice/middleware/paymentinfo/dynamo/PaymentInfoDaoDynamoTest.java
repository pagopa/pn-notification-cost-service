package it.pagopa.pn.notificationcostservice.middleware.paymentinfo.dynamo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.exception.PnDbConflictException;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.PaymentInfoDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.paymentinfo.DtoToEntityPaymentInfoMapper;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.paymentinfo.EntityToDtoPaymentInfoMapper;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentInfoDaoDynamoTest {
    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Mock
    private PnNotificationCostServiceConfigs configs;

    @Mock
    private PnNotificationCostServiceConfigs.PaymentInfoTable paymentInfoTable;

    @Mock
    private DynamoDbAsyncTable<PaymentInfoEntity> mockTable;

    @Mock
    private DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfoMapper;

    @Mock
    private EntityToDtoPaymentInfoMapper entityToDtoPaymentInfoMapper;

    private PaymentInfoDaoDynamo dao;

    @BeforeEach
    void setup() {
        when(paymentInfoTable.getTableName()).thenReturn("PaymentInfo");
        when(configs.getPaymentInfoTable()).thenReturn(paymentInfoTable);
        when(dynamoDbEnhancedAsyncClient.table(
                any(String.class),
                any(TableSchema.class)
        )).thenReturn(mockTable);

        dao = new PaymentInfoDaoDynamo(
                dynamoDbEnhancedAsyncClient,
                configs,
                dtoToEntityPaymentInfoMapper,
                entityToDtoPaymentInfoMapper
        );
    }

    @Test
    void updateItem_nullList_returnsEmptyList() {
        Mono<List<PaymentInfo>> result = dao.updateItem(null);

        StepVerifier.create(result)
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }

    @Test
    void updateItem_emptyList_returnsEmptyList() {
        Mono<List<PaymentInfo>> result = dao.updateItem(Collections.emptyList());

        StepVerifier.create(result)
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }

    @Test
    void updateItem_singlePayment_success() {
        PaymentInfo paymentInfo = PaymentInfo.builder()
                .iuv("IUV-123")
                .iun("IUN-ABC")
                .recIndex(1)
                .applyCost(true)
                .build();

        PaymentInfoEntity entity = new PaymentInfoEntity();
        entity.setIuv("IUV-123");
        entity.setIun("IUN-ABC");
        entity.setRecIndex(1);
        entity.setApplyCost(true);

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(paymentInfo)).thenReturn(entity);
        when(mockTable.updateItem(any(UpdateItemEnhancedRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(entity));
        when(entityToDtoPaymentInfoMapper.entityToDto(entity)).thenReturn(paymentInfo);

        // WHEN
        Mono<List<PaymentInfo>> result = dao.updateItem(List.of(paymentInfo));

        // THEN
        StepVerifier.create(result)
                .expectNext(List.of(paymentInfo))
                .verifyComplete();

        verify(dtoToEntityPaymentInfoMapper, times(1)).dtoToEntity(paymentInfo);
        verify(mockTable, times(1)).updateItem(any(UpdateItemEnhancedRequest.class));
        verify(entityToDtoPaymentInfoMapper, times(1)).entityToDto(entity);
    }

    @Test
    void updateItem_multiplePayments_success() {

        PaymentInfo payment1 = PaymentInfo.builder().iuv("IUV-1").iun("IUN-1").recIndex(1).applyCost(true).build();
        PaymentInfo payment2 = PaymentInfo.builder().iuv("IUV-2").iun("IUN-2").recIndex(2).applyCost(false).build();

        PaymentInfoEntity entity1 = new PaymentInfoEntity();
        entity1.setIuv("IUV-1");
        entity1.setIun("IUN-1");
        entity1.setRecIndex(1);
        entity1.setApplyCost(true);

        PaymentInfoEntity entity2 = new PaymentInfoEntity();
        entity2.setIuv("IUV-2");
        entity2.setIun("IUN-2");
        entity2.setRecIndex(2);
        entity2.setApplyCost(false);

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(payment1)).thenReturn(entity1);
        when(dtoToEntityPaymentInfoMapper.dtoToEntity(payment2)).thenReturn(entity2);
        when(mockTable.updateItem(any(UpdateItemEnhancedRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(entity1))
                .thenReturn(CompletableFuture.completedFuture(entity2));
        when(entityToDtoPaymentInfoMapper.entityToDto(entity1)).thenReturn(payment1);
        when(entityToDtoPaymentInfoMapper.entityToDto(entity2)).thenReturn(payment2);

        // WHEN
        Mono<List<PaymentInfo>> result = dao.updateItem(List.of(payment1, payment2));

        // THEN
        StepVerifier.create(result)
                .expectNextMatches(list -> list.size() == 2 && list.containsAll(List.of(payment1, payment2)))
                .verifyComplete();

        verify(mockTable, times(2)).updateItem(any(UpdateItemEnhancedRequest.class));
    }

    @Test
    void updateItem_conditionalCheckFailed_throwsPnDbConflictException() {
        PaymentInfo paymentInfo = PaymentInfo.builder()
                .iuv("IUV-123")
                .iun("IUN-ABC")
                .recIndex(1)
                .applyCost(true)
                .build();

        PaymentInfoEntity entity = new PaymentInfoEntity();
        entity.setIuv("IUV-123");

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(paymentInfo)).thenReturn(entity);

        CompletableFuture<PaymentInfoEntity> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(ConditionalCheckFailedException.builder()
                .message("Condition failed")
                .build());

        when(mockTable.updateItem(any(UpdateItemEnhancedRequest.class))).thenReturn(failedFuture);

        // WHEN
        Mono<List<PaymentInfo>> result = dao.updateItem(List.of(paymentInfo));

        // THEN
        StepVerifier.create(result)
                .expectError(PnDbConflictException.class)
                .verify();
    }

    @Test
    void updateItem_genericError_propagatesError() {
        PaymentInfo paymentInfo = PaymentInfo.builder()
                .iuv("IUV-123")
                .iun("IUN-ABC")
                .build();

        PaymentInfoEntity entity = new PaymentInfoEntity();
        entity.setIuv("IUV-123");

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(paymentInfo)).thenReturn(entity);

        CompletableFuture<PaymentInfoEntity> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Generic DynamoDB error"));

        when(mockTable.updateItem(any(UpdateItemEnhancedRequest.class))).thenReturn(failedFuture);

        // WHEN
        Mono<List<PaymentInfo>> result = dao.updateItem(List.of(paymentInfo));

        // THEN
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}
