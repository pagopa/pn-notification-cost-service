package it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo;

import software.amazon.awssdk.enhanced.dynamodb.Key;

public class BaseDao {
    protected Key getKeyBuild(String pk, int sk) {
        return Key.builder().partitionValue(pk).sortValue(sk).build();
    }
}