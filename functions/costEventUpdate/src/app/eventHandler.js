const { extractKinesisData } = require("./lib/kinesis.js");
const { mapEvents } = require("./lib/eventMapper.js");
const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");

const sqs = new SQSClient({ region: process.env.REGION });
const QUEUE_URL = process.env.QUEUE_URL;

function appendBatchItemFailures(batchItemFailures, itemIdentifiers) {
  const allIdentifiers = [
    ...batchItemFailures.map((item) => item.itemIdentifier),
    ...itemIdentifiers,
  ];

  return [...new Set(allIdentifiers)].map((itemIdentifier) => ({
    itemIdentifier,
  }));
}


exports.handleEvent = async (event) => {
  // 1. Kinesis data extraction
  const cdcEvents = extractKinesisData(event);
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length == 0) {
    console.log("No events to process");
    return {
      batchItemFailures: [],
    };
  } else {
    let batchItemFailures = [];
    while (cdcEvents.length > 0) {
      // we process them in batches of 10
      let currentCdcEvents = cdcEvents.splice(0, 10);

      try {
        // 2. event mapping
        let processedItems = await mapEvents(currentCdcEvents);

        // 3. SQS message sending
        if (processedItems.length > 0) {
          let responseError = await sendMessages(processedItems);

          if (responseError.length > 0) {
            console.log(
              "Error in persisting current cdcEvents: ",
              JSON.stringify(currentCdcEvents)
            );
            batchItemFailures = appendBatchItemFailures(
              batchItemFailures,
              responseError.map((i) => i.kinesisSeqNumber)
            );
          }
        } else {
          console.log(
            "No events to persist in current cdcEvents: ",
            JSON.stringify(currentCdcEvents)
          );
        }
      } catch (exc) {
        const processedItems = Array.isArray(exc.processedItems)
          ? exc.processedItems
          : [];
        const failedEvents = Array.isArray(exc.failedEvents)
          ? exc.failedEvents
          : currentCdcEvents;
        const shouldStopProcessing = exc.shouldStopProcessing === true;

        console.log(
          "Error in persisting current cdcEvents: ",
          currentCdcEvents
        );

        if (processedItems.length > 0) {
          try {
            const responseError = await sendMessages(processedItems);

            batchItemFailures = appendBatchItemFailures(
              batchItemFailures,
              responseError.map((item) => item.kinesisSeqNumber)
            );
          } catch (sendError) {
            console.log(
              "Error while sending already processed items after partial mapping failure: ",
              sendError
            );
            batchItemFailures = appendBatchItemFailures(
              batchItemFailures,
              processedItems.map((item) => item.Id)
            );
          }
        }

        batchItemFailures = appendBatchItemFailures(
          batchItemFailures,
          failedEvents.map((item) => item.kinesisSeqNumber)
        );

        if (shouldStopProcessing) {
          console.log(
            "Stopping batch processing after validation error in current cdcEvents"
          );
          break;
        }
      }
    }
    if (batchItemFailures.length > 0) {
      console.log("process finished with some errors!");
    }
    return {
      batchItemFailures: batchItemFailures,
    };
  }
};

/**
 * send the messages to SQS in batch
 *
 * @param {*} messages array of mapped messages to send
 * @returns
 */
async function sendMessages(messages) {
  let error = [];
  try {
    console.log(
      "Proceeding to send " + messages.length + " messages to " + QUEUE_URL
    );
    const input = {
      Entries: messages,
      QueueUrl: QUEUE_URL,
    };

    console.log("Sending batch message: %j", input);

    const command = new SendMessageBatchCommand(input);
    const response = await sqs.send(command);

    if (response.Failed && response.Failed.length > 0) {
      console.log(
        "error sending some message totalErrors:" + response.Failed.length
      );

      error = error.concat(
        response.Failed.map((i) => {
          return { kinesisSeqNumber: i.Id };
        })
      );
    }
  } catch (exc) {
    console.log("error sending message", exc);
    throw exc;
  }
  return error;
}
