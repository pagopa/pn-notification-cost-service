#!/bin/bash

ENDPOINT="http://localhost:4566"
REGION="us-east-1"
PROFILE="default"

echo "### 1. CREATING TABLES ###"

aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
    dynamodb create-table --table-name pn-NotificationDeliveryCost \
    --attribute-definitions AttributeName=pk,AttributeType=S AttributeName=sk,AttributeType=N \
    --key-schema AttributeName=pk,KeyType=HASH AttributeName=sk,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
    dynamodb create-table --table-name pn-PaymentInfo \
    --attribute-definitions AttributeName=pk,AttributeType=S \
    --key-schema AttributeName=pk,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=5

echo "### 2. CREATE QUEUES ###"
queues="pn-notification-cost-to-update pn-notification-cost-outcome"

for qn in $queues ; do
    echo "Creating queue: $qn..."
    aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
        sqs create-queue --attributes '{"DelaySeconds":"2"}' --queue-name $qn
done

echo "### 3. EVENT BUS SETUP ###"
event_bus_name="pn-CoreEventBus"
rule_name="notification-cost-service"
aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
  events create-event-bus --name $event_bus_name

notification_cost_service_pattern='{"source": ["pn-notification-cost-service"], "detail-type": ["NotificationCostServiceOutcomeEvent"]}'
aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
  events put-rule --name $rule_name --event-pattern "$notification_cost_service_pattern" --event-bus-name $event_bus_name

aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
events enable-rule --name $rule_name --event-bus-name $event_bus_name
target_arn="arn:aws:sqs:us-east-1:000000000000:pn-notification-cost-outcome"
aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
  events put-targets --rule $rule_name --event-bus-name $event_bus_name \
  --targets "Id"="1","Arn"="$target_arn","InputPath"="$.detail"

echo "Initialization terminated correctly."