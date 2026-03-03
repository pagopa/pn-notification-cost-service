package it.pagopa.pn.notificationcostservice.middleware.notificationdeliverycost.dynamo;

import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.BaseDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.concurrent.ExecutionException;

@SpringBootTest
class TestDao extends BaseDao {

    DynamoDbAsyncTable<NotificationDeliveryCostEntity> table;

    public TestDao(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, String table) {
        this.table = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(NotificationDeliveryCostEntity.class));
    }

    public void putItem(NotificationDeliveryCostEntity entity) {
        PutItemEnhancedRequest<NotificationDeliveryCostEntity> req = PutItemEnhancedRequest.builder(NotificationDeliveryCostEntity.class)
                .item(entity)
                .build();
        table.putItem(req);
    }

    public void delete(String pk, Integer sk) throws ExecutionException, InterruptedException {

        DeleteItemEnhancedRequest req = DeleteItemEnhancedRequest.builder()
                .key(getKeyBuild(pk, sk))
                .build();

        table.deleteItem(req).get();
    }
}