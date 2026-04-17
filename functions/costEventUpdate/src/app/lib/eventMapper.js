const { parseKinesisObjToJsonObj } = require("./utils");
const crypto = require("crypto");

const EVENT_TYPE = "COST_UPDATE";

function updateCostPhaseForSendAnalogDomicile(timelineObj) {
  if (
    timelineObj.details?.sentAttemptMade === undefined ||
    timelineObj.details?.sentAttemptMade === null
  ) {
    throw new Error("timelineObject does not have sentAttemptMade");
  }

  return "SEND_ANALOG_DOMICILE_ATTEMPT_" + timelineObj.details.sentAttemptMade;
}

function validateAnalogTimelineObj(category, timelineObj) {
  const missingFields = [
    timelineObj.details?.recIndex === undefined || timelineObj.details?.recIndex === null
      ? "details.recIndex"
      : null,
    timelineObj.details?.analogCost === undefined || timelineObj.details?.analogCost === null
      ? "details.analogCost"
      : null,
    !timelineObj.details?.productType ? "details.productType" : null,
  ].filter(Boolean);

  if (missingFields.length > 0) {
    throw new Error(
      `Missing required fields for ${category}: ${missingFields.join(", ")}`
    );
  }
}


function checkDateParsingOrThrow(dateString) {
  // Accetta da 1 a 9 cifre decimali per i secondi, per coprire sia i formati con millisecondi che quelli con nanosecondi
  const expectedFormat = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,9})?Z$/;

  const invalidMessage = "Invalid date format. Expected YYYY-MM-DDTHH:mm:ssZ or YYYY-MM-DDTHH:mm:ss.sssZ with up to 9 decimal places for seconds";
  if (!expectedFormat.test(dateString)) {
    throw new Error(invalidMessage);
  }

  const timestamp = Date.parse(dateString);
  if (Number.isNaN(timestamp)) {
    throw new Error(invalidMessage);
  }

  return timestamp;
}


exports.mapEvents = async (events) => {
  const featureDateRaw = process.env.FEATURE_DATE;
  const featureDate = checkDateParsingOrThrow(featureDateRaw);
  const processedItems = [];

  for (let index = 0; index < events.length; index++) {
    const filteredEvent = events[index];

    try {
      const notificationSentAt = checkDateParsingOrThrow(filteredEvent.dynamodb.NewImage.notificationSentAt?.S);
      if (notificationSentAt < featureDate) {
        console.log(`Skipping event with iun ${filteredEvent.dynamodb.NewImage.iun.S} due to notificationSentAt ${filteredEvent.dynamodb.NewImage.notificationSentAt.S} being before feature date ${featureDateRaw}`);
        continue;
      }
      const item = mapSingleEvent(filteredEvent);
      processedItems.push(item);
    } catch (error) {
      console.warn(`Error processing event with sequence number ${filteredEvent.kinesisSeqNumber}: ${error.message}`);
      return {
        processedItems,
        failedEvents: events.slice(index)
      };
    }
  }

  return { processedItems, failedEvents: [] };
};

function mapSingleEvent(filteredEvent) {
  const date = new Date();

  const timelineObj = parseKinesisObjToJsonObj(
    filteredEvent.dynamodb.NewImage
  );

  const resultElementBody = {
    iun: timelineObj.iun,
    eventType: EVENT_TYPE
  };

  let messageAttributes = {
    publisher: {
      DataType: "String",
      StringValue: "notificationCostService",
    },
    iun: {
      DataType: "String",
      StringValue: resultElementBody.iun,
    },
    eventId: {
      DataType: "String",
      StringValue: crypto.randomUUID(),
    },
    createdAt: {
      DataType: "String",
      StringValue: date.toISOString(),
    },
    eventType: {
      DataType: "String",
      StringValue: EVENT_TYPE,
    },
  };

  const category = timelineObj.category;

  switch (category) {
    case "SEND_ANALOG_DOMICILE":
      validateAnalogTimelineObj(category, timelineObj);
      resultElementBody.recIndex = timelineObj.details.recIndex;
      resultElementBody.cost = timelineObj.details.analogCost;
      resultElementBody.productType = timelineObj.details.productType;
      resultElementBody.costUpdatePhase = updateCostPhaseForSendAnalogDomicile(timelineObj);
      break;

    case "SEND_SIMPLE_REGISTERED_LETTER":
      validateAnalogTimelineObj(category, timelineObj);
      resultElementBody.recIndex = timelineObj.details.recIndex;
      resultElementBody.cost = timelineObj.details.analogCost;
      resultElementBody.productType = timelineObj.details.productType;
      resultElementBody.costUpdatePhase = "SEND_SIMPLE_REGISTERED_LETTER";
      break;

    case "NOTIFICATION_CANCELLED":
      resultElementBody.isCancelled = true;
      resultElementBody.costUpdatePhase = "NOTIFICATION_CANCELLED";
      break;

    case "REQUEST_REFUSED":
      resultElementBody.isRefused = true;
      resultElementBody.costUpdatePhase = "REQUEST_REFUSED";
      break;

    default:
      throw new Error(`Category is not in supported types: ${category}`);
  }

  return buildResultElement(filteredEvent, resultElementBody, messageAttributes);
};

function buildResultElement(filteredEvent, resultElementBody, messageAttributes) {
    let resultElement = {
      Id: filteredEvent.kinesisSeqNumber,
      MessageBody: JSON.stringify(resultElementBody),
      MessageAttributes: messageAttributes,
    };

    return resultElement;
}
