package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.model.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import it.pagopa.pn.notificationcostservice.service.CostCalculator;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class NotificationCostServiceImplTest {

    @Mock
    private NotificationDeliveryCostDao notificationDeliveryCostDao;

    @Mock
    private NotificationDeliveryCostMapper notificationDeliveryCostMapper;

    @Mock
    private CostCalculator costCalculator;

    @Mock
    private MomProducer<NotificationCostInitializationEvent> notificationCostInitialization;

    @InjectMocks
    private NotificationCostServiceImpl notificationCostService;

    private static final String IUN = "TEST-IUN-12345";
    private static final Integer REC_INDEX = 0;

    @Test
    void getNotificationCostRecipient_ReturnsMappedResponseWhenItemExistsAndNotDeleted() {
       NotificationDeliveryCost notificationDeliveryCost = NotificationDeliveryCostTestBuilder.builder()
               .withIun(IUN)
               .withRecIndex(REC_INDEX)
               .withIsDeleted(false)
               .build();


       when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX)).thenReturn(Mono.just(notificationDeliveryCost));

       CalculatedCosts calculatedCosts = CalculatedCosts.builder().build();
       when(costCalculator.calculateCosts(notificationDeliveryCost)).thenReturn(calculatedCosts);

       NotificationCostRecipientResponseDto mappedResponse = new NotificationCostRecipientResponseDto();
       when(notificationDeliveryCostMapper.mapDtoToResponse(eq(notificationDeliveryCost), any())).thenReturn(mappedResponse);

       StepVerifier.create(notificationCostService.getNotificationCostRecipient(IUN, REC_INDEX))
               .expectNext(mappedResponse)
               .verifyComplete();

       verify(notificationDeliveryCostDao).getNotificationDeliveryCostItem(IUN, REC_INDEX);
       verify(costCalculator).calculateCosts(notificationDeliveryCost);
       verify(notificationDeliveryCostMapper).mapDtoToResponse(eq(notificationDeliveryCost), any());
    }
    @Test
    void getNotificationCostRecipient_ReturnsNotFoundErrorWhenItemIsDeleted() {
        NotificationDeliveryCost dto = NotificationDeliveryCostTestBuilder.builder()
                .withIun(IUN)
                .withRecIndex(REC_INDEX)
                .withIsDeleted(true)
                .build();

       when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX)).thenReturn(Mono.just(dto));

       StepVerifier.create(notificationCostService.getNotificationCostRecipient(IUN, REC_INDEX))
               .expectError(PnNotFoundException.class)
               .verify();

       verify(notificationDeliveryCostDao).getNotificationDeliveryCostItem(IUN, REC_INDEX);
       verifyNoInteractions(costCalculator);
       verifyNoInteractions(notificationDeliveryCostMapper);

    }

    @Test
    void getNotificationCostRecipient_PropagatesErrorWhenDaoReturnsError() {
       when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX))
               .thenReturn(Mono.error(new RuntimeException("DB error")));

       StepVerifier.create(notificationCostService.getNotificationCostRecipient(IUN, REC_INDEX))
               .expectError(RuntimeException.class)
               .verify();

       verify(notificationDeliveryCostDao).getNotificationDeliveryCostItem(IUN, REC_INDEX);
       verifyNoInteractions(costCalculator);
       verifyNoInteractions(notificationDeliveryCostMapper);
    }
    @Test
    void saveNotificationCost_CompletesAndPushesBuiltEvent() {
        List<NotificationDeliveryCost> notificationCosts = buildNotificationCosts();
        List<PaymentInfo> payments = buildPayments();
        ArgumentCaptor<NotificationCostInitializationEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationCostInitializationEvent.class);

        StepVerifier.create(notificationCostService.saveNotificationCost(IUN, notificationCosts, payments))
                .verifyComplete();

        verify(notificationCostInitialization).push(eventCaptor.capture());
        verifyNoInteractions(notificationDeliveryCostDao, notificationDeliveryCostMapper, costCalculator);

        NotificationCostInitializationEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertNotNull(event.getPayload());
        assertEquals(IUN, event.getPayload().getIun());
        assertEquals(notificationCosts, event.getPayload().getNotificationCosts());
        assertEquals(payments, event.getPayload().getPayments());
    }

    @Test
    void saveNotificationCost_RetriesAndEventuallyCompletesWhenPushFailsTransiently() {
        List<NotificationDeliveryCost> notificationCosts = buildNotificationCosts();
        List<PaymentInfo> payments = buildPayments();

        doThrow(new RuntimeException("temporary error 1"))
                .doThrow(new RuntimeException("temporary error 2"))
                .doNothing()
                .when(notificationCostInitialization)
                .push(any(NotificationCostInitializationEvent.class));

        StepVerifier.withVirtualTime(() ->
                        notificationCostService.saveNotificationCost(IUN, notificationCosts, payments))
                .thenAwait(Duration.ofSeconds(10))
                .verifyComplete();

        verify(notificationCostInitialization, times(3)).push(any(NotificationCostInitializationEvent.class));
    }

    @Test
    void saveNotificationCost_ShouldLogAndThrowWhenRetriesAreExhausted() {
        List<NotificationDeliveryCost> notificationCosts = buildNotificationCosts();
        List<PaymentInfo> payments = buildPayments();

        String errorMessage = "permanent database error";
        RuntimeException permanentException = new RuntimeException(errorMessage);

        doThrow(permanentException)
                .when(notificationCostInitialization)
                .push(any(NotificationCostInitializationEvent.class));

        StepVerifier.withVirtualTime(() ->
                        notificationCostService.saveNotificationCost(IUN, notificationCosts, payments))
                .thenAwait(Duration.ofSeconds(10))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                errorMessage.equals(throwable.getMessage()))
                .verify();

        verify(notificationCostInitialization, times(4)).push(any(NotificationCostInitializationEvent.class));
        verifyNoInteractions(notificationDeliveryCostDao, notificationDeliveryCostMapper, costCalculator);
    }

    private List<NotificationDeliveryCost> buildNotificationCosts() {
        NotificationDeliveryCost notificationDeliveryCost = NotificationDeliveryCostTestBuilder.builder()
                .withIun(IUN)
                .withRecIndex(REC_INDEX)
                .withIsDeleted(false)
                .build();

        return List.of(notificationDeliveryCost);
    }

    private List<PaymentInfo> buildPayments() {
        PaymentInfo paymentInfo = PaymentInfo.builder()
                .iun(IUN)
                .recIndex(REC_INDEX)
                .iuv("IUV-123456789")
                .applyCost(true)
                .build();

        return List.of(paymentInfo);
    }
}
