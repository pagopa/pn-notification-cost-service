const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

describe("event handler tests", function () {
  it("test Ok", async () => {
    const event = {};

    const mockSQSClient = {
      send: async () => ({}), // Mock per un successo
    };

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {
          async send() {
            // implementation goes here
          }
        },
      },
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{}];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return { processedItems: [{ test: 1 }], failedEvents: [] };
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test errore nella send", async () => {
    const event = {};

    const mockSQSClient = {
      send: async () => ({
        Failed: [
          {
            Id: "message-1",
            Code: "SomeErrorCode",
            Message: "Errore nell'invio del messaggio 1",
          },
        ],
      }),
    };

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {
          async send() {
            // implementation goes here
          }
        },
      },
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{ payload: "1", kinesisSeqNumber: "test" }];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return { processedItems: [{ test: 1 }], failedEvents: [] };
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [{ itemIdentifier: "message-1" }],
    });
  });

  it("test exception nella send", async () => {
    const event = {};

    const mockSQSClient = {
      send: async () => {
        throw new Error("Simulated SQS Error");
      },
    };

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {
          async send() {
            // implementation goes here
          }
        },
      },
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{ payload: "1", kinesisSeqNumber: "test" }];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return { processedItems: [{ test: 1 }], failedEvents: [] };
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [{ itemIdentifier: "test" }],
    });
  });

  it("test no kinesis data", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return { processedItems: [{ test: 1 }], failedEvents: [] };
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test no data to persist", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{ test: 1 }];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return { processedItems: [], failedEvents: [] };
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test exception with processedItems sends partial messages", async () => {
    const event = {};
    let sentCommand;

    const processedItems = [
      {
        Id: "mapped-1",
        MessageBody: "{}",
        MessageAttributes: {},
      },
    ];

    const mockSQSClient = {
      send: async (command) => {
        sentCommand = command;
        return {};
      },
    };

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {
          constructor(input) {
            this.input = input;
          }
        },
      },
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [
            { payload: "1", kinesisSeqNumber: "test-valid" },
            { payload: "2", kinesisSeqNumber: "test-invalid" },
          ];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: async () => {
          return { 
            processedItems, 
            failedEvents: [{ payload: "2", kinesisSeqNumber: "test-invalid" }] };
        },
      },
    });

    const res = await lambda.handleEvent(event);

    expect(sentCommand.input.Entries).to.deep.equal(processedItems);
    expect(res).deep.equals({
      batchItemFailures: [{ itemIdentifier: "test-invalid" }],
    });
  });

  it("test partial validation error stops processing following slices", async () => {
    const event = {};
    let mapEventsCalls = 0;

    const currentEvents = Array.from({ length: 11 }, (_, index) => ({
      kinesisSeqNumber: `seq-${index + 1}`,
    }));

    const processedItems = [
      {
        Id: "seq-1",
        MessageBody: "{}",
        MessageAttributes: {},
      },
    ];

    const mockSQSClient = {
      send: async () => ({}),
    };

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {
          constructor(input) {
            this.input = input;
          }
        },
      },
      "./lib/kinesis.js": {
        extractKinesisData: () => currentEvents,
      },
      "./lib/eventMapper.js": {
        mapEvents: async () => {
          mapEventsCalls += 1;
          return { 
            processedItems, 
            failedEvents: [{ kinesisSeqNumber: "seq-2" }] 
          };
        },
      },
    });

    const res = await lambda.handleEvent(event);

    expect(mapEventsCalls).to.equal(1);
    expect(res).to.deep.equal({
      batchItemFailures: [{ itemIdentifier: "seq-2" }],
    });
  });
});
