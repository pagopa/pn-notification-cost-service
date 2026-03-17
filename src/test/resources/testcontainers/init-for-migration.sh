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
    dynamodb create-table --table-name pn-Timelines \
    --attribute-definitions AttributeName=iun,AttributeType=S AttributeName=timelineElementId,AttributeType=S \
    --key-schema AttributeName=iun,KeyType=HASH AttributeName=timelineElementId,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

aws --profile $PROFILE --region $REGION --endpoint-url=$ENDPOINT \
    dynamodb create-table --table-name pn-Notifications \
    --attribute-definitions AttributeName=iun,AttributeType=S \
    --key-schema AttributeName=iun,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-PaymentInfo \
    --attribute-definitions \
        AttributeName=pk,AttributeType=S \
    --key-schema \
        AttributeName=pk,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

echo "### CREATE QUEUES ###"

queues="pn-cost-to-update"

for qn in  $( echo $queues | tr " " "\n" ) ; do

    echo creating queue $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn
done


echo "Tables created. Inserting test cases..."

IUN1="IUN-STANDARD-MIX"
aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Notifications --item "{\"iun\": {\"S\": \"$IUN1\"}, \"paFee\": {\"N\": \"100\"}, \"vat\": {\"N\": \"22\"}, \"senderPaId\": {\"S\": \"PA-001\"}, \"notificationFeePolicy\": {\"S\": \"FLAT_RATE\"}, \"pagoPaIntMode\": {\"S\": \"NONE\"}, \"recipients\": {\"L\": [{\"M\": {\"recipientId\": {\"S\": \"REC-0\"}}}, {\"M\": {\"recipientId\": {\"S\": \"REC-1\"}}}, {\"M\": {\"recipientId\": {\"S\": \"REC-2\"}}}]}}"

aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Timelines --item "{\"iun\": {\"S\": \"$IUN1\"}, \"timelineElementId\": {\"S\": \"SEND_ANALOG_DOMICILE.$IUN1.RECINDEX_0.ATTEMPT_0\"}, \"category\": {\"S\": \"SEND_ANALOG_DOMICILE\"}, \"details\": {\"M\": {\"recIndex\": {\"N\": \"0\"}, \"sentAttemptMade\": {\"N\": \"0\"}, \"analogCost\": {\"N\": \"310\"}, \"productType\": {\"S\": \"AR\"}}}}"

aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Timelines --item "{\"iun\": {\"S\": \"$IUN1\"}, \"timelineElementId\": {\"S\": \"SEND_ANALOG_DOMICILE.$IUN1.RECINDEX_1.ATTEMPT_1\"}, \"category\": {\"S\": \"SEND_ANALOG_DOMICILE\"}, \"details\": {\"M\": {\"recIndex\": {\"N\": \"1\"}, \"sentAttemptMade\": {\"N\": \"1\"}, \"analogCost\": {\"N\": \"550\"}, \"productType\": {\"S\": \"AR\"}}}}"



IUN2="IUN-SIMPLE-LETTER"
aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Notifications --item "{\"iun\": {\"S\": \"$IUN2\"}, \"paFee\": {\"N\": \"100\"}, \"vat\": {\"N\": \"22\"}, \"senderPaId\": {\"S\": \"PA-002\"}, \"notificationFeePolicy\": {\"S\": \"FLAT_RATE\"}, \"pagoPaIntMode\": {\"S\": \"NONE\"}, \"recipients\": {\"L\": [{\"M\": {\"recipientId\": {\"S\": \"REC-SINGLE\"}}}]}}"

aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Timelines --item "{\"iun\": {\"S\": \"$IUN2\"}, \"timelineElementId\": {\"S\": \"SEND_SIMPLE.$IUN2.RECINDEX_0\"}, \"category\": {\"S\": \"SEND_SIMPLE_REGISTERED_LETTER\"}, \"details\": {\"M\": {\"recIndex\": {\"N\": \"0\"}, \"analogCost\": {\"N\": \"120\"}, \"productType\": {\"S\": \"896\"}}}}"


IUN3="IUN-REFUSED"
aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Notifications --item "{\"iun\": {\"S\": \"$IUN3\"}, \"notificationStatus\": {\"S\": \"REQUEST_REFUSED\"}, \"paFee\": {\"N\": \"100\"}, \"vat\": {\"N\": \"22\"}, \"senderPaId\": {\"S\": \"PA-003\"}, \"notificationFeePolicy\": {\"S\": \"FLAT_RATE\"}, \"pagoPaIntMode\": {\"S\": \"NONE\"}, \"recipients\": {\"L\": [{\"M\": {\"recipientId\": {\"S\": \"REC-REFUSED\"}}}]}}"

aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Timelines --item "{\"iun\": {\"S\": \"$IUN3\"}, \"timelineElementId\": {\"S\": \"REQUEST_REFUSED.$IUN3.RECINDEX_0\"}, \"category\": {\"S\": \"REQUEST_REFUSED\"}, \"details\": {\"M\": {\"recIndex\": {\"N\": \"0\"}, \"analogCost\": {\"N\": \"120\"}, \"productType\": {\"S\": \"896\"}}}}"

aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Timelines --item "{\"iun\": {\"S\": \"$IUN3\"}, \"timelineElementId\": {\"S\": \"REQUEST_REFUSED.$IUN3.RECINDEX_1\"}, \"category\": {\"S\": \"NOTIFICATION_CANCELLED\"}, \"details\": {\"M\": {\"recIndex\": {\"N\": \"1\"}, \"analogCost\": {\"N\": \"120\"}, \"productType\": {\"S\": \"896\"}}}}"


IUN4="IUN-DOUBLE-ATTEMPT"
aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Notifications --item "{\"iun\": {\"S\": \"$IUN4\"}, \"paFee\": {\"N\": \"100\"}, \"vat\": {\"N\": \"22\"}, \"senderPaId\": {\"S\": \"PA-004\"}, \"notificationFeePolicy\": {\"S\": \"FLAT_RATE\"}, \"pagoPaIntMode\": {\"S\": \"NONE\"}, \"recipients\": {\"L\": [{\"M\": {\"recipientId\": {\"S\": \"REC-FULL\"}}}]}}"

aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Timelines --item "{\"iun\": {\"S\": \"$IUN4\"}, \"timelineElementId\": {\"S\": \"ANALOG.$IUN4.RECINDEX_2.ATTEMPT_0\"}, \"category\": {\"S\": \"SEND_ANALOG_DOMICILE\"}, \"details\": {\"M\": {\"recIndex\": {\"N\": \"2\"}, \"sentAttemptMade\": {\"N\": \"0\"}, \"analogCost\": {\"N\": \"300\"}, \"productType\": {\"S\": \"AR\"}}}}"

aws --endpoint-url=$ENDPOINT dynamodb put-item --table-name pn-Timelines --item "{\"iun\": {\"S\": \"$IUN4\"}, \"timelineElementId\": {\"S\": \"ANALOG.$IUN4.RECINDEX_0.ATTEMPT_1\"}, \"category\": {\"S\": \"SEND_ANALOG_DOMICILE\"}, \"details\": {\"M\": {\"recIndex\": {\"N\": \"0\"}, \"sentAttemptMade\": {\"N\": \"1\"}, \"analogCost\": {\"N\": \"450\"}, \"productType\": {\"S\": \"AR\"}}}}"

echo "Initialization terminated correctly."