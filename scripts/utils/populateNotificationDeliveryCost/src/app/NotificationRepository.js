const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, './config/.env') });

const { DynamoDBDocumentClient, GetCommand, QueryCommand, BatchWriteCommand, ScanCommand, BatchGetCommand } = require("@aws-sdk/lib-dynamodb");
const { AwsAuthClient } = require('./AwsAuthClient');
const { AwsDynamoDBClient } = require('./AwsDynamoDBClient');
const IS_LOCAL = process.env.NODE_ENV === 'local';
const AWS_PROFILE = process.env.AWS_PROFILE;

let ddbDocClient;

async function setupAWS() {
    try {
        const awsAuthClient = new AwsAuthClient();
        const coreTemporaryCredentials = await awsAuthClient.getCredentials(
            AWS_PROFILE,
            IS_LOCAL
        );

        const dynamoDBClient = new AwsDynamoDBClient(
            coreTemporaryCredentials,
            IS_LOCAL
        );

        ddbDocClient = DynamoDBDocumentClient.from(dynamoDBClient.getClient(), { marshallOptions: { removeUndefinedValues: true } });

    } catch (error) {
        console.error(`❌ Errore nella configurazione AWS: ${error.message}`);
        process.exit(1);
    }
}

/**
 * Recupera le notifiche tramite BatchGet e filtra per policy
 * @param {string[]} iuns
 */
const getNotificationsToProcess = async (iuns = []) => {
  try {
    if (!iuns || iuns.length === 0) return [];

    const tableName = "pn-Notifications";
    let allRetrievedItems = [];

    for (let i = 0; i < iuns.length; i += 100) {
      const iunChunk = iuns.slice(i, i + 100);

      const command = new BatchGetCommand({
        RequestItems: {
          [tableName]: {
            Keys: iunChunk.map(iun => ({ iun: iun })),
          }
        }
      });

      const response = await ddbDocClient.send(command);

      if (response.Responses && response.Responses[tableName]) {
        allRetrievedItems.push(...response.Responses[tableName]);
      }

      let unprocessed = response.UnprocessedKeys;
      while (unprocessed && Object.keys(unprocessed).length > 0 && unprocessed[tableName]) {
              const keysToRetry = unprocessed[tableName].Keys;
              const retryCommand = new BatchGetCommand({
                RequestItems: {
                  [tableName]: {
                    Keys: keysToRetry
                  }
                }
              });
        const retryResponse = await ddbDocClient.send(retryCommand);
        if (retryResponse.Responses && retryResponse.Responses[tableName]) {
          allRetrievedItems.push(...retryResponse.Responses[tableName]);
        }
        unprocessed = retryResponse.UnprocessedKeys;
      }
    }

    const filteredItems = allRetrievedItems.filter(item =>
      item.notificationFeePolicy === "DELIVERY_MODE" ||
      item.notificationFeePolicy === "FLAT_RATE"
    );

    console.log(`[OK] BatchGet completato. Trovati ${allRetrievedItems.length} record, di cui ${filteredItems.length} validi per la policy.`);

    return filteredItems;

  } catch (error) {
    console.error("❌ Errore nel BatchGet delle notifiche:", error.message);
    throw error;
  }
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
  const tableName = "NotificationDeliveryCost";

  for (let i = 0; i < items.length; i += 25) {
    const itemChunk = items.slice(i, i + 25);
    const command = new BatchWriteCommand({
      RequestItems: {
        [tableName]: itemChunk.map(item => ({
          PutRequest: { Item: item }
        }))
      }
    });
    await ddbDocClient.send(command);
  }
};

module.exports = {
  setupAWS,
  getNotificationsToProcess,
  getTimelineByIun,
  saveDeliveryCosts
};