package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.model.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.NotificationCostRequest;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.RecipientCostData;
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
        NotificationCostRequest request = buildNotificationCostRequest();
        ArgumentCaptor<NotificationCostInitializationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCostInitializationEvent.class);
        StepVerifier.create(notificationCostService.saveNotificationCost(IUN, request))
                .verifyComplete();

        verify(notificationCostInitialization).push(eventCaptor.capture());
        verifyNoInteractions(notificationDeliveryCostDao, notificationDeliveryCostMapper, costCalculator);

        NotificationCostInitializationEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals(IUN, event.getPayload().getIun());
        assertEquals(request.getRecipients(), event.getPayload().getRecipients());
    }

    @Test
    void saveNotificationCost_RetriesAndEventuallyCompletesWhenPushFailsTransiently() {
        NotificationCostRequest request = buildNotificationCostRequest();

        doThrow(new RuntimeException("temporary error 1"))
                .doThrow(new RuntimeException("temporary error 2"))
                .doNothing()
                .when(notificationCostInitialization)
                .push(any(NotificationCostInitializationEvent.class));

        StepVerifier.withVirtualTime(() -> notificationCostService.saveNotificationCost(IUN, request))
                .thenAwait(Duration.ofSeconds(10))
                .verifyComplete();

        verify(notificationCostInitialization, times(3)).push(any(NotificationCostInitializationEvent.class));
    }

    @Test
    void saveNotificationCost_ShouldLogAndThrowWhenRetriesAreExhausted() {
        NotificationCostRequest request = buildNotificationCostRequest();
        String errorMessage = "permanent database error";
        RuntimeException permanentException = new RuntimeException(errorMessage);
        doThrow(permanentException)
                .when(notificationCostInitialization)
                .push(any(NotificationCostInitializationEvent.class));
        StepVerifier.withVirtualTime(() -> notificationCostService.saveNotificationCost(IUN, request))
                .thenAwait(Duration.ofSeconds(10)) // Superiamo il tempo di backoff
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals(errorMessage))
                .verify();

        // 1 tentativo iniziale + 3 retry = 4 push totali
        verify(notificationCostInitialization, times(4)).push(any(NotificationCostInitializationEvent.class));
        verifyNoInteractions(notificationDeliveryCostDao, notificationDeliveryCostMapper, costCalculator);
    }

    private NotificationCostRequest buildNotificationCostRequest() {
        RecipientCostData recipient = RecipientCostData.builder()
                .recIndex(REC_INDEX)
                .recipientInternalId("recipient-internal-id")
                .senderInternalId("sender-internal-id")
                .baseCost(100)
                .sendFee(20)
                .paFee(10)
                .vat(22)
                .build();
        return NotificationCostRequest.builder()
                .recipients(List.of(recipient))
                .build();
    }
}
