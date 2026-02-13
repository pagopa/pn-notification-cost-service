#!/bin/bash

echo " - Create NotificationDeliveryCost TABLE"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name NotificationDeliveryCost \
    --attribute-definitions \
        AttributeName=iun,AttributeType=S \
        AttributeName=recIndex,AttributeType=N \
    --key-schema \
        AttributeName=iun,KeyType=HASH \
        AttributeName=recIndex,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5
echo "Initialization terminated"





