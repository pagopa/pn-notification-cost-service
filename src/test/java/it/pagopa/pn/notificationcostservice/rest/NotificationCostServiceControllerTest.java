package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NewNotificationCostRequestDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostPaymentResponseDto;
import it.pagopa.pn.notificationcostservice.model.ValidationStatus;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import it.pagopa.pn.notificationcostservice.service.NotificationCostService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import it.pagopa.pn.notificationcostservice.service.mapper.PaymentInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class NotificationCostServiceControllerTest {

    @Mock
    private NotificationCostService notificationCostService;

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
}

