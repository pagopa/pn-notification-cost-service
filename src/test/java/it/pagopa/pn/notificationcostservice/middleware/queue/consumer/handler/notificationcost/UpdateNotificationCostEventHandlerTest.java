package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler.notificationcost;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.UpdateNotificationCostEvent;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.NotificationDeliveryCostDaoDynamo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.List;
import java.util.Comparator;

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
    private NotificationDeliveryCostDaoDynamo notificationCostUpdateDao;

    @Mock
    private Page<NotificationDeliveryCostEntity> notificationDeliveryCostEntityPage;

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

        verifyNoInteractions(notificationCostUpdaterService, notificationCostUpdateDao);
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

        verify(notificationCostUpdaterService)
                .updateCostByPhase(notificationCostsCaptor.capture());
        verifyNoInteractions(notificationCostUpdateDao);

        NotificationCostUpdate notificationCostUpdate = getSingleCapturedNotificationCost(notificationCostsCaptor);
        assertStandardNotificationCost(notificationCostUpdate, IUN, REC_INDEX, COST, PRODUCT_TYPE,
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

        verify(notificationCostUpdaterService)
                .updateCostByPhase(notificationCostsCaptor.capture());
        verifyNoInteractions(notificationCostUpdateDao);

        NotificationCostUpdate notificationCostUpdate = getSingleCapturedNotificationCost(notificationCostsCaptor);
        assertStandardNotificationCost(notificationCostUpdate, IUN, REC_INDEX, COST, PRODUCT_TYPE,
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

        verify(notificationCostUpdaterService)
                .updateCostByPhase(notificationCostsCaptor.capture());
        verifyNoInteractions(notificationCostUpdateDao);

        NotificationCostUpdate notificationCostUpdate = getSingleCapturedNotificationCost(notificationCostsCaptor);
        assertStandardNotificationCost(notificationCostUpdate, IUN, REC_INDEX, COST, PRODUCT_TYPE,
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

        when(notificationCostUpdateDao.getAllByIun(IUN)).thenReturn(Mono.just(notificationDeliveryCostEntityPage));
        when(notificationDeliveryCostEntityPage.items()).thenReturn(entities);
        when(notificationCostUpdaterService.updateCostByPhase(any(NotificationCostUpdate.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .verifyComplete();

        verify(notificationCostUpdateDao).getAllByIun(IUN);
        verify(notificationCostUpdaterService, times(2))
                .updateCostByPhase(notificationCostsCaptor.capture());

        List<NotificationCostUpdate> captured = notificationCostsCaptor.getAllValues().stream()
                .sorted(Comparator.comparing(NotificationCostUpdate::getRecIndex))
                .toList();
        assertEquals(2, captured.size());
        assertDeletedNotificationCost(captured.get(0), IUN, 0, CostUpdatePhaseInt.REQUEST_REFUSED);
        assertDeletedNotificationCost(captured.get(1), IUN, 1, CostUpdatePhaseInt.REQUEST_REFUSED);
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

        when(notificationCostUpdateDao.getAllByIun(IUN)).thenReturn(Mono.just(notificationDeliveryCostEntityPage));
        when(notificationDeliveryCostEntityPage.items()).thenReturn(entities);
        when(notificationCostUpdaterService.updateCostByPhase(any(NotificationCostUpdate.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .verifyComplete();

        verify(notificationCostUpdateDao).getAllByIun(IUN);
        verify(notificationCostUpdaterService, times(2))
                .updateCostByPhase(notificationCostsCaptor.capture());

        List<NotificationCostUpdate> captured = notificationCostsCaptor.getAllValues().stream()
                .sorted(Comparator.comparing(NotificationCostUpdate::getRecIndex))
                .toList();
        assertEquals(2, captured.size());
        assertDeletedNotificationCost(captured.get(0), IUN, 1, CostUpdatePhaseInt.NOTIFICATION_CANCELLED);
        assertDeletedNotificationCost(captured.get(1), IUN, 3, CostUpdatePhaseInt.NOTIFICATION_CANCELLED);
    }

    @Test
    void handleUpdateNotificationCostEvent_shouldCompleteWithoutInvokingUpdaterWhenPhaseIsValidation() {
        UpdateNotificationCostEvent.Payload payload = basePayloadBuilder()
                .costUpdatePhase(CostUpdatePhaseInt.VALIDATION)
                .build();

        StepVerifier.create(handler.handleUpdateNotificationCostEvent(payload))
                .verifyComplete();

        verifyNoInteractions(notificationCostUpdaterService, notificationCostUpdateDao);
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

    private void assertStandardNotificationCost(
            NotificationCostUpdate notificationCostUpdate,
            String iun,
            int recIndex,
            int cost,
            String productType,
            CostUpdatePhaseInt costUpdatePhase
    ) {
        assertEquals(iun, notificationCostUpdate.getIun());
        assertEquals(recIndex, notificationCostUpdate.getRecIndex());
        assertEquals(cost, notificationCostUpdate.getCost());
        assertEquals(productType, notificationCostUpdate.getProductType());
        assertEquals(costUpdatePhase, notificationCostUpdate.getCostUpdatePhase());
    }

    private void assertDeletedNotificationCost(
            NotificationCostUpdate notificationCostUpdate,
            String iun,
            int recIndex,
            CostUpdatePhaseInt costUpdatePhase
    ) {
        assertEquals(iun, notificationCostUpdate.getIun());
        assertEquals(recIndex, notificationCostUpdate.getRecIndex());
        assertEquals(0, notificationCostUpdate.getCost());
        assertNull(notificationCostUpdate.getProductType());
        assertEquals(costUpdatePhase, notificationCostUpdate.getCostUpdatePhase());
    }
}

