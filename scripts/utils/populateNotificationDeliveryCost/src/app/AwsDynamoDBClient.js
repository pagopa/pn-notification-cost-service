const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");

class AwsDynamoDBClient {
  constructor(credentials, isLocal) {
    const conf = {
      region: process.env.AWS_REGION || "us-east-1",
      credentials: credentials,
    };

    if (isLocal) {
      conf.endpoint = 'http://localhost:4566';
      conf.sslEnabled = false;
      conf.region = 'us-east-1';
    }

    this._dynamoClient = new DynamoDBClient(conf);
  }

  getClient() {
    return this._dynamoClient;
  }
}

module.exports = { AwsDynamoDBClient };

