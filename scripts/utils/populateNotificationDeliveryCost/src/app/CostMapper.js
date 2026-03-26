const toSafeNumber = (val) => {
  if (val === null || val === undefined) return null;
  const parsed = Number(val);
  return isNaN(parsed) ? null : parsed;
};

const extractRecIndex = (elementId) => {
  if (!elementId || typeof elementId !== 'string') return null;
  const match = elementId.match(/RECINDEX_(\d+)/);
  return match ? Number(match[1]) : null;
};

const extractAttempt = (elementId) => {
  if (!elementId || typeof elementId !== 'string') return null;
  const match = elementId.match(/ATTEMPT_(\d+)/);
  return match ? Number(match[1]) : null;
};

/**
 * Mappa i campi dalla tabella NotificationDeliveryCost
 */
const mapToDeliveryCost = (notif, currentIndex, recipientEvents, isDeletedField, ttlValue) => {
  const recipientId = (notif.recipients && notif.recipients[currentIndex])
    ? notif.recipients[currentIndex].recipientId : null;

  let firstAnalogCost = null;
  let secondAnalogCost = null;
  let simpleRegisteredLetterCost = null;

  const deliveryCost = {
      pk: String(notif.iun),
      sk: Number(currentIndex),
      baseCost: {
        sendFee: 100,
        paFee: notif.paFee
      },
      vat: toSafeNumber(notif.vat),
      ttl: ttlValue,
      notificationFeePolicy: notif.notificationFeePolicy,
      senderPaId: notif.senderPaId,
      senderTaxId: notif.senderTaxId,
      recipientInternalId: recipientId,
      isDeleted: isDeletedField,
      lastUpdate: new Date().toISOString(),
      pagoPaIntMode: notif.pagoPaIntMode
    };

  for (const event of recipientEvents) {
    const attempt = extractAttempt(event.timelineElementId);
    const productType = event.details?.productType || event.productType || null;
    const cost = toSafeNumber(event.details?.analogCost || event.analogCost);

    if (event.category === 'SEND_ANALOG_DOMICILE') {
      if (attempt === 0) {
        deliveryCost.firstAnalogCost = { productType, cost };
      } else if (attempt === 1) {
        deliveryCost.secondAnalogCost = { productType, cost };
      }
    } else if (event.category === 'SEND_SIMPLE_REGISTERED_LETTER') {
      simpleRegisteredLetterCost = { productType, cost };
      deliveryCost.simpleRegisteredLetterCost = { productType, cost };
    }
  }
  return deliveryCost;
};

module.exports = {
  toSafeNumber,
  extractRecIndex,
  extractAttempt,
  mapToDeliveryCost
};