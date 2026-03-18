package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class BaseDao {
    protected Key getKeyBuild(String pk, Integer sk) {
        return Key.builder().partitionValue(pk).sortValue(sk).build();
    }

    protected Expression expressionBuilder(Map<String, AttributeValue> expressionValues, Map<String, String> expressionNames) {
        Expression.Builder expressionBuilder = Expression.builder();
        expressionBuilder.expression("#pk = :pk");
        if (expressionValues != null) {
            expressionBuilder.expressionValues(expressionValues);
        }
        if (expressionNames != null) {
            expressionBuilder.expressionNames(expressionNames);
        }
        return expressionBuilder.build();
    }
}