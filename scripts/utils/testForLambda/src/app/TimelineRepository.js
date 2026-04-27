const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, './config/.env') });

const { DynamoDBDocumentClient, BatchWriteCommand } = require("@aws-sdk/lib-dynamodb");
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
 * Salva i record mappati nella tabella di destinazione tramite BatchWrite
 */
const saveTimelineElements = async (items) => {
  const tableName = "pn-Timelines";
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
  saveTimelineElements
};