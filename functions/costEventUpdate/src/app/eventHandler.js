const { extractKinesisData } = require("./lib/kinesis.js");
const { mapEvents } = require("./lib/eventMapper.js");
const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");

const sqs = new SQSClient({ region: process.env.REGION });
const QUEUE_URL = process.env.QUEUE_URL;
const CHUNK_SIZE = 10;

exports.handleEvent = async (event) => {
  // 1. Kinesis data extraction
  const cdcEvents = extractKinesisData(event);
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length == 0) {
    console.log("No events to process");
    return {
      batchItemFailures: [],
    };
  }

  const failedSeqNumbers = new Set();
  for (let i = 0; i < cdcEvents.length; i += CHUNK_SIZE) {
    const currentChunk = cdcEvents.slice(i, i + CHUNK_SIZE);

    try {
      // 2. event mapping
      const { processedItems, failedEvents } = await mapEvents(currentChunk);

      // 3. SQS message sending
      // Provo ad inviare gli item mappati con successo a prescindere da eventuali failedEvents restituiti
      if (processedItems.length > 0) {
        let sendError = await sendMessages(processedItems);

        if (sendError.length > 0) {
          console.log(
            "Error in persisting current chunk of cdcEvents: ",
            JSON.stringify(currentChunk)
          );
          sendError.forEach(e => failedSeqNumbers.add(e.kinesisSeqNumber));
        }
      } else {
        console.log(
          "No events to persist in current chunk of cdcEvents: ",
          JSON.stringify(currentChunk)
        );
      }

      // Se il mapping di un evento fallisce, evitiamo di processare i successivi per ridurre i duplicati.
      // Fermando il ciclo ora, deleghiamo ad AWS la risottomissione degli eventi non ancora
      // lavorati, garantendo una gestione pulita del checkpointing su Kinesis.      
      if (failedEvents.length > 0) {
        console.log("Mapping failed for events:", JSON.stringify(failedEvents));
        failedEvents.forEach(e => failedSeqNumbers.add(e.kinesisSeqNumber));
        break; 
      }
    } catch (exc) {
      console.log(
        "Error in persisting current chunk of cdcEvents: ",
        currentChunk,
        exc
      );

      // In caso di errore imprevisto, segno tutti gli eventi rimanenti come falliti e interrompo l'elaborazione del batch
      cdcEvents.slice(i).forEach(e => failedSeqNumbers.add(e.kinesisSeqNumber));
      break;
    }
  }
  if (failedSeqNumbers.size > 0) {
    console.log("process finished with some errors!");
  }
  return {
    batchItemFailures: Array.from(failedSeqNumbers).map(id => ({ itemIdentifier: id }))
  };
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
