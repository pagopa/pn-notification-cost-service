package it.pagopa.pn.notificationcostservice;


import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
public abstract class MockAWSObjectsTest {

    @MockitoBean
    private MomProducer<NotificationCostInitializationEvent> notificationCostInitializationEventMomProducer;

    @MockitoBean
    private SqsAsyncClient sqsAsyncClient;

    @MockitoBean
    private DynamoDbAsyncClient dynamoDbClient;
}
