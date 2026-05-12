package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogUpdateCostPhaseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NewNotificationCostRequestDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostPaymentResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.PaperCostToInvalidateDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.model.ValidationStatus;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import it.pagopa.pn.notificationcostservice.service.NotificationCostService;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.service.mapper.PaymentInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class NotificationCostServiceControllerTest {

    @Mock
    private NotificationCostService notificationCostService;

    @Mock
    private NotificationCostUpdaterService notificationCostUpdaterService;

    @Mock
    private NotificationDeliveryCostDao notificationDeliveryCostDao;

    @InjectMocks
    private NotificationCostServiceController controller;

    @Mock
    private NotificationDeliveryCostMapper mapper;

    @Mock
    private PaymentInfoMapper paymentInfoMapper;

    private static final String TEST_IUN = "TEST-IUN-123";
    private static final Integer TEST_REC_INDEX = 0;

    @Test
    void testNotificationCostRecipient_Success() {
        NotificationCostRecipientResponseDto response = new NotificationCostRecipientResponseDto();

        when(notificationCostService.getNotificationCostRecipient(TEST_IUN, TEST_REC_INDEX))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<NotificationCostRecipientResponseDto>> result =
                controller.getNotificationCost(TEST_IUN, TEST_REC_INDEX, null);

        StepVerifier.create(result)
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertNotNull(responseEntity.getBody());
                    assertEquals(response, responseEntity.getBody());
                })
                .verifyComplete();

        verify(notificationCostService).getNotificationCostRecipient(TEST_IUN, TEST_REC_INDEX);
    }

    @Test
    void initializeNotificationCostTest() {
        String iun = TEST_IUN;
        NewNotificationCostRequestDto requestDto = new NewNotificationCostRequestDto();
        ServerWebExchange exchange = mock(ServerWebExchange.class);

        List<NotificationDeliveryCost> notificationCosts = List.of(mock(NotificationDeliveryCost.class));
        List<PaymentInfo> payments = List.of(mock(PaymentInfo.class));

        when(mapper.mapDtoToNotificationDeliveryCost(iun, requestDto)).thenReturn(notificationCosts);
        when(paymentInfoMapper.mapDtoToPaymentInfo(iun, requestDto)).thenReturn(payments);
        when(notificationCostService.saveNotificationCost(iun, notificationCosts, payments))
                .thenReturn(Mono.empty());

        Mono<ResponseEntity<String>> result =
                controller.initializeNotificationCost(iun, Mono.just(requestDto), exchange);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(ValidationStatus.OK.name(), response.getBody());
                })
                .verifyComplete();

        verify(mapper).mapDtoToNotificationDeliveryCost(iun, requestDto);
        verify(paymentInfoMapper).mapDtoToPaymentInfo(iun, requestDto);
        verify(notificationCostService).saveNotificationCost(iun, notificationCosts, payments);
    }

    @Test
    void initializeNotificationCostPropagatesServiceError() {
        NewNotificationCostRequestDto requestDto = new NewNotificationCostRequestDto();
        List<NotificationDeliveryCost> notificationCosts = List.of(mock(NotificationDeliveryCost.class));
        List<PaymentInfo> payments = List.of(mock(PaymentInfo.class));
        IllegalStateException expected = new IllegalStateException("enqueue failed");

        when(mapper.mapDtoToNotificationDeliveryCost(TEST_IUN, requestDto)).thenReturn(notificationCosts);
        when(paymentInfoMapper.mapDtoToPaymentInfo(TEST_IUN, requestDto)).thenReturn(payments);
        when(notificationCostService.saveNotificationCost(TEST_IUN, notificationCosts, payments))
                .thenReturn(Mono.error(expected));

        StepVerifier.create(controller.initializeNotificationCost(TEST_IUN, Mono.just(requestDto), null))
                .expectErrorMatches(throwable -> throwable == expected)
                .verify();

        verify(mapper).mapDtoToNotificationDeliveryCost(eq(TEST_IUN), same(requestDto));
        verify(paymentInfoMapper).mapDtoToPaymentInfo(eq(TEST_IUN), same(requestDto));
        verify(notificationCostService).saveNotificationCost(TEST_IUN, notificationCosts, payments);
    }

    @Test
    void testNotificationCostByPayment_Success() {
        String creditorTaxId = "77777777777";
        String noticeCode = "398918182323606420";
        String expectedIuv = creditorTaxId + "##" + noticeCode;
        NotificationCostPaymentResponseDto response = new NotificationCostPaymentResponseDto();

        when(notificationCostService.getNotificationCostPaymentInfo(expectedIuv))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<NotificationCostPaymentResponseDto>> result =
                controller.getNotificationCostByPayment(creditorTaxId, noticeCode, null);

        StepVerifier.create(result)
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertNotNull(responseEntity.getBody());
                    assertEquals(response, responseEntity.getBody());
                })
                .verifyComplete();

        verify(notificationCostService).getNotificationCostPaymentInfo(expectedIuv);
    }

    @Test
    void invalidatePaperCost_shouldInvokeUpdaterForEachCostPhase() {
        PaperCostToInvalidateDto requestDto = new PaperCostToInvalidateDto()
                .recIndex("RECINDEX_3")
                .costPhases(List.of(
                        AnalogUpdateCostPhaseDto.SEND_ANALOG_DOMICILE_ATTEMPT_0,
                        AnalogUpdateCostPhaseDto.SEND_ANALOG_DOMICILE_ATTEMPT_1
                ));
        ArgumentCaptor<NotificationCostUpdate> notificationCostUpdateCaptor = ArgumentCaptor.forClass(NotificationCostUpdate.class);

        when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(TEST_IUN, 3))
                .thenReturn(Mono.just(mock(NotificationDeliveryCost.class)));
        when(notificationCostUpdaterService.updateCostByPhase(any(NotificationCostUpdate.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.invalidatePaperCost(TEST_IUN, Mono.just(requestDto), null))
                .assertNext(response -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode()))
                .verifyComplete();

        verify(notificationDeliveryCostDao, times(2)).getNotificationDeliveryCostItem(TEST_IUN, 3);
        verify(notificationCostUpdaterService, times(2)).updateCostByPhase(notificationCostUpdateCaptor.capture());

        List<NotificationCostUpdate> capturedUpdates = notificationCostUpdateCaptor.getAllValues();
        assertEquals(2, capturedUpdates.size());

        assertInvalidateNotificationCost(capturedUpdates.get(0), CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0);
        assertInvalidateNotificationCost(capturedUpdates.get(1), CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_1);
        verifyNoMoreInteractions(notificationCostService, mapper, paymentInfoMapper, notificationDeliveryCostDao, notificationCostUpdaterService);
    }

    @Test
    void invalidatePaperCost_shouldCompleteWhenCostPhasesIsEmpty() {
        PaperCostToInvalidateDto requestDto = new PaperCostToInvalidateDto()
                .recIndex("RECINDEX_5")
                .costPhases(List.of());

        StepVerifier.create(controller.invalidatePaperCost(TEST_IUN, Mono.just(requestDto), null))
                .assertNext(response -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode()))
                .verifyComplete();

        verifyNoInteractions(notificationDeliveryCostDao, notificationCostUpdaterService, notificationCostService, mapper, paymentInfoMapper);
    }

    @Test
    void invalidatePaperCost_shouldPropagateErrorWhenEntityDoesNotExist() {
        PaperCostToInvalidateDto requestDto = new PaperCostToInvalidateDto()
                .recIndex("RECINDEX_3")
                .costPhases(List.of(AnalogUpdateCostPhaseDto.SEND_ANALOG_DOMICILE_ATTEMPT_0));
        PnNotFoundException expectedException = new PnNotFoundException(
                "Not Found",
                "No item found with iun: " + TEST_IUN + " and recIndex: 3",
                "PN_NOTIFICATIONDELIVERYCOST_NOTFOUND"
        );

        when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(TEST_IUN, 3))
                .thenReturn(Mono.error(expectedException));

        StepVerifier.create(controller.invalidatePaperCost(TEST_IUN, Mono.just(requestDto), null))
                .expectErrorSatisfies(throwable -> {
                    PnNotFoundException ex = assertInstanceOf(PnNotFoundException.class, throwable);
                    assertEquals(HttpStatus.NOT_FOUND.value(), ex.getProblem().getStatus());
                    String detail = ex.getProblem().getDetail();
                    assertNotNull(detail);
                    assertTrue(detail.contains(TEST_IUN));
                    assertTrue(detail.contains("recIndex: 3"));
                })
                .verify();

        verify(notificationDeliveryCostDao).getNotificationDeliveryCostItem(TEST_IUN, 3);
        verify(notificationCostUpdaterService, never()).updateCostByPhase(any(NotificationCostUpdate.class));
        verifyNoMoreInteractions(notificationDeliveryCostDao, notificationCostUpdaterService);
        verifyNoInteractions(notificationCostService, mapper, paymentInfoMapper);
    }

    private void assertInvalidateNotificationCost(NotificationCostUpdate notificationCostUpdate,
                                                  CostUpdatePhaseInt expectedPhase) {
        assertEquals(TEST_IUN, notificationCostUpdate.getIun());
        assertEquals(3, notificationCostUpdate.getRecIndex());
        assertEquals(0, notificationCostUpdate.getCost());
        assertNull(notificationCostUpdate.getProductType());
        assertEquals(expectedPhase, notificationCostUpdate.getCostUpdatePhase());
        assertNotNull(notificationCostUpdate.getElementTimestamp());
        assertTrue(notificationCostUpdate.isInvalidationFlow());
    }
}

