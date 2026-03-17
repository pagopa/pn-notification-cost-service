package it.pagopa.pn.notificationcostservice.rest;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRequestDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.RequestAcceptedDto;
import it.pagopa.pn.notificationcostservice.model.ValidationStatus;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.NotificationCostRequest;
import it.pagopa.pn.notificationcostservice.service.NotificationCostService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationCostRequestMapper;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class NotificationCostServiceControllerTest {

    @Mock
    private NotificationCostService notificationCostService;
    @Mock
    private NotificationCostRequestMapper notificationCostRequestMapper;
    @InjectMocks
    private NotificationCostServiceController controller;

    private static final String TEST_IUN = "TEST-IUN-123";
    private static final Integer TEST_REC_INDEX = 0;

    @Test
    void testNotificationCostRecipient_Success() {
        NotificationCostRecipientResponseDto response = new NotificationCostRecipientResponseDto();

        when(notificationCostService.getNotificationCostRecipient(TEST_IUN, TEST_REC_INDEX))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<NotificationCostRecipientResponseDto>> result =
                controller.notificationCostRecipient(TEST_IUN, TEST_REC_INDEX, null);

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
        // GIVEN
        String iun = "TEST-IUN-123";
        NotificationCostRequestDto requestDto = new NotificationCostRequestDto();
        NotificationCostRequest request = NotificationCostRequest.builder().build();
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(notificationCostRequestMapper.fromDto(any(NotificationCostRequestDto.class)))
                .thenReturn(request);
        when(notificationCostService.saveNotificationCost(eq(iun), eq(request)))
                .thenReturn(Mono.empty());
        Mono<ResponseEntity<RequestAcceptedDto>> result = controller.initializeNotificationCost(iun, Mono.just(requestDto), exchange);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(ValidationStatus.OK.name(), response.getBody().getStatus());
                })
                .verifyComplete();
        verify(notificationCostService).saveNotificationCost(iun, request);
    }
    @Test
    void initializeNotificationCostPropagatesServiceError() {
        NotificationCostRequestDto requestDto = new NotificationCostRequestDto();
        NotificationCostRequest request = NotificationCostRequest.builder().build();
        IllegalStateException expected = new IllegalStateException("enqueue failed");
        when(notificationCostRequestMapper.fromDto(requestDto)).thenReturn(request);
        when(notificationCostService.saveNotificationCost(TEST_IUN, request)).thenReturn(Mono.error(expected));
        StepVerifier.create(controller.initializeNotificationCost(TEST_IUN, Mono.just(requestDto), null))
                .expectErrorMatches(throwable -> throwable == expected)
                .verify();
        verify(notificationCostRequestMapper).fromDto(same(requestDto));
        verify(notificationCostService).saveNotificationCost(TEST_IUN, request);
    }
}
