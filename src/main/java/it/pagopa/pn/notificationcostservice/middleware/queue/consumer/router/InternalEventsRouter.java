package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.router;

import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEvent;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;


public interface InternalEventsRouter {
    Mono<Void> handleEvent(Message<InternalEvent> event);
}
