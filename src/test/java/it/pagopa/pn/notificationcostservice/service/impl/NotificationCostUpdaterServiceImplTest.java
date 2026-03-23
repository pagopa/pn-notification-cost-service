package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationCostUpdaterMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCostUpdaterServiceImplTest {

    private static final String IUN_1 = "TEST-IUN-1";
    private static final String IUN_2 = "TEST-IUN-2";

    @Mock
    private NotificationDeliveryCostDao notificationDeliveryCostDao;

    @Mock
    private NotificationCostUpdaterMapper notificationCostUpdaterMapper;

    @InjectMocks
    private NotificationCostUpdaterServiceImpl service;

    @Test
    void updateCostByPhase_shouldCompleteWhenNotificationDeliveryCostsIsNull() {
        StepVerifier.create(service.updateCostByPhase(CostUpdatePhaseInt.VALIDATION, null))
                .verifyComplete();

        verifyNoInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    @Test
    void updateCostByPhase_shouldCompleteWhenNotificationDeliveryCostsIsEmpty() {
        StepVerifier.create(service.updateCostByPhase(CostUpdatePhaseInt.VALIDATION, List.of()))
                .verifyComplete();

        verifyNoInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    @Test
    void updateCostByPhase_shouldMapAndUpdateEachItemInOrder() {
        NotificationDeliveryCost notification1 = buildNotification(IUN_1, 0, 100, 50, 22);
        NotificationDeliveryCost notification2 = buildNotification(IUN_2, 1, 200, 70, 10);

        NotificationDeliveryCostEntity entity1 = buildEntity(IUN_1, 0, 100, 50, 22);
        NotificationDeliveryCostEntity entity2 = buildEntity(IUN_2, 1, 200, 70, 10);

        when(notificationCostUpdaterMapper.mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification1))
                .thenReturn(entity1);
        when(notificationCostUpdaterMapper.mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification2))
                .thenReturn(entity2);

        when(notificationDeliveryCostDao.updateNotificationDeliveryCostsItem(List.of(entity1)))
                .thenReturn(Mono.empty());
        when(notificationDeliveryCostDao.updateNotificationDeliveryCostsItem(List.of(entity2)))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.updateCostByPhase(
                        CostUpdatePhaseInt.VALIDATION,
                        List.of(notification1, notification2)
                ))
                .verifyComplete();

        InOrder inOrder = inOrder(notificationCostUpdaterMapper, notificationDeliveryCostDao);
        inOrder.verify(notificationCostUpdaterMapper)
                .mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification1);
        inOrder.verify(notificationDeliveryCostDao)
                .updateNotificationDeliveryCostsItem(List.of(entity1));
        inOrder.verify(notificationCostUpdaterMapper)
                .mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification2);
        inOrder.verify(notificationDeliveryCostDao)
                .updateNotificationDeliveryCostsItem(List.of(entity2));

        verifyNoMoreInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    @Test
    void updateCostByPhase_shouldPropagateMapperError() {
        NotificationDeliveryCost notification = buildNotification(IUN_1, 0, 100, 50, 22);
        RuntimeException expectedException = new RuntimeException("mapper error");

        when(notificationCostUpdaterMapper.mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification))
                .thenThrow(expectedException);

        StepVerifier.create(service.updateCostByPhase(
                        CostUpdatePhaseInt.VALIDATION,
                        List.of(notification)
                ))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "mapper error".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterMapper)
                .mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification);
        verifyNoInteractions(notificationDeliveryCostDao);
    }

    @Test
    void updateCostByPhase_shouldPropagateDaoError() {
        NotificationDeliveryCost notification = buildNotification(IUN_1, 0, 100, 50, 22);
        NotificationDeliveryCostEntity entity = buildEntity(IUN_1, 0, 100, 50, 22);
        RuntimeException expectedException = new RuntimeException("dao error");

        when(notificationCostUpdaterMapper.mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification))
                .thenReturn(entity);
        when(notificationDeliveryCostDao.updateNotificationDeliveryCostsItem(List.of(entity)))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(service.updateCostByPhase(
                        CostUpdatePhaseInt.VALIDATION,
                        List.of(notification)
                ))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "dao error".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterMapper)
                .mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification);
        verify(notificationDeliveryCostDao)
                .updateNotificationDeliveryCostsItem(List.of(entity));
        verifyNoMoreInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    @Test
    void updateCostByPhase_shouldStopProcessingWhenFirstDaoUpdateFails() {
        NotificationDeliveryCost notification1 = buildNotification(IUN_1, 0, 100, 50, 22);
        NotificationDeliveryCost notification2 = buildNotification(IUN_2, 1, 200, 70, 10);

        NotificationDeliveryCostEntity entity1 = buildEntity(IUN_1, 0, 100, 50, 22);
        RuntimeException expectedException = new RuntimeException("first update failed");

        when(notificationCostUpdaterMapper.mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification1))
                .thenReturn(entity1);
        when(notificationDeliveryCostDao.updateNotificationDeliveryCostsItem(List.of(entity1)))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(service.updateCostByPhase(
                        CostUpdatePhaseInt.VALIDATION,
                        List.of(notification1, notification2)
                ))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "first update failed".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterMapper)
                .mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification1);
        verify(notificationDeliveryCostDao)
                .updateNotificationDeliveryCostsItem(List.of(entity1));

        verify(notificationCostUpdaterMapper, never())
                .mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, notification2);
        verifyNoMoreInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    private NotificationDeliveryCost buildNotification(
            String iun,
            int recIndex,
            int sendFee,
            int paFee,
            int vat
    ) {
        return NotificationDeliveryCostTestBuilder.builder()
                .withIun(iun)
                .withRecIndex(recIndex)
                .withBaseCost(BaseCost.builder()
                        .sendFee(sendFee)
                        .paFee(paFee)
                        .build())
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.SYNC)
                .withVat(vat)
                .build();
    }

    private NotificationDeliveryCostEntity buildEntity(
            String iun,
            int recIndex,
            int sendFee,
            int paFee,
            int vat
    ) {
        return NotificationDeliveryCostEntity.builder()
                .iun(iun)
                .recIndex(recIndex)
                .baseCost(BaseCostEntity.builder()
                        .sendFee(sendFee)
                        .paFee(paFee)
                        .build())
                .vat(vat)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .build();
    }
}
