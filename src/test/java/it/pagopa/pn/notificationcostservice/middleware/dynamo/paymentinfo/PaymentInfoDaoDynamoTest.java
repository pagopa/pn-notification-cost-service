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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
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

}
