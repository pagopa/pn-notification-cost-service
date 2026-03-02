const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, './config/.env') });

const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  ScanCommand,
  QueryCommand,
  BatchWriteCommand
} = require("@aws-sdk/lib-dynamodb");

/**
 * Configurazione e creazione del client
 */
const createDocClient = () => {
  const endpoint = process.env.LOCALSTACK_ENDPOINT;
  const isLocal = process.env.NODE_ENV === 'local' || !!endpoint;

  if (!isLocal && !process.env.AWS_SESSION_TOKEN && process.env.NODE_ENV !== 'dev') {
     throw new Error("❌ CONFIGURATION ERROR: .env non caricato correttamente");
  }

  const config = {
    region: process.env.AWS_REGION || "us-east-1",
    requestHandler: { connectionTimeout: 2000 }
  };

  if (isLocal) {
    config.endpoint = endpoint || "http://localhost:4566";
    config.credentials = { accessKeyId: "test", secretAccessKey: "test" };
    console.log(`[OK] Localstack Mode: ${config.endpoint}`);
  }

  const client = new DynamoDBClient(config);
  return DynamoDBDocumentClient.from(client, { marshallOptions: { removeUndefinedValues: true } });
};

const ddbDocClient = createDocClient();
/**
 * Recupera tutte le notifiche da processare
 */
const getNotificationsToProcess = async () => {
  let items = [];
  let lastEvaluatedKey = undefined;

  do {
    const command = new ScanCommand({
      TableName: "pn-Notifications",
      FilterExpression: "notificationFeePolicy IN (:p1, :p2)",
      ExpressionAttributeValues: { ":p1": "DELIVERY_MODE", ":p2": "FLAT_RATE" },
      ExclusiveStartKey: lastEvaluatedKey,
    });

    const response = await ddbDocClient.send(command);
    if (response.Items) items.push(...response.Items);
    lastEvaluatedKey = response.LastEvaluatedKey;
  } while (lastEvaluatedKey);

  return items;
};

/**
 * Recupera gli eventi della timeline per uno specifico IUN
 */
const getTimelineByIun = async (iun) => {
  const command = new QueryCommand({
    TableName: "pn-Timelines",
    KeyConditionExpression: "iun = :iun",
    ExpressionAttributeValues: { ":iun": iun }
  });

  const response = await ddbDocClient.send(command);
  return response.Items || [];
};

/**
 * Salva i record mappati nella tabella di destinazione tramite BatchWrite
 */
const saveDeliveryCosts = async (items) => {
  if (!items || items.length === 0) return;

  const command = new BatchWriteCommand({
    RequestItems: {
      "NotificationDeliveryCost": items.map(item => ({
        PutRequest: { Item: item }
      }))
    }
  });

  await ddbDocClient.send(command);
};

module.exports = {
  getNotificationsToProcess,
  getTimelineByIun,
  saveDeliveryCosts
};