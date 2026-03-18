package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost;

import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.middleware.queue.utils.EventNotificationCostBuilder;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
class NotificationCostInitializationEventTest {
    @Test
    void toBuilderCreatesEqualCopy() {
        List<NotificationDeliveryCost> notificationCosts = List.of(
                NotificationDeliveryCostTestBuilder.builder()
                        .withIun("IUN-COPY")
                        .withRecIndex(2)
                        .build()
        );

        List<PaymentInfo> payments = List.of(
                PaymentInfo.builder()
                        .iun("IUN-COPY")
                        .recIndex(2)
                        .iuv("IUV-2")
                        .applyCost(false)
                        .build()
        );

        NotificationCostInitializationEvent original =
                EventNotificationCostBuilder.buildNotificationCostEvent("IUN-COPY", notificationCosts, payments);

        NotificationCostInitializationEvent copy = original.toBuilder().build();

        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        assertEquals(original.getPayload().getNotificationCosts(), copy.getPayload().getNotificationCosts());
        assertEquals(original.getPayload().getPayments(), copy.getPayload().getPayments());
    }
}
