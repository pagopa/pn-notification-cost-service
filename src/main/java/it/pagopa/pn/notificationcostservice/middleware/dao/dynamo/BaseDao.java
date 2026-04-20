package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public class BaseDao {
    protected DynamoDbAsyncClient dynamoDbAsyncClient;
    protected String tableName;

    protected BaseDao(DynamoDbAsyncClient dynamoDbAsyncClient, String tableName) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.tableName = tableName;
    }

    protected Key getKeyBuild(String pk, Integer sk) {
        return Key.builder().partitionValue(pk).sortValue(sk).build();
    }

    /**
     * Aggiorna un item DynamoDB solo se non esiste ancora oppure se i valori
     * attuali corrispondono a quelli passati come condizione.
     *
     * @param keyAttributes chiave primaria dell'item
     * @param flatAttributes attributi semplici da aggiornare e verificare
     * @param mapAttributes attributi annidati da aggiornare e verificare
     * @return un {@link Mono} con gli attributi aggiornati restituiti da DynamoDB
     */
    protected Mono<Map<String, AttributeValue>> updateIfMatchOrNotExists(
            Map<String, AttributeValue> keyAttributes,
            Map<String, AttributeValue> flatAttributes,
            Map<String, Map<String, AttributeValue>> mapAttributes) {

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames          = new HashMap<>();
        List<String> setParts                        = new ArrayList<>();
        List<String> conditionParts                  = new ArrayList<>();
        // Handle null maps
        Map<String, AttributeValue> flatAttrs = flatAttributes != null ? flatAttributes : Collections.emptyMap();
        Map<String, Map<String, AttributeValue>> mapAttrs = mapAttributes != null ? mapAttributes : Collections.emptyMap();


        // Flat attributes
        for (Map.Entry<String, AttributeValue> entry : flatAttrs.entrySet()) {
            String attr  = entry.getKey();
            String ph    = ":" + attr;
            String namePh = "#" + attr;

            expressionValues.put(ph, entry.getValue());
            expressionNames.put(namePh, attr);
            setParts.add(namePh + " = " + ph);
            conditionParts.add(namePh + " = " + ph);
        }

        // Map attributes
        for (Map.Entry<String, Map<String, AttributeValue>> mapEntry : mapAttrs.entrySet()) {
            String mapName = mapEntry.getKey();
            expressionNames.put("#" + mapName, mapName);

            AttributeValue mapValue = AttributeValue.builder().m(mapEntry.getValue()).build();
            expressionValues.put(":" + mapName, mapValue);
            setParts.add("#" + mapName + " = :" + mapName);

            for (Map.Entry<String, AttributeValue> fieldEntry : mapEntry.getValue().entrySet()) {
                String fieldName = fieldEntry.getKey();
                String fieldPh   = ":" + mapName + "_" + fieldName;
                String fieldNPh  = "#" + mapName + "_" + fieldName;

                expressionValues.put(fieldPh, fieldEntry.getValue());
                expressionNames.put(fieldNPh, fieldName);
                conditionParts.add("#" + mapName + "." + fieldNPh + " = " + fieldPh);
            }
        }

        // attribute_not_exists su PK + SK
        List<String> notExistsParts = new ArrayList<>();
        for (String keyName : keyAttributes.keySet()) {
            expressionNames.put("#" + keyName, keyName);
            notExistsParts.add("attribute_not_exists(#" + keyName + ")");
        }

        String updateExpression    = "SET " + String.join(", ", setParts);
        String conditionExpression = "(" + String.join(" AND ", notExistsParts) + ")"
                + " OR ("
                + String.join(" AND ", conditionParts)
                + ")";

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(keyAttributes)
                .updateExpression(updateExpression)
                .conditionExpression(conditionExpression)
                .expressionAttributeValues(expressionValues)
                .expressionAttributeNames(expressionNames)
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(request))
                .map(UpdateItemResponse::attributes);
    }
}