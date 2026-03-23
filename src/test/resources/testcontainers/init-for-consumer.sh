#!/bin/bash

ENDPOINT="http://localhost:4566"
REGION="us-east-1"
PROFILE="default"

echo " - Creating Tables..."

aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
    dynamodb create-table --table-name pn-NotificationDeliveryCost \
    --attribute-definitions AttributeName=pk,AttributeType=S AttributeName=sk,AttributeType=N \
    --key-schema AttributeName=pk,KeyType=HASH AttributeName=sk,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
    dynamodb create-table \
    --table-name pn-PaymentInfo \
    --attribute-definitions \
        AttributeName=pk,AttributeType=S \
    --key-schema \
        AttributeName=pk,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

echo "### CREATE QUEUES ###"

queues="pn-notification-cost-to-update pn-notification-cost-outcome"

for qn in $( echo $queues | tr " " "\n" ) ; do
    echo creating queue $qn ...

    aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn
done

echo "Tables created. Inserting test cases..."

echo "### CREATE EVENT BUS - pn-CoreEventBus ###"
event_bus_name="pn-CoreEventBus"
aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
  events create-event-bus --name $event_bus_name

echo "### CREATE EVENT BUS RULE - pn-notification-cost-service ###"
rule_name_notification_cost_service="notification-cost-service"
notification_cost_service_pattern='{"source": ["pn-notification-cost-service"], "detail-type": ["NotificationCostServiceOutcomeEvent"], "detail": {"clientId":["pn-notification-cost-service"]}}'
aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
  events put-rule \
  --name $rule_name_notification_cost_service \
  --event-pattern "$notification_cost_service_pattern" \
  --event-bus-name $event_bus_name

echo "### ENABLE RULE NOTIFICATION COST SERVICE ###"
aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
  events enable-rule \
  --name $rule_name_notification_cost_service \
  --event-bus-name $event_bus_name

echo "### ADD TARGET TO RULE NOTIFICATION COST SERVICE ###"
target_arn="arn:aws:sqs:us-east-1:000000000000:pn-notification-cost-outcome"

aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
  events put-targets \
  --rule $rule_name_notification_cost_service \
  --targets "Id"="1","Arn"="$target_arn","InputPath"="$.detail" \
  --event-bus-name $event_bus_name

echo "Initialization terminated correctly."