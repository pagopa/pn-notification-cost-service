const { parseKinesisObjToJsonObj } = require("./utils");
const crypto = require("crypto");
const PartialBatchProcessingError = require("./PartialBatchProcessingError");

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


function checkParsingFeatureDateOrThrow(featureDate) {
  const expectedFormat = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/;

  if (!expectedFormat.test(featureDate)) {
    throw new Error(
      "Invalid FEATURE_DATE format. Expected YYYY-MM-DDTHH:mm:ssZ"
    );
  }

  const timestamp = Date.parse(featureDate);

  if (Number.isNaN(timestamp)) {
    throw new Error(
      "Invalid FEATURE_DATE format. Expected YYYY-MM-DDTHH:mm:ssZ"
    );
  }

  return featureDate;
}


exports.mapEvents = async (events) => {
  const featureDate = checkParsingFeatureDateOrThrow(process.env.FEATURE_DATE);
  const result = [];

  for (let index = 0; index < events.length; index++) {
    const filteredEvent = events[index];

    if (filteredEvent.dynamodb.NewImage.notificationSentAt.S <= featureDate) {
      continue;
    }

    const date = new Date();

    try {
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
          const costPhase = updateCostPhaseForSendAnalogDomicile(timelineObj);
          resultElementBody.costUpdatePhase = costPhase;
          createAndPushElement(result, filteredEvent, resultElementBody, messageAttributes);
          break;

        case "SEND_SIMPLE_REGISTERED_LETTER":
          validateAnalogTimelineObj(category, timelineObj);
          resultElementBody.recIndex = timelineObj.details.recIndex;
          resultElementBody.cost = timelineObj.details.analogCost;
          resultElementBody.productType = timelineObj.details.productType;
          resultElementBody.costUpdatePhase = "SEND_SIMPLE_REGISTERED_LETTER";
          createAndPushElement(result, filteredEvent, resultElementBody, messageAttributes);
          break;

        case "NOTIFICATION_CANCELLED":
          resultElementBody.isCancelled = true;
          resultElementBody.costUpdatePhase = "NOTIFICATION_CANCELLED";
          createAndPushElement(result, filteredEvent, resultElementBody, messageAttributes);
          break;

        case "REQUEST_REFUSED":
          resultElementBody.isRefused = true;
          resultElementBody.costUpdatePhase = "REQUEST_REFUSED";
          createAndPushElement(result, filteredEvent, resultElementBody, messageAttributes);
          break;
        default:
          throw new Error(`Missing required field category: ${category}`);
      }
    } catch (error) {
      throw new PartialBatchProcessingError(error.message, {
        processedItems: [...result],
        failedEvents: [filteredEvent],
        cause: error,
      });
    }
  }
  return result;
};

function createAndPushElement(result, filteredEvent, resultElementBody, messageAttributes) {
    let resultElement = {
      Id: filteredEvent.kinesisSeqNumber,
      MessageBody: JSON.stringify(resultElementBody),
      MessageAttributes: messageAttributes,
    };

    console.log("Mapped message for the queue: %j", JSON.stringify(resultElement));
    result.push(resultElement);
}
