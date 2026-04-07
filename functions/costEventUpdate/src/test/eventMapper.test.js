const { expect } = require("chai");
const fs = require("fs");

const { mapEvents } = require("../app/lib/eventMapper");

const EVENT_TYPE = "COST_UPDATE";
const PASSING_FEATURE_DATE = "2023-08-08T00:00:00Z";
const FAILING_FEATURE_DATE = "2023-08-09T00:00:00Z";

function loadEventFixture() {
  const eventJSON = fs.readFileSync(
    "./src/test/events/eventMapper.send_analog_domicile.json"
  );
  return JSON.parse(eventJSON);
}

function expectCommonMessageAttributes(messageAttributes, iun) {
  expect(messageAttributes).to.have.all.keys(
    "publisher",
    "iun",
    "eventId",
    "createdAt",
    "eventType"
  );
  expect(messageAttributes.publisher.DataType).equal("String");
  expect(messageAttributes.publisher.StringValue).equal("notificationCostService");
  expect(messageAttributes.iun.DataType).equal("String");
  expect(messageAttributes.iun.StringValue).equal(iun);
  expect(messageAttributes.eventId.DataType).equal("String");
  expect(messageAttributes.eventId.StringValue).to.exist;
  expect(messageAttributes.createdAt.DataType).equal("String");
  expect(messageAttributes.createdAt.StringValue).to.exist;
  expect(messageAttributes.eventType.DataType).equal("String");
  expect(messageAttributes.eventType.StringValue).equal(EVENT_TYPE);
}

describe("event mapper tests", function () {
  const iun = "VWKQ-WQNT-VJZG-202308-K-1";

  beforeEach(() => {
    // Set FEATURE_DATE to a value that allows the event to be processed by default
    process.env.FEATURE_DATE = PASSING_FEATURE_DATE;
  });

  afterEach(() => {
    delete process.env.FEATURE_DATE;
  });

  it("test SEND_ANALOG_DOMICILE ATTEMPT_0 mapping", async () => {
    let event = loadEventFixture();

    const events = [event];

    const { processedItems, failedEvents } = await mapEvents(events);

    expect(processedItems).to.have.length(1);

    // Check MessageBody fields
    let body = JSON.parse(processedItems[0].MessageBody);
    expect(body).to.have.all.keys("iun", "eventType", "recIndex", "cost", "productType", "costUpdatePhase");
    expect(body.iun).equal(iun);
    expect(body.eventType).equal(EVENT_TYPE);
    expect(body.recIndex).equal("0");
    expect(body.costUpdatePhase).equal("SEND_ANALOG_DOMICILE_ATTEMPT_0");
    expect(body.cost).equal("926");
    expect(body.productType).equal("AR_REGISTERED_LETTER");

    // Check message attributes
    expect(processedItems[0]).to.have.all.keys('Id', 'MessageBody', 'MessageAttributes');
    expect(processedItems[0].Id).equal("test-seq-1");

    // Check all MessageAttributes
    expectCommonMessageAttributes(processedItems[0].MessageAttributes, iun);
  });

  it("test SEND_ANALOG_DOMICILE ATTEMPT_1 and different recIndex mapping", async () => {
    let event = loadEventFixture();

    // change ATTEMPT to 1
    event.dynamodb.NewImage.details.M.sentAttemptMade.N = "1";

    // change recIndex to 1
    event.dynamodb.NewImage.details.M.recIndex.N = "1";

    const events = [event];

    const { processedItems, failedEvents } = await mapEvents(events);

    expect(processedItems).to.have.length(1);

    let body = JSON.parse(processedItems[0].MessageBody);
    expect(body).to.have.all.keys("iun", "eventType", "recIndex", "cost", "productType", "costUpdatePhase");
    expect(body.iun).equal(iun);
    expect(body.eventType).equal(EVENT_TYPE);
    expect(body.recIndex).equal("1");
    expect(body.costUpdatePhase).equal("SEND_ANALOG_DOMICILE_ATTEMPT_1");
    expect(body.cost).equal("926");
    expect(body.productType).equal("AR_REGISTERED_LETTER");

    // Check message attributes
    expect(processedItems[0]).to.have.all.keys('Id', 'MessageBody', 'MessageAttributes');
    expectCommonMessageAttributes(processedItems[0].MessageAttributes, iun);
  });

  it("test SEND_SIMPLE_REGISTERED_LETTER mapping", async () => {
    let event = loadEventFixture();

    // change category to SEND_SIMPLE_REGISTERED_LETTER
    event.dynamodb.NewImage.category.S = "SEND_SIMPLE_REGISTERED_LETTER";

    // change the timelineElementId
    event.dynamodb.NewImage.timelineElementId.S =
      "SEND_SIMPLE_REGISTERED_LETTER.IUN_" + iun + ".RECINDEX_0";

    const events = [event];

    const { processedItems, failedEvents } = await mapEvents(events);

    expect(processedItems).to.have.length(1);

    let body = JSON.parse(processedItems[0].MessageBody);
    expect(body).to.have.all.keys("iun", "eventType", "recIndex", "cost", "productType", "costUpdatePhase");
    expect(body.iun).equal(iun);
    expect(body.eventType).equal(EVENT_TYPE);
    expect(body.recIndex).equal("0");
    expect(body.costUpdatePhase).equal("SEND_SIMPLE_REGISTERED_LETTER");
    expect(body.cost).equal("926");
    expect(body.productType).equal("AR_REGISTERED_LETTER");

    // Check message attributes
    expect(processedItems[0]).to.have.all.keys('Id', 'MessageBody', 'MessageAttributes');
    expectCommonMessageAttributes(processedItems[0].MessageAttributes, iun);
  });

  it("test NOTIFICATION_CANCELLED mapping", async () => {
    let event = loadEventFixture();

    // change category to NOTIFICATION_CANCELLED
    event.dynamodb.NewImage.category.S = "NOTIFICATION_CANCELLED";
    event.dynamodb.NewImage.timelineElementId.S =
      "NOTIFICATION_CANCELLED.IUN_" + iun + ".RECINDEX_0";

    const events = [event];

    const { processedItems, failedEvents } = await mapEvents(events);

    // NOTIFICATION_CANCELLED is being sent without recIndex being set in the body
    // because eventMapper doesn't set recIndex for this category
    expect(processedItems).to.have.length(1);
    // Check MessageBody fields - NOTE: no recIndex for NOTIFICATION_CANCELLED
    let body = JSON.parse(processedItems[0].MessageBody);
    expect(body).to.have.all.keys("iun", "eventType", "isCancelled", "costUpdatePhase");
    expect(body.iun).equal(iun);
    expect(body.eventType).equal(EVENT_TYPE);
    expect(body.isCancelled).equal(true);
    expect(body.costUpdatePhase).equal("NOTIFICATION_CANCELLED");

    // Check message attributes
    expect(processedItems[0]).to.have.all.keys('Id', 'MessageBody', 'MessageAttributes');
    expectCommonMessageAttributes(processedItems[0].MessageAttributes, iun);
  });

  it("test REQUEST_REFUSED mapping", async () => {
    let event = loadEventFixture();

    // change category to REQUEST_REFUSED
    event.dynamodb.NewImage.category.S = "REQUEST_REFUSED";
    event.dynamodb.NewImage.timelineElementId.S =
      "REQUEST_REFUSED.IUN_" + iun + ".RECINDEX_0";

    const events = [event];

    const { processedItems, failedEvents } = await mapEvents(events);

    // REQUEST_REFUSED is being sent without recIndex being set in the body
    // because eventMapper doesn't set recIndex for this category
    expect(processedItems).to.have.length(1);
    // Check MessageBody fields - NOTE: no recIndex for REQUEST_REFUSED
    let body = JSON.parse(processedItems[0].MessageBody);
    expect(body).to.have.all.keys("iun", "eventType", "isRefused", "costUpdatePhase");
    expect(body.iun).equal(iun);
    expect(body.eventType).equal(EVENT_TYPE);
    expect(body.isRefused).equal(true);
    expect(body.costUpdatePhase).equal("REQUEST_REFUSED");

    // Check message attributes
    expect(processedItems[0]).to.have.all.keys('Id', 'MessageBody', 'MessageAttributes');
    expectCommonMessageAttributes(processedItems[0].MessageAttributes, iun);
  });

  it("test unmapped event", async () => {
    let event = loadEventFixture();

    // specify an unsupported category
    event.dynamodb.NewImage.category.S = "UNSUPPORTED_CATEGORY";

    const events = [event];

    const { processedItems, failedEvents } = await mapEvents(events);
    expect(processedItems).to.have.length(0);
    expect(failedEvents).to.deep.equal([event]);
  });

  it("test missing fields event - missing recIndex", async () => {
    let event = loadEventFixture();

    // remove recIndex
    delete event.dynamodb.NewImage.details.M.recIndex;

    let events = [event];

    const { processedItems, failedEvents } = await mapEvents(events);
    expect(processedItems).to.have.length(0);
    expect(failedEvents).to.deep.equal([event]);
  });

  it("test missing fields event - missing analogCost", async () => {
    let event = loadEventFixture();

    // remove analogCost - firstAnalogCost will still have productType
    delete event.dynamodb.NewImage.details.M.analogCost;

    let events = [event];

    const { processedItems, failedEvents } = await mapEvents(events);
    expect(processedItems).to.have.length(0);
    expect(failedEvents).to.deep.equal([event]);
  });

  it("test partial batch error exposes only the invalid event in failedEvents", async () => {
    let validEvent = loadEventFixture();
    let invalidEvent = loadEventFixture();
    let invalidEvent2 = loadEventFixture();
    invalidEvent.kinesisSeqNumber = "test-seq-invalid";
    delete invalidEvent.dynamodb.NewImage.details.M.analogCost;
    invalidEvent2.kinesisSeqNumber = "test-seq-invalid-2";
    delete invalidEvent2.dynamodb.NewImage.details.M.recIndex;

    const events = [validEvent, invalidEvent, invalidEvent2];

    const {processedItems, failedEvents} = await mapEvents(events);
    expect(processedItems).to.have.length(1);
    expect(processedItems[0].Id).to.equal("test-seq-1");
    expect(failedEvents).to.deep.equal([invalidEvent, invalidEvent2]);
  });

  it("test SEND_ANALOG_DOMICILE missing ATTEMPT", async () => {
    let event = loadEventFixture();

    // remove ATTEMPT_0 from timelineElementId
    delete event.dynamodb.NewImage.details.M.sentAttemptMade;
    const events = [event];
    const {processedItems, failedEvents} = await mapEvents(events);
    expect(processedItems).to.have.length(0);
    expect(failedEvents).to.deep.equal([event]);
  });

  it("test event processed when notificationSentAt equals FEATURE_DATE", async () => {
    if (!process.env.FEATURE_DATE) {
      throw new Error("FEATURE_DATE must be set for eventMapper tests");
    }

    let event = loadEventFixture();
    event.dynamodb.NewImage.notificationSentAt.S = PASSING_FEATURE_DATE;

    const {processedItems, failedEvents} = await mapEvents([event]);

    expect(processedItems).to.have.length(1);
    expect(failedEvents).to.have.length(0);
  });

  it("test event fails when notificationSentAt is malformed", async () => {
    if (!process.env.FEATURE_DATE) {
      throw new Error("FEATURE_DATE must be set for eventMapper tests");
    }

    let event = loadEventFixture();
    event.dynamodb.NewImage.notificationSentAt.S = "invalid-date-format";

    const {processedItems, failedEvents} = await mapEvents([event]);

    expect(processedItems).to.have.length(0);
    expect(failedEvents).to.have.length(1);
  });

  it("test event filtered when notificationSentAt is before FEATURE_DATE", async () => {
    if (!process.env.FEATURE_DATE) {
      throw new Error("FEATURE_DATE must be set for eventMapper tests");
    }

    let event = loadEventFixture();
    event.dynamodb.NewImage.notificationSentAt.S = "2023-08-01T17:23:32.640258864Z";

    const {processedItems, failedEvents} = await mapEvents([event]);

    expect(processedItems).to.have.length(0);
    expect(failedEvents).to.have.length(0);
  });

  it("test invalid FEATURE_DATE format throws error", async () => {
    const previousFeatureDate = process.env.FEATURE_DATE;
    process.env.FEATURE_DATE = "2023-08-08T00:Z"; // Invalid format

    try {
      await mapEvents([loadEventFixture()]);
      expect.fail("Expected mapEvents to throw an invalid FEATURE_DATE error");
    } catch (error) {
      expect(error.message).to.equal(
        "Invalid date format. Expected YYYY-MM-DDTHH:mm:ssZ or YYYY-MM-DDTHH:mm:ss.sssZ with up to 9 decimal places for seconds"
      );
    } finally {
      process.env.FEATURE_DATE = previousFeatureDate;
    }
  });

  it("test missing FEATURE_DATE throws error", async () => {
    delete process.env.FEATURE_DATE;

    try {
      await mapEvents([loadEventFixture()]);
      expect.fail("Expected mapEvents to throw an invalid FEATURE_DATE error");
    } catch (error) {
      expect(error.message).to.equal(
        "Invalid date format. Expected YYYY-MM-DDTHH:mm:ssZ or YYYY-MM-DDTHH:mm:ss.sssZ with up to 9 decimal places for seconds"
      );
    }
  });
});


