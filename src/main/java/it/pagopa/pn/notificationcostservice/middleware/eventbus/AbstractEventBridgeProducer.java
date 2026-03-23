package it.pagopa.pn.notificationcostservice.middleware.eventbus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.GenericEventBridgeEvent;
import it.pagopa.pn.notificationcostservice.exception.EventBridgeSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.List;

@Slf4j
@Component
public abstract class AbstractEventBridgeProducer<T extends GenericEventBridgeEvent> implements EventBridgeProducer<T> {

    private final EventBridgeAsyncClient amazonEventBridge;
    private final String eventBusName;
    private final String eventBusDetailType;
    private final String eventBusSource;
    private final ObjectMapper objectMapper;

    protected AbstractEventBridgeProducer(
            EventBridgeAsyncClient amazonEventBridge,
            String eventBusSource,
            String detailType,
            String name,
            ObjectMapper objectMapper
    ) {
        this.amazonEventBridge = amazonEventBridge;
        this.eventBusSource = eventBusSource;
        this.eventBusName = name;
        this.eventBusDetailType = detailType;
        this.objectMapper = objectMapper;
    }

    private PutEventsRequest putEventsRequestBuilder(List<T> events) {
        PutEventsRequest putEventsRequest = PutEventsRequest.builder()
                .entries(events.stream()
                        .map(this::buildEventRequest)
                        .toList()
                )
                .build();

        log.debug("PutEventsRequest: {}", putEventsRequest);
        return putEventsRequest;
    }

    private PutEventsRequestEntry buildEventRequest(T event) {
        return PutEventsRequestEntry.builder()
                .eventBusName(eventBusName)
                .detailType(eventBusDetailType)
                .source(eventBusSource)
                .detail(serializeDetail(event))
                .build();
    }

    private String serializeDetail(T event) {
        try {
            return objectMapper.writeValueAsString(event.getDetail());
        } catch (JsonProcessingException e) {
            throw new EventBridgeSendException(
                    String.format("Error serializing event detail for event bus: %s", eventBusName)
            );
        }
    }

    @Override
    public Mono<Void> sendEvent(T event) {
        return sendEvent(List.of(event));
    }

    @Override
    public Mono<Void> sendEvent(List<T> events) {
        return Mono.fromFuture(amazonEventBridge.putEvents(putEventsRequestBuilder(events)))
                .doOnError(throwable -> log.error("Error sending event on event bridge", throwable))
                .flatMap(response -> {
                    if (response.failedEntryCount() != null && response.failedEntryCount() > 0) {
                        response.entries().forEach(entry ->
                                log.error("EventBridge failed entry: errorCode={}, errorMessage={}",
                                        entry.errorCode(), entry.errorMessage())
                        );
                        log.error("error sending event on event bus={} response={}", eventBusName, response);
                        return Mono.error(new EventBridgeSendException(
                                String.format("Error sending event on event bus: %s", eventBusName)
                        ));
                    }
                    log.debug("Event sent successfully: {}", response.entries());
                    return Mono.empty();
                });
    }
}