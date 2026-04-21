package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationCostUpdaterMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCostUpdaterServiceImplTest {

    private static final String IUN_1 = "TEST-IUN-1";
    private static final Integer REC_INDEX = 0;
    private static final Integer COST = 100;
    private static final String PRODUCT_TYPE = "AR_REGISTERED_LETTER";

    @Mock
    private NotificationDeliveryCostDao notificationDeliveryCostDao;

    @Mock
    private NotificationCostUpdaterMapper notificationCostUpdaterMapper;

    @InjectMocks
    private NotificationCostUpdaterServiceImpl service;

    @Test
    void updateBaseCost_shouldErrorWhenNotificationDeliveryCostsIsNull() {
        StepVerifier.create(service.updateBaseCost(null))
                .expectError(PnInternalException.class)
                .verify();

        verifyNoInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    @Test
    void updateBaseCost_shouldCompleteWhenGivenNotificationCostIsMappedAndUpdated() {
        NotificationDeliveryCost notificationDeliveryCost = buildNotificationDeliveryCost();
        NotificationDeliveryCostEntity entity = buildEntity();

        when(notificationCostUpdaterMapper.toEntityForBaseCostUpdate(notificationDeliveryCost))
                .thenReturn(entity);
        when(notificationDeliveryCostDao.updateBaseCostIfNotExistsOrMatch(entity))
                .thenReturn(Mono.just(entity));

        StepVerifier.create(service.updateBaseCost(notificationDeliveryCost))
                .verifyComplete();

        verify(notificationCostUpdaterMapper, times(1))
                .toEntityForBaseCostUpdate(notificationDeliveryCost);
        verify(notificationDeliveryCostDao, times(1))
                .updateBaseCostIfNotExistsOrMatch(entity);
        verify(notificationDeliveryCostDao, never())
                .updateNotificationDeliveryCostNotNull(any());
        verifyNoMoreInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }


    @Test
    void updateBaseCost_shouldPropagateMapperError() {
        NotificationDeliveryCost notificationDeliveryCost = buildNotificationDeliveryCost();

        when(notificationCostUpdaterMapper.toEntityForBaseCostUpdate(notificationDeliveryCost))
                .thenThrow(new RuntimeException("mapper error"));

        StepVerifier.create(service.updateBaseCost(notificationDeliveryCost))
                .expectError(RuntimeException.class)
                .verify();

        verify(notificationCostUpdaterMapper, times(1))
                .toEntityForBaseCostUpdate(notificationDeliveryCost);
        verifyNoInteractions(notificationDeliveryCostDao);
    }

    @Test
    void updateBaseCost_shouldPropagateDaoError() {
        NotificationDeliveryCost notificationDeliveryCost = buildNotificationDeliveryCost();
        NotificationDeliveryCostEntity entity = buildEntity();
        RuntimeException expectedException = new RuntimeException("dao error");

        when(notificationCostUpdaterMapper.toEntityForBaseCostUpdate(notificationDeliveryCost))
                .thenReturn(entity);
        when(notificationDeliveryCostDao.updateBaseCostIfNotExistsOrMatch(entity))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(service.updateBaseCost(notificationDeliveryCost))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "dao error".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterMapper, times(1))
                .toEntityForBaseCostUpdate(notificationDeliveryCost);
        verify(notificationDeliveryCostDao, times(1))
                .updateBaseCostIfNotExistsOrMatch(entity);
        verify(notificationDeliveryCostDao, never())
                .updateNotificationDeliveryCostNotNull(any());
        verifyNoMoreInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    @Test
    void updateCostByPhase_shouldErrorWhenNotificationCostUpdateIsNull() {
        StepVerifier.create(service.updateCostByPhase(null))
                .expectError(PnInternalException.class)
                .verify();

        verifyNoInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    @Test
    void updateCostByPhase_shouldCompleteWhenGivenNotificationCostUpdateIsMappedAndUpdated() {
        NotificationCostUpdate notificationCostUpdate = buildNotificationCostUpdate(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0);
        NotificationDeliveryCostEntity entity = buildEntity();

        when(notificationCostUpdaterMapper.mapNotificationCostUpdater(notificationCostUpdate))
                .thenReturn(entity);
        when(notificationDeliveryCostDao.updateNotificationDeliveryCostNotNull(entity))
                .thenReturn(Mono.just(entity));

        StepVerifier.create(service.updateCostByPhase(notificationCostUpdate))
                .verifyComplete();

        verify(notificationCostUpdaterMapper, times(1))
                .mapNotificationCostUpdater(notificationCostUpdate);
        verify(notificationDeliveryCostDao, times(1))
                .updateNotificationDeliveryCostNotNull(entity);
        verifyNoMoreInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    @Test
    void updateCostByPhase_shouldPropagateMapperError() {
        NotificationCostUpdate notificationCostUpdate = buildNotificationCostUpdate(CostUpdatePhaseInt.SEND_SIMPLE_REGISTERED_LETTER);

        when(notificationCostUpdaterMapper.mapNotificationCostUpdater(notificationCostUpdate))
                .thenThrow(new RuntimeException("mapper error"));

        RuntimeException ex = Assertions.assertThrows(RuntimeException.class,
                () -> service.updateCostByPhase(notificationCostUpdate).block());
        Assertions.assertEquals("mapper error", ex.getMessage());

        verify(notificationCostUpdaterMapper, times(1))
                .mapNotificationCostUpdater(notificationCostUpdate);
        verifyNoInteractions(notificationDeliveryCostDao);
    }

    @Test
    void updateCostByPhase_shouldPropagateDaoError() {
        NotificationCostUpdate notificationCostUpdate = buildNotificationCostUpdate(CostUpdatePhaseInt.REQUEST_REFUSED);
        NotificationDeliveryCostEntity entity = buildEntity();
        RuntimeException expectedException = new RuntimeException("dao error");

        when(notificationCostUpdaterMapper.mapNotificationCostUpdater(notificationCostUpdate))
                .thenReturn(entity);
        when(notificationDeliveryCostDao.updateNotificationDeliveryCostNotNull(entity))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(service.updateCostByPhase(notificationCostUpdate))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "dao error".equals(ex.getMessage()))
                .verify();

        verify(notificationCostUpdaterMapper, times(1))
                .mapNotificationCostUpdater(notificationCostUpdate);
        verify(notificationDeliveryCostDao, times(1))
                .updateNotificationDeliveryCostNotNull(entity);
        verifyNoMoreInteractions(notificationCostUpdaterMapper, notificationDeliveryCostDao);
    }

    private NotificationDeliveryCost buildNotificationDeliveryCost() {
        return NotificationDeliveryCostTestBuilder.builder()
                .withIun(IUN_1)
                .withRecIndex(0)
                .withBaseCost(BaseCost.builder()
                        .sendFee(100)
                        .paFee(50)
                        .build())
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.SYNC)
                .withVat(22)
                .withSenderPaId("TEST-SENDER-PA-ID")
                .withSenderTaxId("TEST-SENDER-TAX-ID")
                .withLastUpdate(Instant.now())
                .build();
    }

    private NotificationDeliveryCostEntity buildEntity() {
        return NotificationDeliveryCostEntity.builder()
                .iun(IUN_1)
                .recIndex(REC_INDEX)
                .baseCost(BaseCostEntity.builder()
                        .sendFee(100)
                        .paFee(50)
                        .build())
                .vat(22)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .lastUpdate(Instant.now())
                .build();
    }

    private NotificationCostUpdate buildNotificationCostUpdate(CostUpdatePhaseInt costUpdatePhase) {
        return NotificationCostUpdate.builder()
                .iun(IUN_1)
                .recIndex(REC_INDEX)
                .cost(COST)
                .productType(PRODUCT_TYPE)
                .costUpdatePhase(costUpdatePhase)
                .elementTimestamp(Instant.now())
                .build();
    }
}
