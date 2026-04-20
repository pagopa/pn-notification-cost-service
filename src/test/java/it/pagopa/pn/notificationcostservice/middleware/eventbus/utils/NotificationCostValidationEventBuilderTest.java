package it.pagopa.pn.notificationcostservice.middleware.eventbus.utils;

import it.pagopa.pn.api.dto.events.notificationcost.utils.ValidationStatus;
import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationCostValidationEventBuilderTest {

    @Test
    void buildOkValidationEvent_shouldCreateExpectedEvent() {
        String iun = "TEST-IUN-12345";

        PnNotificationCostValidationEvent event =
                NotificationCostValidationEventBuilder.buildOkValidationEvent(iun);

        assertNotNull(event);
        assertNotNull(event.getDetail());

        assertNotNull(event.getDetail().getPnNotificationCostValidationPayload());
        assertEquals(
                iun,
                event.getDetail().getPnNotificationCostValidationPayload().getIun()
        );
        assertEquals(
                ValidationStatus.OK,
                event.getDetail().getPnNotificationCostValidationPayload().getStatus()
        );
    }

    @Test
    void buildOkValidationEvent_shouldAllowNullIun_withCurrentImplementation() {
        PnNotificationCostValidationEvent event =
                NotificationCostValidationEventBuilder.buildOkValidationEvent(null);

        assertNotNull(event);
        assertNotNull(event.getDetail());

        assertNotNull(event.getDetail().getPnNotificationCostValidationPayload());
        assertNull(event.getDetail().getPnNotificationCostValidationPayload().getIun());
        assertEquals(
                ValidationStatus.OK,
                event.getDetail().getPnNotificationCostValidationPayload().getStatus()
        );
    }
}

