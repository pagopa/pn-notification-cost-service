package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost;
import it.pagopa.pn.notificationcostservice.middleware.queue.utils.EventNotificationCostBuilder;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.NotificationCostRequest;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentData;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.RecipientCostData;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
class NotificationCostInitializationEventTest {
    @Test
    void toBuilderCreatesEqualCopy() {
        NotificationCostRequest request = NotificationCostRequest.builder()
                .recipients(List.of(RecipientCostData.builder()
                        .recIndex(2)
                        .recipientInternalId("recipient-2")
                        .senderInternalId("sender-2")
                        .payments(List.of(PaymentData.builder().iuv("IUV-2").applyCost(false).build()))
                        .baseCost(120)
                        .sendFee(20)
                        .paFee(100)
                        .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                        .pagoPaIntMode(PagoPaIntMode.ASYNC)
                        .vat(22)
                        .build()))
                .build();
        NotificationCostInitializationEvent original = EventNotificationCostBuilder.buildNotificationCostEvent(request, "IUN-COPY");
        NotificationCostInitializationEvent copy = original.toBuilder().build();
        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        assertEquals(original.getPayload().getRecipients(), copy.getPayload().getRecipients());
    }
}
