package it.pagopa.pn.notificationcostservice.middleware.dynamo.paymentinfo;

import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.PaymentInfoDaoDynamo;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.paymentinfo.DtoToEntityPaymentInfoMapper;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentInfoDaoDynamoTest {
    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Mock
    private DynamoDbAsyncClient dynamoDbAsyncClient;

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
                any(String.class),
                org.mockito.ArgumentMatchers.<TableSchema<PaymentInfoEntity>>any()
        )).thenReturn(mockTable);

        dao = new PaymentInfoDaoDynamo(
                dynamoDbEnhancedAsyncClient,
                dynamoDbAsyncClient,
                configs,
                dtoToEntityPaymentInfoMapper
        );
    }

    @Test
    void updateItemIfNotExistsOrMatch_nullList_returnsEmpty() {
        StepVerifier.create(dao.updateItemIfNotExistsOrMatch(null))
                .verifyComplete();
        verifyNoInteractions(dtoToEntityPaymentInfoMapper, dynamoDbAsyncClient, mockTable);
    }

    @Test
    void updateItemIfNotExistsOrMatch_emptyList_returnsEmpty() {
        StepVerifier.create(dao.updateItemIfNotExistsOrMatch(Collections.emptyList()))
                .verifyComplete();
        verifyNoInteractions(dtoToEntityPaymentInfoMapper, dynamoDbAsyncClient, mockTable);
    }


    @Test
    void updateItemIfNotExistsOrMatch_singlePayment_buildsConditionalUpdateRequest() {
        PaymentInfo payment = PaymentInfo.builder()
                .iuv("IUV-123")
                .iun("IUN-123")
                .recIndex(2)
                .applyCost(true)
                .build();

        PaymentInfoEntity entity = PaymentInfoEntity.builder()
                .iuv("IUV-123")
                .iun("IUN-123")
                .recIndex(2)
                .applyCost(true)
                .build();

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(payment)).thenReturn(entity);

        Map<String, AttributeValue> responseAttributes = Map.of(
                PaymentInfoEntity.COL_PK, AttributeValue.builder().s("IUV-123").build(),
                PaymentInfoEntity.COL_IUN, AttributeValue.builder().s("IUN-123").build(),
                PaymentInfoEntity.COL_REC_INDEX, AttributeValue.builder().n("2").build(),
                PaymentInfoEntity.COL_APPLY_COST, AttributeValue.builder().bool(true).build()
        );

        when(dynamoDbAsyncClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        UpdateItemResponse.builder()
                                .attributes(responseAttributes)
                                .build()
                ));

        StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(payment)))
                .verifyComplete();

        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(dynamoDbAsyncClient).updateItem(captor.capture());

        UpdateItemRequest request = captor.getValue();
        Assertions.assertEquals("PaymentInfo", request.tableName());
        Assertions.assertEquals("IUV-123", request.key().get(PaymentInfoEntity.COL_PK).s());
        Assertions.assertTrue(request.updateExpression().contains("#iun = :iun"));
        Assertions.assertTrue(request.updateExpression().contains("#recIndex = :recIndex"));
        Assertions.assertTrue(request.updateExpression().contains("#applyCost = :applyCost"));
        Assertions.assertTrue(request.conditionExpression().contains("attribute_not_exists(#pk)"));
        Assertions.assertEquals("IUN-123", request.expressionAttributeValues().get(":iun").s());
        Assertions.assertEquals("2", request.expressionAttributeValues().get(":recIndex").n());
        Assertions.assertTrue(request.expressionAttributeValues().get(":applyCost").bool());
    }

    @Test
    void updateItemIfNotExistsOrMatch_propagatesDynamoError() {
        PaymentInfo payment = PaymentInfo.builder()
                .iuv("IUV-ERR")
                .iun("IUN-ERR")
                .recIndex(1)
                .applyCost(false)
                .build();

        PaymentInfoEntity entity = PaymentInfoEntity.builder()
                .iuv("IUV-ERR")
                .iun("IUN-ERR")
                .recIndex(1)
                .applyCost(false)
                .build();

        when(dtoToEntityPaymentInfoMapper.dtoToEntity(payment)).thenReturn(entity);

        CompletableFuture<UpdateItemResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("conditional update failed"));

        when(dynamoDbAsyncClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(failedFuture);

        StepVerifier.create(dao.updateItemIfNotExistsOrMatch(List.of(payment)))
                .expectError(RuntimeException.class)
                .verify();

        verify(dynamoDbAsyncClient).updateItem(any(UpdateItemRequest.class));
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
