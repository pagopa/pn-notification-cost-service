const { expect } = require('chai');
    const { toSafeNumber, extractRecIndex, extractAttempt, mapToDeliveryCost } = require('../app/CostMapper');

describe('CostMapper', () => {
  describe('toSafeNumber', () => {
    it('should return a number when given a valid number string', () => {
      expect(toSafeNumber('123')).to.equal(123);
    });

    it('should return a number when given a valid number', () => {
      expect(toSafeNumber(123)).to.equal(123);
    });

    it('should return null when given a non-numeric string', () => {
      expect(toSafeNumber('abc')).to.be.null;
    });

    it('should return null when given null', () => {
      expect(toSafeNumber(null)).to.be.null;
    });

    it('should return null when given undefined', () => {
      expect(toSafeNumber(undefined)).to.be.null;
    });
  });

  describe('extractRecIndex', () => {
    it('should return the recIndex when the format is correct', () => {
      expect(extractRecIndex('RECINDEX_123')).to.equal(123);
    });

    it('should return null when the format is incorrect', () => {
      expect(extractRecIndex('INVALID_FORMAT')).to.be.null;
    });

    it('should return null when given a non-string value', () => {
      expect(extractRecIndex(123)).to.be.null;
    });
  });

  describe('extractAttempt', () => {
    it('should return the attempt number when the format is correct', () => {
      expect(extractAttempt('ATTEMPT_1')).to.equal(1);
    });

    it('should return null when the format is incorrect', () => {
      expect(extractAttempt('INVALID_FORMAT')).to.be.null;
    });

    it('should return null when given a non-string value', () => {
      expect(extractAttempt(123)).to.be.null;
    });
  });

  describe('mapToDeliveryCost', () => {
    const ttlValue = Math.floor(Date.now() / 1000) + 3600;

    it('should correctly map a notification with analog send events', () => {
      const notif = {
        iun: 'test-iun',
        recipients: [{ recipientId: 'test-recipient' }],
        paFee: 50,
        vat: 22,
        notificationFeePolicy: 'DELIVERY_MODE',
        senderPaId: 'test-pa-id',
        pagoPaIntMode: 1,
        notificationStatus: 'DELIVERED'
      };
      const recipientEvents = [
        { timelineElementId: 'RECINDEX_0#ATTEMPT_0', category: 'SEND_ANALOG_DOMICILE', details: { productType: 'AR', analogCost: 150 } },
        { timelineElementId: 'RECINDEX_0#ATTEMPT_1', category: 'SEND_ANALOG_DOMICILE', details: { productType: '890', analogCost: 200 } }
      ];
      const costItem = mapToDeliveryCost(notif, 0, recipientEvents, ttlValue);

      expect(costItem.iun).to.equal('test-iun');
      expect(costItem.recIndex).to.equal(0);
      expect(costItem.firstAnalogCost).to.deep.equal({ productType: 'AR', cost: 150 });
      expect(costItem.secondAnalogCost).to.deep.equal({ productType: '890', cost: 200 });
      expect(costItem.isDeleted).to.be.false;
    });

    it('should correctly map a notification with simple registered letter', () => {
        const notif = {
            iun: 'test-iun-2',
            recipients: [{ recipientId: 'test-recipient-2' }],
            paFee: 70,
            vat: 22,
            notificationFeePolicy: 'FLAT_RATE',
            senderPaId: 'test-pa-id-2',
            pagoPaIntMode: 0,
            status: 'ACCEPTED'
        };
        const recipientEvents = [
            { timelineElementId: 'RECINDEX_0', category: 'SEND_SIMPLE_REGISTERED_LETTER', details: { productType: 'RIR', analogCost: 180 } }
        ];
        const costItem = mapToDeliveryCost(notif, 0, recipientEvents, ttlValue);

        expect(costItem.iun).to.equal('test-iun-2');
        expect(costItem.simpleRegisteredLetterCost).to.deep.equal({ productType: 'RIR', cost: 180 });
        expect(costItem.isDeleted).to.be.false;
    });

    it('should mark as deleted for REFUSED or CANCELLED status', () => {
        const notif = {
            iun: 'test-iun-3',
            notificationStatus: 'REQUEST_REFUSED'
        };
        const costItem = mapToDeliveryCost(notif, 0, [], ttlValue);
        expect(costItem.isDeleted).to.be.true;
    });
  });
});

