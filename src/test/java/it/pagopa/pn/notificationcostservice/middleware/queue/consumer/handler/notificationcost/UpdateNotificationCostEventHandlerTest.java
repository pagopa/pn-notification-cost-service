package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.UpdateNotificationCostEvent;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateNotificationCostEventHandlerTest {

    private static final String IUN = "TEST-IUN-123";
    private static final Integer REC_INDEX = 2;
    private static final Integer COST = 926;
    private static final String PRODUCT_TYPE = "AR_REGISTERED_LETTER";

    @Mock
    private NotificationCostUpdaterService notificationCostUpdaterService;

    @Mock
    private NotificationDeliveryCostDao notificationDeliveryCostDao;

    @Mock
    private PaymentInfoDao paymentInfoDao;

    @InjectMocks
    private UpdateNotificationCostEventHandler handler;

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenCostUpdatePhaseIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(null)
                .build();

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .expectError(PnInternalException.class)
                .verify();

        verifyNoInteractions(notificationCostUpdaterService, notificationDeliveryCostDao, paymentInfoDao);
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldMapSimpleRegisteredLetter() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_SIMPLE_REGISTERED_LETTER)
                .build();
        ArgumentCaptor<NotificationCostUpdate> notificationCostsCaptor = ArgumentCaptor.forClass(NotificationCostUpdate.class);

        when(notificationCostUpdaterService.updateCostByPhase(any(NotificationCostUpdate.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .verifyComplete();

        verify(notificationCostUpdaterService).updateCostByPhase(notificationCostsCaptor.capture());
        verifyNoInteractions(notificationDeliveryCostDao, paymentInfoDao);

        NotificationCostUpdate notificationCostUpdate = getSingleCapturedNotificationCost(notificationCostsCaptor);
        assertStandardNotificationCost(notificationCostUpdate,
                CostUpdatePhaseInt.SEND_SIMPLE_REGISTERED_LETTER);
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldMapSendAnalogDomicileAttemptZero() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0)
                .build();
        ArgumentCaptor<NotificationCostUpdate> notificationCostsCaptor = ArgumentCaptor.forClass(NotificationCostUpdate.class);

        when(notificationCostUpdaterService.updateCostByPhase(any(NotificationCostUpdate.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .verifyComplete();

        verify(notificationCostUpdaterService).updateCostByPhase(notificationCostsCaptor.capture());
        verifyNoInteractions(notificationDeliveryCostDao, paymentInfoDao);

        NotificationCostUpdate notificationCostUpdate = getSingleCapturedNotificationCost(notificationCostsCaptor);
        assertStandardNotificationCost(notificationCostUpdate,
                CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0);
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldMapSendAnalogDomicileAttemptOne() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_1)
                .build();
        ArgumentCaptor<NotificationCostUpdate> notificationCostsCaptor = ArgumentCaptor.forClass(NotificationCostUpdate.class);

        when(notificationCostUpdaterService.updateCostByPhase(any(NotificationCostUpdate.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .verifyComplete();

        verify(notificationCostUpdaterService).updateCostByPhase(notificationCostsCaptor.capture());
        verifyNoInteractions(notificationDeliveryCostDao, paymentInfoDao);

        NotificationCostUpdate notificationCostUpdate = getSingleCapturedNotificationCost(notificationCostsCaptor);
        assertStandardNotificationCost(notificationCostUpdate,
                CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_1);
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldMapRequestRefusedForAllRecipients() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.REQUEST_REFUSED)
                .build();
        ArgumentCaptor<NotificationCostUpdate> notificationCostsCaptor = ArgumentCaptor.forClass(NotificationCostUpdate.class);
        List<NotificationDeliveryCostEntity> entities = List.of(
                NotificationDeliveryCostEntity.builder().iun(IUN).recIndex(0).build(),
                NotificationDeliveryCostEntity.builder().iun(IUN).recIndex(1).build()
        );

        when(paymentInfoDao.deleteItemsByIun(IUN)).thenReturn(Mono.empty());
        when(notificationDeliveryCostDao.getAllByIun(IUN)).thenReturn(Flux.fromIterable(entities));
        when(notificationCostUpdaterService.updateCostByPhase(any(NotificationCostUpdate.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .verifyComplete();

        verify(paymentInfoDao).deleteItemsByIun(IUN);
        verify(notificationDeliveryCostDao).getAllByIun(IUN);
        verify(notificationCostUpdaterService, times(2))
                .updateCostByPhase(notificationCostsCaptor.capture());

        List<NotificationCostUpdate> captured = notificationCostsCaptor.getAllValues().stream()
                .sorted(Comparator.comparing(NotificationCostUpdate::getRecIndex))
                .toList();

        assertEquals(2, captured.size());
        assertDeletedNotificationCost(captured.get(0), 0, CostUpdatePhaseInt.REQUEST_REFUSED);
        assertDeletedNotificationCost(captured.get(1), 1, CostUpdatePhaseInt.REQUEST_REFUSED);
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldMapNotificationCancelledForAllRecipients() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.NOTIFICATION_CANCELLED)
                .build();
        ArgumentCaptor<NotificationCostUpdate> notificationCostsCaptor = ArgumentCaptor.forClass(NotificationCostUpdate.class);
        List<NotificationDeliveryCostEntity> entities = List.of(
                NotificationDeliveryCostEntity.builder().iun(IUN).recIndex(1).build(),
                NotificationDeliveryCostEntity.builder().iun(IUN).recIndex(3).build()
        );

        when(paymentInfoDao.deleteItemsByIun(IUN)).thenReturn(Mono.empty());
        when(notificationDeliveryCostDao.getAllByIun(IUN)).thenReturn(Flux.fromIterable(entities));
        when(notificationCostUpdaterService.updateCostByPhase(any(NotificationCostUpdate.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .verifyComplete();

        verify(paymentInfoDao).deleteItemsByIun(IUN);
        verify(notificationDeliveryCostDao).getAllByIun(IUN);
        verify(notificationCostUpdaterService, times(2))
                .updateCostByPhase(notificationCostsCaptor.capture());

        List<NotificationCostUpdate> captured = notificationCostsCaptor.getAllValues().stream()
                .sorted(Comparator.comparing(NotificationCostUpdate::getRecIndex))
                .toList();
        assertEquals(2, captured.size());
        assertDeletedNotificationCost(captured.get(0), 1, CostUpdatePhaseInt.NOTIFICATION_CANCELLED);
        assertDeletedNotificationCost(captured.get(1), 3, CostUpdatePhaseInt.NOTIFICATION_CANCELLED);
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldCompleteWithoutInvokingUpdaterWhenPhaseIsValidation() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.VALIDATION)
                .build();

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .expectError(PnInternalException.class)
                .verify();

        verifyNoInteractions(notificationCostUpdaterService, notificationDeliveryCostDao, paymentInfoDao);
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenSimpleRegisteredLetterCostIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_SIMPLE_REGISTERED_LETTER)
                .cost(null)
                .build();

        assertMissingFieldValidationError(payload, "cost = null");
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenSimpleRegisteredLetterProductTypeIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_SIMPLE_REGISTERED_LETTER)
                .productType(null)
                .build();

        assertMissingFieldValidationError(payload, "productType = null");
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenSimpleRegisteredLetterRecIndexIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_SIMPLE_REGISTERED_LETTER)
                .recIndex(null)
                .build();

        assertMissingFieldValidationError(payload, "recIndex = null");
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenSendAnalogDomicileAttemptZeroCostIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0)
                .cost(null)
                .build();

        assertMissingFieldValidationError(payload, "cost = null");
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenSendAnalogDomicileAttemptZeroProductTypeIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0)
                .productType(null)
                .build();

        assertMissingFieldValidationError(payload, "productType = null");
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenSendAnalogDomicileAttemptZeroRecIndexIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0)
                .recIndex(null)
                .build();

        assertMissingFieldValidationError(payload, "recIndex = null");
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenSendAnalogDomicileAttemptOneCostIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_1)
                .cost(null)
                .build();

        assertMissingFieldValidationError(payload, "cost = null");
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenSendAnalogDomicileAttemptOneProductTypeIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_1)
                .productType(null)
                .build();

        assertMissingFieldValidationError(payload, "productType = null");
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldErrorWhenSendAnalogDomicileAttemptOneRecIndexIsNull() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_1)
                .recIndex(null)
                .build();

        assertMissingFieldValidationError(payload, "recIndex = null");
    }

    private UpdateNotificationCostEvent.Payload.PayloadBuilder basePayloadBuilder() {
        return UpdateNotificationCostEvent.Payload.builder()
                .iun(IUN)
                .recIndex(REC_INDEX)
                .cost(COST)
                .productType(PRODUCT_TYPE);
    }

    private NotificationCostUpdate getSingleCapturedNotificationCost(
            ArgumentCaptor<NotificationCostUpdate> notificationCostsCaptor
    ) {
        return notificationCostsCaptor.getValue();
    }

    private void assertMissingFieldValidationError(UpdateNotificationCostEvent.Payload payload, String expectedMessageFragment) {
        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(PnInternalException.class, ex);
                    String detail = ((PnInternalException) ex).getProblem().getDetail();
                    assertNotNull(detail);
                    assertTrue(detail.contains("Missing required field"));
                    assertTrue(detail.contains(expectedMessageFragment));
                })
                .verify();

        verifyNoInteractions(notificationCostUpdaterService, notificationDeliveryCostDao, paymentInfoDao);
    }

    private void assertStandardNotificationCost(
            NotificationCostUpdate notificationCostUpdate,
            CostUpdatePhaseInt costUpdatePhase
    ) {
        assertEquals(IUN, notificationCostUpdate.getIun());
        assertEquals(REC_INDEX, notificationCostUpdate.getRecIndex());
        assertEquals(COST, notificationCostUpdate.getCost());
        assertEquals(PRODUCT_TYPE, notificationCostUpdate.getProductType());
        assertEquals(costUpdatePhase, notificationCostUpdate.getCostUpdatePhase());
    }

    private void assertDeletedNotificationCost(
            NotificationCostUpdate notificationCostUpdate,
            int recIndex,
            CostUpdatePhaseInt costUpdatePhase
    ) {
        assertEquals(IUN, notificationCostUpdate.getIun());
        assertEquals(recIndex, notificationCostUpdate.getRecIndex());
        assertNull(notificationCostUpdate.getCost());
        assertNull(notificationCostUpdate.getProductType());
        assertEquals(costUpdatePhase, notificationCostUpdate.getCostUpdatePhase());
    }
}
