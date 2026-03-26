package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost;

import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.eventbus.EventBridgeProducer;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCostInitializationEventHandlerTest {

    private static final String IUN = "TEST-IUN-123";

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
                .iun(IUN)
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
                .iun(IUN)
                .notificationCosts(List.of())
                .payments(buildPayments())
                .build();

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectError(PnInternalException.class)
                .verify();

        verifyNoInteractions(notificationCostUpdaterService, paymentInfoDao, producer);
    }

    @Test
    void handleNotificationCostInitializationEvent_shouldPropagateErrorWhenSavingNotificationCostsFails() {
        List<NotificationDeliveryCost> notificationCosts = buildNotificationCosts();
        List<PaymentInfo> payments = buildPayments();
        NotificationCostInitializationEvent.Payload payload = buildPayload(notificationCosts, payments);

        RuntimeException expectedException = new RuntimeException("save failed");

        PublisherProbe<Void> paymentUpdateProbe = PublisherProbe.empty();
        PublisherProbe<Void> outcomeEventProbe = PublisherProbe.empty();

        when(notificationCostUpdaterService.updateCostByPhase(CostUpdatePhaseInt.VALIDATION, notificationCosts))
                .thenReturn(Mono.error(expectedException));
        when(paymentInfoDao.updateItem(payments)).thenReturn(paymentUpdateProbe.mono());
        when(producer.sendEvent(any(PnNotificationCostValidationEvent.class))).thenReturn(outcomeEventProbe.mono());

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "save failed".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterService)
                .updateCostByPhase(CostUpdatePhaseInt.VALIDATION, notificationCosts);

        paymentUpdateProbe.assertWasNotSubscribed();
        outcomeEventProbe.assertWasNotSubscribed();
    }

    @Test
    void handleNotificationCostInitializationEvent_shouldPropagateErrorWhenUpdatingPaymentsFails() {
        List<NotificationDeliveryCost> notificationCosts = buildNotificationCosts();
        List<PaymentInfo> payments = buildPayments();
        NotificationCostInitializationEvent.Payload payload = buildPayload(notificationCosts, payments);

        RuntimeException expectedException = new RuntimeException("update failed");

        PublisherProbe<Void> outcomeEventProbe = PublisherProbe.empty();

        when(notificationCostUpdaterService.updateCostByPhase(CostUpdatePhaseInt.VALIDATION, notificationCosts))
                .thenReturn(Mono.empty());
        when(paymentInfoDao.updateItem(payments)).thenReturn(Mono.error(expectedException));
        when(producer.sendEvent(any(PnNotificationCostValidationEvent.class))).thenReturn(outcomeEventProbe.mono());

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "update failed".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterService)
                .updateCostByPhase(CostUpdatePhaseInt.VALIDATION, notificationCosts);
        verify(paymentInfoDao).updateItem(payments);

        outcomeEventProbe.assertWasNotSubscribed();
    }

    @Test
    void handleNotificationCostInitializationEvent_shouldPropagateErrorWhenSendingOutcomeEventFails() {
        List<NotificationDeliveryCost> notificationCosts = buildNotificationCosts();
        List<PaymentInfo> payments = buildPayments();
        NotificationCostInitializationEvent.Payload payload = buildPayload(notificationCosts, payments);

        RuntimeException expectedException = new RuntimeException("event bridge failed");

        when(notificationCostUpdaterService.updateCostByPhase(CostUpdatePhaseInt.VALIDATION, notificationCosts))
                .thenReturn(Mono.empty());
        when(paymentInfoDao.updateItem(payments)).thenReturn(Mono.empty());
        when(producer.sendEvent(any(PnNotificationCostValidationEvent.class)))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(handler.handleNotificationCostInitializationEvent(payload))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "event bridge failed".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterService)
                .updateCostByPhase(CostUpdatePhaseInt.VALIDATION, notificationCosts);
        verify(paymentInfoDao).updateItem(payments);
        verify(producer).sendEvent(any(PnNotificationCostValidationEvent.class));
    }


    private NotificationCostInitializationEvent.Payload buildPayload(
            List<NotificationDeliveryCost> notificationCosts,
            List<PaymentInfo> payments
    ) {
        return NotificationCostInitializationEvent.Payload.builder()
                .iun(IUN)
                .notificationCosts(notificationCosts)
                .payments(payments)
                .build();
    }

    private List<NotificationDeliveryCost> buildNotificationCosts() {
        return List.of(
                NotificationDeliveryCostTestBuilder.builder()
                        .withIun(IUN)
                        .withRecIndex(0)
                        .withIsDeleted(false)
                        .build()
        );
    }

    private List<PaymentInfo> buildPayments() {
        return List.of(
                PaymentInfo.builder()
                        .iun(IUN)
                        .recIndex(0)
                        .iuv("IUV-123456")
                        .applyCost(true)
                        .build()
        );
    }
}
