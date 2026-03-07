package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.service.NotificationCostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCostServiceControllerTest {

    @Mock
    private NotificationCostService notificationCostService;

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
}

