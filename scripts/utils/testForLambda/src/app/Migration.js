const path = require('path');
const {
    setupAWS,
    saveTimelineElements
} = require("./TimelineRepository");
const {mapToTimelineElementRequestRefused, mapToTimelineCutElement} = require("./TimelineMapper");
require('dotenv').config({path: path.resolve(__dirname, './config/.env')});
const CUT_ELEMENT = process.env.CUT_ELEMENT;

exports.runMigration = async function (elementToPersist) {
    try {
        const total = Number(elementToPersist);
        if (!Number.isFinite(total) || total <= 0) return;

        await setupAWS();
        console.log("Starting migration process...");
        const items = [];

        let cutElement = Number(CUT_ELEMENT);
        if (Number(CUT_ELEMENT) === 0 || Number(CUT_ELEMENT) < 0) {
            console.log("Valore non valido per CUT_ELEMENT");
            cutElement = 1;
            console.log("Procedo a settare il valore a 1");
        }

        const specialCount = Math.round(total * (cutElement / 10));

        const specialIndexes = new Set();
        while (specialIndexes.size < specialCount) {
            specialIndexes.add(Math.floor(Math.random() * total));
        }

        for (let i = 0; i < total; i++) {
            if (specialIndexes.has(i)) {
                items.push(mapToTimelineCutElement(i));
            } else {
                items.push(mapToTimelineElementRequestRefused(i));
            }
        }
        await saveTimelineElements(items);
        console.log("Migrazione terminata con successo.");
    } catch (error) {
        console.error("Errore critico durante la migrazione:", error);
    }
};