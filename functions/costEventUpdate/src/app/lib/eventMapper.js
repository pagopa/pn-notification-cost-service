const { parseKinesisObjToJsonObj } = require("./utils");
const crypto = require("crypto");

const FEATURE_DATE = process.env.FEATURE_DATE;
const EVENT_TYPE = "COST_UPDATE";

function updateCostPhaseForSendAnalogDomicile(timelineObj) {
  // timelineObj.timelineElementId must exist and be a string
  if (
    !timelineObj.timelineElementId ||
    typeof timelineObj.timelineElementId !== "string"
  ) {
    return null;
  }

  if (timelineObj.timelineElementId.indexOf("ATTEMPT_0") >= 0) {
    return "SEND_ANALOG_DOMICILE_ATTEMPT_0";
  } else if (timelineObj.timelineElementId.indexOf("ATTEMPT_1") >= 0) {
    return "SEND_ANALOG_DOMICILE_ATTEMPT_1";
  } else {
    return null;
  }
}

exports.mapEvents = async (events) => {
  const filteredEvents = events.filter((e) => {
    return (
      e.dynamodb.NewImage.notificationSentAt.S > FEATURE_DATE
    );
  });

  let result = [];

  for (const filteredEvent of filteredEvents) {
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
        resultElementBody.recIndex = timelineObj.details?.recIndex ?? undefined;
        resultElementBody.cost = timelineObj.details?.analogCost ?? undefined;
        resultElementBody.productType = timelineObj.details?.productType ?? undefined;
        const costPhase = updateCostPhaseForSendAnalogDomicile(timelineObj);
        if (costPhase) {
          resultElementBody.costUpdatePhase = costPhase;
        }
        break;

      case "SEND_SIMPLE_REGISTERED_LETTER":
        resultElementBody.recIndex = timelineObj.details?.recIndex ?? undefined;
        resultElementBody.cost = timelineObj.details?.analogCost ?? undefined,
        resultElementBody.productType = timelineObj.details?.productType ?? undefined,
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
    }

    let resultElement = {
      Id: filteredEvent.kinesisSeqNumber,
      MessageBody: JSON.stringify(resultElementBody),
      MessageAttributes: messageAttributes,
    };

    if (resultElementBody.costUpdatePhase && ((resultElementBody.isCancelled || resultElementBody.isRefused) || (
      resultElementBody.recIndex &&
      resultElementBody.cost && resultElementBody.productType))) {
      console.log(
        "Mapped message for the queue: %j",
        JSON.stringify(resultElement)
      );

      result.push(resultElement);
    } else {
      console.error(
        "Error in parsing timelineObj (analogCost or recIndex missing or incomplete timelineElementId): ",
        timelineObj
      );
    }
  }
  return result;
};
