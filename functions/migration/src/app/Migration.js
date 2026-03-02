const {
  getNotificationsToProcess,
  getTimelineByIun,
  saveDeliveryCosts
} = require("./NotificationRepository");
const { extractRecIndex, mapToDeliveryCost } = require("./CostMapper");

exports.runMigration = async function () {
  try {
    console.log("Starting migration process...");
    const notifications = await getNotificationsToProcess();
    console.log(`Found ${notifications.length} notifications to process.`);

    if (notifications.length === 0) {
      console.log("No notifications to process. Exiting.");
      return { "message": "No notifications to process" };
    }

    const ttlValue = Math.floor(Date.now() / 1000) + (365 * 24 * 60 * 60);

    for (const notif of notifications) {
      const timelineItems = await getTimelineByIun(notif.iun);

      const uniqueRecIndices = [...new Set(
        timelineItems.map(item => extractRecIndex(item.timelineElementId)).filter(idx => idx !== null)
      )];
      const indicesToProcess = uniqueRecIndices.length > 0 ? uniqueRecIndices : [0];

      const itemsToWrite = indicesToProcess.map(idx => {
        const recipientEvents = timelineItems.filter(item => extractRecIndex(item.timelineElementId) === idx);
        return mapToDeliveryCost(notif, idx, recipientEvents, ttlValue);
      });


      await saveDeliveryCosts(itemsToWrite);
    }

    console.log("Migrazione terminata con successo.");
  } catch (error) {
    console.error("Errore critico durante la migrazione:", error);
  }
};