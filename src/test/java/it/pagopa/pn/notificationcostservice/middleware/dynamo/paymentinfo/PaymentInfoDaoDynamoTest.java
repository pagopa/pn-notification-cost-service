package it.pagopa.pn.notificationcostservice.middleware.dynamo.paymentinfo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.PaymentInfoDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.paymentinfo.DtoToEntityPaymentInfoMapper;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
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
    private DynamoDbAsyncIndex<PaymentInfoEntity> mockIndex;

    @Mock
    private DtoToEntityPaymentInfoMapper dtoToEntityPaymentInfoMapper;

    private PaymentInfoDaoDynamo dao;

    @BeforeEach
    void setup() {
        when(paymentInfoTable.getTableName()).thenReturn("PaymentInfo");
        when(configs.getPaymentInfoTable()).thenReturn(paymentInfoTable);
        when(dynamoDbEnhancedAsyncClient.table(
                anyString(),
                ArgumentMatchers.<TableSchema<PaymentInfoEntity>>any()
        )).thenReturn(mockTable);

        dao = new PaymentInfoDaoDynamo(
                dynamoDbEnhancedAsyncClient,
                configs,
                dtoToEntityPaymentInfoMapper
        );
    }

    @Test
    void updateItem_nullList_returnsEmpty() {
        Mono<Void> result = dao.updateItem(null);

        StepVerifier.create(result)
                .verifyComplete();

        verifyNoInteractions(dtoToEntityPaymentInfoMapper, mockTable);
    }

    @Test
    void updateItem_emptyList_returnsEmpty() {
        Mono<Void> result = dao.updateItem(Collections.emptyList());

        StepVerifier.create(result)
                .verifyComplete();

        verifyNoInteractions(dtoToEntityPaymentInfoMapper, mockTable);
    }

    @Test
    void updateItem_singlePayment_success() {
        PaymentInfo paymentInfo = PaymentInfo.builder()
                .iuv("IUV-123")
                .iun("IUN-123")
                .recIndex(0)
                .applyCost(true)
                .build();

        PaymentInfoEntity entity = newPaymentInfoEntity("IUV-123", "IUN-123", 0);

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(paymentInfo)).thenReturn(entity);
        when(mockTable.updateItem(ArgumentMatchers.<UpdateItemEnhancedRequest<PaymentInfoEntity>>any()))
                .thenReturn(CompletableFuture.completedFuture(entity));

        Mono<Void> result = dao.updateItem(List.of(paymentInfo));

        StepVerifier.create(result)
                .verifyComplete();

        verify(dtoToEntityPaymentInfoMapper).dtoToEntity(paymentInfo);
        verify(mockTable).updateItem(ArgumentMatchers.<UpdateItemEnhancedRequest<PaymentInfoEntity>>any());
    }

    @Test
    void updateItem_multiplePayments_success() {
        PaymentInfo p1 = PaymentInfo.builder().iuv("IUV-1").iun("IUN-1").recIndex(0).build();
        PaymentInfo p2 = PaymentInfo.builder().iuv("IUV-2").iun("IUN-2").recIndex(1).build();

        PaymentInfoEntity e1 = newPaymentInfoEntity("IUV-1", "IUN-1", 0);
        PaymentInfoEntity e2 = newPaymentInfoEntity("IUV-2", "IUN-2", 1);

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(p1)).thenReturn(e1);
        when(dtoToEntityPaymentInfoMapper.dtoToEntity(p2)).thenReturn(e2);
        when(mockTable.updateItem(ArgumentMatchers.<UpdateItemEnhancedRequest<PaymentInfoEntity>>any()))
                .thenReturn(CompletableFuture.completedFuture(e1))
                .thenReturn(CompletableFuture.completedFuture(e2));

        Mono<Void> result = dao.updateItem(List.of(p1, p2));

        StepVerifier.create(result)
                .verifyComplete();

        verify(mockTable, times(2))
                .updateItem(ArgumentMatchers.<UpdateItemEnhancedRequest<PaymentInfoEntity>>any());
    }

    @Test
    void deleteItemsByIun_success_deletesAllItemsReturnedByGsiQuery() {
        String iun = "IUN-DELETE";
        PaymentInfoEntity e1 = newPaymentInfoEntity("IUV-1", iun, 0);
        PaymentInfoEntity e2 = newPaymentInfoEntity("IUV-2", iun, 1);

        SdkPublisher<Page<PaymentInfoEntity>> sdkPublisher = subscriber -> Flux.just(
                Page.create(List.of(e1)),
                Page.create(List.of(e2))
        ).subscribe(subscriber);

        PagePublisher<PaymentInfoEntity> pagePublisher = PagePublisher.create(sdkPublisher);

        when(mockTable.index(PaymentInfoEntity.IUN_GSI)).thenReturn(mockIndex);
        when(mockIndex.query(ArgumentMatchers.<QueryEnhancedRequest>any())).thenReturn(pagePublisher);
        when(mockTable.deleteItem(ArgumentMatchers.<Key>any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Mono<Void> result = dao.deleteItemsByIun(iun);

        StepVerifier.create(result)
                .verifyComplete();

        verify(mockTable).index(PaymentInfoEntity.IUN_GSI);
        verify(mockIndex).query(ArgumentMatchers.<QueryEnhancedRequest>any());
        verify(mockTable, times(2)).deleteItem(ArgumentMatchers.<Key>any());
    }

    @Test
    void deleteItemsByIun_whenNoItemsFound_completesWithoutDeletes() {
        String iun = "IUN-EMPTY";

        SdkPublisher<Page<PaymentInfoEntity>> sdkPublisher = subscriber -> Flux
                .<Page<PaymentInfoEntity>>empty()
                .subscribe(subscriber);

        PagePublisher<PaymentInfoEntity> pagePublisher = PagePublisher.create(sdkPublisher);

        when(mockTable.index(PaymentInfoEntity.IUN_GSI)).thenReturn(mockIndex);
        when(mockIndex.query(ArgumentMatchers.<QueryEnhancedRequest>any())).thenReturn(pagePublisher);

        Mono<Void> result = dao.deleteItemsByIun(iun);

        StepVerifier.create(result)
                .verifyComplete();

        verify(mockTable).index(PaymentInfoEntity.IUN_GSI);
        verify(mockIndex).query(ArgumentMatchers.<QueryEnhancedRequest>any());
        verify(mockTable, never()).deleteItem(ArgumentMatchers.<Key>any());
    }

    @Test
    void deleteItemsByIun_whenQueryFails_propagatesError() {
        String iun = "IUN-ERROR";
        RuntimeException expected = new RuntimeException("query failed");

        SdkPublisher<Page<PaymentInfoEntity>> sdkPublisher = subscriber -> Flux
                .<Page<PaymentInfoEntity>>error(expected)
                .subscribe(subscriber);

        PagePublisher<PaymentInfoEntity> pagePublisher = PagePublisher.create(sdkPublisher);

        when(mockTable.index(PaymentInfoEntity.IUN_GSI)).thenReturn(mockIndex);
        when(mockIndex.query(ArgumentMatchers.<QueryEnhancedRequest>any())).thenReturn(pagePublisher);

        StepVerifier.create(dao.deleteItemsByIun(iun))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && "query failed".equals(ex.getMessage()))
                .verify();

        verify(mockTable, never()).deleteItem(ArgumentMatchers.<Key>any());
    }

    @Test
    void deleteItemsByIun_whenDeleteFails_propagatesError() {
        String iun = "IUN-DELETE-ERROR";
        PaymentInfoEntity entity = newPaymentInfoEntity("IUV-ERR", iun, 0);

        SdkPublisher<Page<PaymentInfoEntity>> sdkPublisher = subscriber -> Flux.just(
                Page.create(List.of(entity))
        ).subscribe(subscriber);

        PagePublisher<PaymentInfoEntity> pagePublisher = PagePublisher.create(sdkPublisher);

        when(mockTable.index(PaymentInfoEntity.IUN_GSI)).thenReturn(mockIndex);
        when(mockIndex.query(ArgumentMatchers.<QueryEnhancedRequest>any())).thenReturn(pagePublisher);
        when(mockTable.deleteItem(ArgumentMatchers.<Key>any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("delete failed")));

        StepVerifier.create(dao.deleteItemsByIun(iun))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && "delete failed".equals(ex.getMessage()))
                .verify();

        verify(mockTable).deleteItem(ArgumentMatchers.<Key>any());
    }

    private static PaymentInfoEntity newPaymentInfoEntity(String iuv, String iun, Integer recIndex) {
        return PaymentInfoEntity.builder()
                .iuv(iuv)
                .iun(iun)
                .recIndex(recIndex)
                .applyCost(true)
                .build();
    }
}
