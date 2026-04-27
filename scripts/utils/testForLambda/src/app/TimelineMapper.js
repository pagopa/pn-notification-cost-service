const iun = "TEST-LGVT-RVZT-202604-E-1";
const mapToTimelineElementRequestRefused = (index) => {
    return {
        iun: iun,
        timelineElementId: "REQUEST_REFUSED.IUN_"+iun+"_"+String(index),
        lastUpdate: new Date().toISOString(),
        businessTimestamp: new Date().toISOString(),
        category: "REQUEST_REFUSED",
        details: {
            nextSourceAttemptsMade: Number(0),
            notificationCost: Number(100),
            notificationRequestId: "TU5MVi1MR1ZULVJWWlQtMjAyNjA0LUUtMQ==",
            numberOfRecipients: Number(1),
            paProtocolNumber: "20260427103948",
            refusalReasons: [
                {
                    detail: "Internal Server Error",
                    errorCode: "FILE_NOTFOUND",
                    recIndex: null
                }
            ]
        },
        legalFactId: [],
        notificationSentAt: new Date().toISOString(),
        paId: "5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce",
        statusInfo: {
            actual: "REFUSED",
            statusChanged: true,
            statusChangeTimestamp: new Date().toISOString(),
        },
        timestamp: new Date().toISOString()
    };
};

const mapToTimelineCutElement = (index) => {
    return {
        iun: iun,
        timelineElementId: "SEND_ANALOG_DOMICILE.IUN_"+iun+"_"+String(index),
        lastUpdate: new Date().toISOString(),
        businessTimestamp: new Date().toISOString(),
        category:"SEND_ANALOG_DOMICILE",
        details: {
            analogCost: Number(310),
            productType: "AR",
            sentAttemptMade: Number(0)
        },
        legalFactId: [],
        notificationSentAt: new Date().toISOString(),
        paId: "5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce",
        statusInfo: {
            actual: "DELIVERING",
            statusChanged: true,
            statusChangeTimestamp: new Date().toISOString(),
        },
        timestamp: new Date().toISOString()
    };
};

module.exports = {
    mapToTimelineElementRequestRefused,
    mapToTimelineCutElement
};