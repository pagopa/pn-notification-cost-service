#!/bin/bash

echo " - Create NotificationDeliveryCost TABLE"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name NotificationDeliveryCost \
    --attribute-definitions \
        AttributeName=IUN,AttributeType=S \
        AttributeName=RecIndex,AttributeType=S \
    --key-schema \
        AttributeName=IUN,KeyType=HASH \
        AttributeName=RecIndex,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5
echo "Initialization terminated"





