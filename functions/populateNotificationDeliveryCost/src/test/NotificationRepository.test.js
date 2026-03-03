const { expect } = require('chai');
const sinon = require('sinon');
const { DynamoDBDocumentClient, ScanCommand, QueryCommand, BatchWriteCommand } = require('@aws-sdk/lib-dynamodb');
const { mockClient } = require('aws-sdk-client-mock');
const NotificationRepository = require('../app/NotificationRepository');
const ddbMock = mockClient(DynamoDBDocumentClient);

describe('NotificationRepository', () => {
  beforeEach(() => {
    ddbMock.reset();
  });

  describe('getNotificationsToProcess', () => {
    it('should retrieve all notifications with pagination', async () => {
      ddbMock.on(ScanCommand)
        .resolvesOnce({ Items: [{ iun: '1' }], LastEvaluatedKey: { iun: '1' } })
        .resolvesOnce({ Items: [{ iun: '2' }] });

      const items = await NotificationRepository.getNotificationsToProcess();
      expect(items).to.have.lengthOf(2);
      expect(items[0].iun).to.equal('1');
      expect(items[1].iun).to.equal('2');
    });
  });

  describe('getTimelineByIun', () => {
    it('should retrieve timeline events for a given IUN', async () => {
      ddbMock.on(QueryCommand).resolves({ Items: [{ iun: 'test-iun', eventId: '1' }] });
      const items = await NotificationRepository.getTimelineByIun('test-iun');
      expect(items).to.have.lengthOf(1);
      expect(items[0].eventId).to.equal('1');
    });
  });

  describe('saveDeliveryCosts', () => {
    it('should not do anything if items array is empty', async () => {
          await NotificationRepository.saveDeliveryCosts([]);
          const batchWriteCalls = ddbMock.commandCalls(BatchWriteCommand);
          expect(batchWriteCalls).to.have.lengthOf(0);
    });

    it('should call BatchWriteCommand with the correct parameters', async () => {
      ddbMock.on(BatchWriteCommand).resolves({});
      const items = [{ iun: 'test-iun', recIndex: 0 }];
      await NotificationRepository.saveDeliveryCosts(items);

      const batchWriteCalls = ddbMock.commandCalls(BatchWriteCommand);
      expect(batchWriteCalls).to.have.lengthOf(1);
      const putRequest = batchWriteCalls[0].args[0].input.RequestItems.NotificationDeliveryCost[0].PutRequest;
      expect(putRequest.Item.iun).to.equal('test-iun');
    });
  });
});

