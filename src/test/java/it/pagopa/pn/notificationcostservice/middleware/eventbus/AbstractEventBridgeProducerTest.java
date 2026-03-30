package it.pagopa.pn.notificationcostservice.middleware.eventbus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.GenericEventBridgeEvent;
import it.pagopa.pn.notificationcostservice.exception.EventBridgeSendException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractEventBridgeProducerTest {

    private static final String EVENT_BUS_NAME = "test-bus";
    private static final String EVENT_SOURCE = "test.source";
    private static final String DETAIL_TYPE = "TestDetailType";

    @Mock
    private EventBridgeAsyncClient amazonEventBridge;

    @Mock
    private GenericEventBridgeEvent event1;

    @Mock
    private GenericEventBridgeEvent event2;

    @Test
    void sendEvent_singleEvent_shouldBuildRequestAndComplete() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TestProducer producer = new TestProducer(amazonEventBridge, objectMapper);

        LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
        detail.put("iun", "IUN123");
        detail.put("status", "OK");

        when(event1.getDetail()).thenReturn(detail);
        when(amazonEventBridge.putEvents(any(PutEventsRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        PutEventsResponse.builder()
                                .failedEntryCount(0)
                                .entries(List.of(PutEventsResultEntry.builder().build()))
                                .build()
                ));

        StepVerifier.create(producer.sendEvent(event1))
                .verifyComplete();

        ArgumentCaptor<PutEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutEventsRequest.class);
        verify(amazonEventBridge).putEvents(requestCaptor.capture());

        PutEventsRequest request = requestCaptor.getValue();
        assertNotNull(request);
        assertEquals(1, request.entries().size());

        var entry = request.entries().getFirst();
        assertEquals(EVENT_BUS_NAME, entry.eventBusName());
        assertEquals(EVENT_SOURCE, entry.source());
        assertEquals(DETAIL_TYPE, entry.detailType());

        assertEquals(
                objectMapper.readTree(objectMapper.writeValueAsString(detail)),
                objectMapper.readTree(entry.detail())
        );
    }

    @Test
    void sendEvent_multipleEvents_shouldBuildAllEntries() {
        ObjectMapper objectMapper = new ObjectMapper();
        TestProducer producer = new TestProducer(amazonEventBridge, objectMapper);

        when(event1.getDetail()).thenReturn(new LinkedHashMap<>(java.util.Map.of("id", "1")));
        when(event2.getDetail()).thenReturn(new LinkedHashMap<>(java.util.Map.of("id", "2")));

        when(amazonEventBridge.putEvents(any(PutEventsRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        PutEventsResponse.builder()
                                .failedEntryCount(0)
                                .entries(List.of(
                                        PutEventsResultEntry.builder().build(),
                                        PutEventsResultEntry.builder().build()
                                ))
                                .build()
                ));

        StepVerifier.create(producer.sendEvent(List.of(event1, event2)))
                .verifyComplete();

        ArgumentCaptor<PutEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutEventsRequest.class);
        verify(amazonEventBridge).putEvents(requestCaptor.capture());

        PutEventsRequest request = requestCaptor.getValue();
        assertNotNull(request);
        assertEquals(2, request.entries().size());

        request.entries().forEach(entry -> {
            assertEquals(EVENT_BUS_NAME, entry.eventBusName());
            assertEquals(EVENT_SOURCE, entry.source());
            assertEquals(DETAIL_TYPE, entry.detailType());
            assertNotNull(entry.detail());
        });
    }

    @Test
    void sendEvent_shouldReturnErrorWhenEventBridgeReportsFailedEntries() {
        ObjectMapper objectMapper = new ObjectMapper();
        TestProducer producer = new TestProducer(amazonEventBridge, objectMapper);

        when(event1.getDetail()).thenReturn(new LinkedHashMap<>(java.util.Map.of("id", "1")));

        when(amazonEventBridge.putEvents(any(PutEventsRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        PutEventsResponse.builder()
                                .failedEntryCount(1)
                                .entries(List.of(
                                        PutEventsResultEntry.builder()
                                                .errorCode("InternalFailure")
                                                .errorMessage("boom")
                                                .build()
                                ))
                                .build()
                ));

        StepVerifier.create(producer.sendEvent(event1))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(EventBridgeSendException.class, error);
                    assertEquals(
                            "Error sending event on event bus: " + EVENT_BUS_NAME,
                            error.getMessage()
                    );
                })
                .verify();
    }

    @Test
    void sendEvent_shouldPropagateErrorWhenAwsClientFutureFails() {
        ObjectMapper objectMapper = new ObjectMapper();
        TestProducer producer = new TestProducer(amazonEventBridge, objectMapper);

        when(event1.getDetail()).thenReturn(new LinkedHashMap<>(java.util.Map.of("id", "1")));

        CompletableFuture<PutEventsResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("aws error"));

        when(amazonEventBridge.putEvents(any(PutEventsRequest.class)))
                .thenReturn(failedFuture);

        StepVerifier.create(producer.sendEvent(event1))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                "aws error".equals(error.getMessage()))
                .verify();
    }

    @Test
    void sendEvent_shouldThrowSynchronouslyWhenSerializationFails() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        TestProducer producer = new TestProducer(amazonEventBridge, objectMapper);

        when(event1.getDetail()).thenReturn(new Object());
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialization error") {});

        EventBridgeSendException exception = assertThrows(
                EventBridgeSendException.class,
                () -> producer.sendEvent(event1)
        );

        assertEquals(
                "Error serializing event detail for event bus: " + EVENT_BUS_NAME,
                exception.getMessage()
        );

        verifyNoInteractions(amazonEventBridge);
    }

    private static class TestProducer extends AbstractEventBridgeProducer<GenericEventBridgeEvent> {
        protected TestProducer(EventBridgeAsyncClient amazonEventBridge, ObjectMapper objectMapper) {
            super(amazonEventBridge, EVENT_SOURCE, DETAIL_TYPE, EVENT_BUS_NAME, objectMapper);
        }
    }
}
