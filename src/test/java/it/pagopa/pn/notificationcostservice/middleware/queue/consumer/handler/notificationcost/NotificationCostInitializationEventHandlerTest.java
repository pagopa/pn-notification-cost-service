package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost;

import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.eventbus.EventBridgeProducer;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCostInitializationEventHandlerTest {

    private static final String IUN_1 = "TEST-IUN-123";
    private static final String IUN_2 = "TEST-IUN-456";

    @Mock
    private NotificationCostUpdaterService notificationCostUpdaterService;

    @Mock
    private PaymentInfoDao paymentInfoDao;

    @Mock
    private EventBridgeProducer<PnNotificationCostValidationEvent> producer;

    @InjectMocks
    private NotificationCostInitializationEventHandler handler;

    @Test
    void handleNotificationCostInitializationEvent_shouldErrorWhenNotificationCostsAreNull() {
        NotificationCostInitializationEvent.Payload payload = NotificationCostInitializationEvent.Payload.builder()
                .iun(IUN_1)
                .notificationCosts(null)
                .payments(buildPayments())
                .build();

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectError(PnInternalException.class)
                .verify();

        verifyNoInteractions(notificationCostUpdaterService, paymentInfoDao, producer);
    }

    @Test
    void handleNotificationCostInitializationEvent_shouldErrorWhenNotificationCostsAreEmpty() {
        NotificationCostInitializationEvent.Payload payload = NotificationCostInitializationEvent.Payload.builder()
                .iun(IUN_1)
                .notificationCosts(List.of())
                .payments(buildPayments())
                .build();

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectError(PnInternalException.class)
                .verify();

        verifyNoInteractions(notificationCostUpdaterService, paymentInfoDao, producer);
    }

    @Test
    void handleNotificationCostInitializationEvent_shouldPropagateErrorWhenFirstUpdateBaseCostFails() {
        List<NotificationDeliveryCost> notificationCosts = buildMultipleItemNotificationCosts();
        List<PaymentInfo> payments = buildPayments();
        NotificationCostInitializationEvent.Payload payload = buildPayload(notificationCosts, payments);

        RuntimeException expectedException = new RuntimeException("save failed");

        // Simulate error on the first item of notificationCosts and ensure that subsequent operations are not executed
        when(notificationCostUpdaterService.updateBaseCost(notificationCosts.getFirst()))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "save failed".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterService)
                .updateBaseCost(notificationCosts.getFirst());
        verify(notificationCostUpdaterService, never())
                .updateBaseCost(notificationCosts.get(1));
        verify(paymentInfoDao, never()).updateItemIfNotExistsOrMatch(anyList());
        verify(producer, never()).sendEvent(any(PnNotificationCostValidationEvent.class));
    }

    @Test
    void handleNotificationCostInitializationEvent_shouldPropagateErrorWhenSecondUpdateBaseCostFails() {
        List<NotificationDeliveryCost> notificationCosts = buildMultipleItemNotificationCosts();
        List<PaymentInfo> payments = buildPayments();
        NotificationCostInitializationEvent.Payload payload = buildPayload(notificationCosts, payments);

        RuntimeException expectedException = new RuntimeException("save failed");

        when(notificationCostUpdaterService.updateBaseCost(notificationCosts.getFirst()))
                .thenReturn(Mono.empty());
        // Simulate error on the second item of notificationCosts and ensure that subsequent operations are not executed
        when(notificationCostUpdaterService.updateBaseCost(notificationCosts.get(1)))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "save failed".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterService, times(1))
                .updateBaseCost(notificationCosts.getFirst());
        verify(notificationCostUpdaterService, times(1))
                .updateBaseCost(notificationCosts.get(1));
        verify(paymentInfoDao, never()).updateItemIfNotExistsOrMatch(anyList());
        verify(producer, never()).sendEvent(any(PnNotificationCostValidationEvent.class));
    }

    @Test
    void handleNotificationCostInitializationEvent_shouldPropagateErrorWhenUpdatingPaymentsFails() {
        List<NotificationDeliveryCost> notificationCosts = buildSingleItemNotificationCosts();
        List<PaymentInfo> payments = buildPayments();
        NotificationCostInitializationEvent.Payload payload = buildPayload(notificationCosts, payments);

        RuntimeException expectedException = new RuntimeException("update failed");

        when(notificationCostUpdaterService.updateBaseCost(notificationCosts.getFirst()))
                .thenReturn(Mono.empty());
        when(paymentInfoDao.updateItemIfNotExistsOrMatch(payments))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "update failed".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterService)
                .updateBaseCost(notificationCosts.getFirst());
        verify(paymentInfoDao).updateItemIfNotExistsOrMatch(payments);
        verify(producer, never()).sendEvent(any(PnNotificationCostValidationEvent.class));
    }

    @Test
    void handleNotificationCostInitializationEvent_shouldCompleteAndSendOkValidationEvent() {
        List<NotificationDeliveryCost> notificationCosts = buildSingleItemNotificationCosts();
        List<PaymentInfo> payments = buildPayments();
        NotificationCostInitializationEvent.Payload payload = buildPayload(notificationCosts, payments);

        when(notificationCostUpdaterService.updateBaseCost(notificationCosts.getFirst()))
                .thenReturn(Mono.empty());
        when(paymentInfoDao.updateItemIfNotExistsOrMatch(payments))
                .thenReturn(Mono.empty());
        when(producer.sendEvent(any(PnNotificationCostValidationEvent.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .verifyComplete();

        verify(notificationCostUpdaterService).updateBaseCost(notificationCosts.getFirst());
        verify(paymentInfoDao).updateItemIfNotExistsOrMatch(payments);

        ArgumentCaptor<PnNotificationCostValidationEvent> captor =
                ArgumentCaptor.forClass(PnNotificationCostValidationEvent.class);
        verify(producer).sendEvent(captor.capture());

        PnNotificationCostValidationEvent event = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(event);
        org.junit.jupiter.api.Assertions.assertEquals(
                IUN_1,
                event.getDetail().getPnNotificationCostValidationPayload().getIun()
        );
    }


    @Test
    void handleNotificationCostInitializationEvent_shouldPropagateErrorWhenSendingOutcomeEventFails() {
        List<NotificationDeliveryCost> notificationCosts = buildSingleItemNotificationCosts();
        List<PaymentInfo> payments = buildPayments();
        NotificationCostInitializationEvent.Payload payload = buildPayload(notificationCosts, payments);

        RuntimeException expectedException = new RuntimeException("event bridge failed");

        when(notificationCostUpdaterService.updateBaseCost(notificationCosts.getFirst()))
                .thenReturn(Mono.empty());
        when(paymentInfoDao.updateItemIfNotExistsOrMatch(payments)).thenReturn(Mono.empty());
        when(producer.sendEvent(any(PnNotificationCostValidationEvent.class)))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "event bridge failed".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterService)
                .updateBaseCost(notificationCosts.getFirst());
        verify(paymentInfoDao).updateItemIfNotExistsOrMatch(payments);
        verify(producer).sendEvent(any(PnNotificationCostValidationEvent.class));
    }


    private NotificationCostInitializationEvent.Payload buildPayload(
            List<NotificationDeliveryCost> notificationCosts,
            List<PaymentInfo> payments
    ) {
        return NotificationCostInitializationEvent.Payload.builder()
                .iun(IUN_1)
                .notificationCosts(notificationCosts)
                .payments(payments)
                .build();
    }

    private List<NotificationDeliveryCost> buildSingleItemNotificationCosts() {
        return List.of(
                NotificationDeliveryCostTestBuilder.builder()
                        .withIun(IUN_1)
                        .withRecIndex(0)
                        .withIsDeleted(false)
                        .withSenderPaId("TEST-SENDER-PA-ID")
                        .withSenderTaxId("TEST-SENDER-TAX-ID")
                        .withLastUpdate(Instant.now())
                        .build()
        );
    }

    private List<NotificationDeliveryCost> buildMultipleItemNotificationCosts() {
        return List.of(
                NotificationDeliveryCostTestBuilder.builder()
                        .withIun(IUN_1)
                        .withRecIndex(0)
                        .withIsDeleted(false)
                        .withSenderPaId("TEST-SENDER-PA-ID-1")
                        .withSenderTaxId("TEST-SENDER-TAX-ID-1")
                        .withLastUpdate(Instant.now())
                        .build(),
                NotificationDeliveryCostTestBuilder.builder()
                        .withIun(IUN_2)
                        .withRecIndex(1)
                        .withIsDeleted(false)
                        .withSenderPaId("TEST-SENDER-PA-ID-2")
                        .withSenderTaxId("TEST-SENDER-TAX-ID-2")
                        .withLastUpdate(Instant.now())
                        .build()
        );
    }

    private List<PaymentInfo> buildPayments() {
        return List.of(
                PaymentInfo.builder()
                        .iun(IUN_1)
                        .recIndex(0)
                        .iuv("IUV-123456")
                        .applyCost(true)
                        .build()
        );
    }
}
