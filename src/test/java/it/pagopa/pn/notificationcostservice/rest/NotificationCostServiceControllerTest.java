package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponse;
import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.mapper.NotificationCostRecipientMapper;
import it.pagopa.pn.notificationcostservice.service.PaymentCostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCostServiceControllerTest {

    @Mock
    private PaymentCostService paymentCostService;

    @Mock
    private NotificationCostRecipientMapper mapper;

    @InjectMocks
    private NotificationCostServiceController controller;

    private static final String TEST_IUN = "TEST-IUN-123";
    private static final Integer TEST_REC_INDEX = 0;

    @Test
    void testNotificationCostRecipient_Success() {
        NotificationCostRecipientResponseDto dto = createTestDto();
        NotificationCostRecipientResponse response = createTestResponse();

        when(paymentCostService.getNotificationCostRecipient(TEST_IUN, TEST_REC_INDEX))
                .thenReturn(Mono.just(dto));
        when(mapper.dtoResponse2Response(dto)).thenReturn(response);

        Mono<ResponseEntity<NotificationCostRecipientResponse>> result =
                controller.notificationCostRecipient(TEST_IUN, TEST_REC_INDEX, null);

        StepVerifier.create(result)
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertNotNull(responseEntity.getBody());
                    assertEquals(response, responseEntity.getBody());
                })
                .verifyComplete();

        verify(paymentCostService).getNotificationCostRecipient(TEST_IUN, TEST_REC_INDEX);
        verify(mapper).dtoResponse2Response(dto);
    }

    @Test
    void testNotificationCostRecipient_ServiceError() {
        RuntimeException exception = new RuntimeException("Service error");

        when(paymentCostService.getNotificationCostRecipient(TEST_IUN, TEST_REC_INDEX))
                .thenReturn(Mono.error(exception));

        Mono<ResponseEntity<NotificationCostRecipientResponse>> result =
                controller.notificationCostRecipient(TEST_IUN, TEST_REC_INDEX, null);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(paymentCostService).getNotificationCostRecipient(TEST_IUN, TEST_REC_INDEX);
        verify(mapper, never()).dtoResponse2Response(any());
    }

    @Test
    void testNotificationCostRecipient_EmptyResponse() {
        when(paymentCostService.getNotificationCostRecipient(TEST_IUN, TEST_REC_INDEX))
                .thenReturn(Mono.empty());

        Mono<ResponseEntity<NotificationCostRecipientResponse>> result =
                controller.notificationCostRecipient(TEST_IUN, TEST_REC_INDEX, null);

        StepVerifier.create(result)
                .verifyComplete();

        verify(paymentCostService).getNotificationCostRecipient(TEST_IUN, TEST_REC_INDEX);
        verify(mapper, never()).dtoResponse2Response(any());
    }

    @Test
    void testNotificationCostRecipient_WithDifferentRecIndex() {
        Integer recIndex = 1;
        NotificationCostRecipientResponseDto dto = createTestDto();
        NotificationCostRecipientResponse response = createTestResponse();

        when(paymentCostService.getNotificationCostRecipient(TEST_IUN, recIndex))
                .thenReturn(Mono.just(dto));
        when(mapper.dtoResponse2Response(dto)).thenReturn(response);

        Mono<ResponseEntity<NotificationCostRecipientResponse>> result =
                controller.notificationCostRecipient(TEST_IUN, recIndex, null);

        StepVerifier.create(result)
                .assertNext(responseEntity -> assertEquals(HttpStatus.OK, responseEntity.getStatusCode()))
                .verifyComplete();

        verify(paymentCostService).getNotificationCostRecipient(TEST_IUN, recIndex);
    }

    private NotificationCostRecipientResponseDto createTestDto() {
        NotificationCostRecipientResponseDto dto = new NotificationCostRecipientResponseDto();
        TotalCostDto totalCost = new TotalCostDto();
        totalCost.setCost(1000);
        dto.setTotalCost(totalCost);
        dto.setPagoPaIntMode(PagoPaIntMode.SYNC);
        return dto;
    }

    private NotificationCostRecipientResponse createTestResponse() {
        return new NotificationCostRecipientResponse();
    }
}

