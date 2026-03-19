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
    void updateItem_nullList_returnsEmpty() {
        Mono<Void> result = dao.updateItem(null);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void updateItem_emptyList_returnsEmpty() {
        Mono<Void> result = dao.updateItem(Collections.emptyList());

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void updateItem_singlePayment_success() {
        PaymentInfo paymentInfo = PaymentInfo.builder()
                .iuv("IUV-123")
                .build();

        PaymentInfoEntity entity = new PaymentInfoEntity();
        entity.setIuv("IUV-123");

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(paymentInfo)).thenReturn(entity);
        when(mockTable.updateItem(any(UpdateItemEnhancedRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(entity));

        // WHEN
        Mono<Void> result = dao.updateItem(List.of(paymentInfo));

        // THEN
        StepVerifier.create(result)
                .verifyComplete();

        verify(dtoToEntityPaymentInfoMapper).dtoToEntity(paymentInfo);
        verify(mockTable).updateItem(any(UpdateItemEnhancedRequest.class));
    }

    @Test
    void updateItem_multiplePayments_success() {
        PaymentInfo p1 = PaymentInfo.builder().iuv("IUV-1").build();
        PaymentInfo p2 = PaymentInfo.builder().iuv("IUV-2").build();
        PaymentInfoEntity e1 = new PaymentInfoEntity();
        PaymentInfoEntity e2 = new PaymentInfoEntity();

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(p1)).thenReturn(e1);
        when(dtoToEntityPaymentInfoMapper.dtoToEntity(p2)).thenReturn(e2);
        when(mockTable.updateItem(any(UpdateItemEnhancedRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(e1))
                .thenReturn(CompletableFuture.completedFuture(e2));

        // WHEN
        Mono<Void> result = dao.updateItem(List.of(p1, p2));

        // THEN
        StepVerifier.create(result)
                .verifyComplete();

        verify(mockTable, times(2)).updateItem(any(UpdateItemEnhancedRequest.class));
    }

    @Test
    void updateItem_conditionalCheckFailed_throwsPnDbConflictException() {
        PaymentInfo paymentInfo = PaymentInfo.builder().iuv("IUV-123").build();
        PaymentInfoEntity entity = new PaymentInfoEntity();

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(paymentInfo)).thenReturn(entity);

        CompletableFuture<PaymentInfoEntity> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(ConditionalCheckFailedException.builder()
                .message("Condition failed")
                .build());

        when(mockTable.updateItem(any(UpdateItemEnhancedRequest.class))).thenReturn(failedFuture);

        // WHEN
        Mono<Void> result = dao.updateItem(List.of(paymentInfo));

        // THEN
        StepVerifier.create(result)
                .expectError(PnDbConflictException.class)
                .verify();
    }
}
