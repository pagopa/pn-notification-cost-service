package it.pagopa.pn.notificationcostservice.rest;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

class PaymentInfoControllerTest {

    private PaymentInfoController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentInfoController();
    }

    @Test
    void initializeNotificationCostTest() {
        String iun = "iun";
        NotificationCostRequestDto requestDto = new NotificationCostRequestDto();
        ServerWebExchange exchange = mock(ServerWebExchange.class);

        Mono<ResponseEntity<String>> result = controller.initializeNotificationCost(iun, Mono.just(requestDto), exchange);

        StepVerifier.create(result)
                .expectNext(ResponseEntity.ok("ok"))
                .verifyComplete();
    }
}

