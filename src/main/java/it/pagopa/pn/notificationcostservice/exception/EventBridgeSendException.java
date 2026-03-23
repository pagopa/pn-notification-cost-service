package it.pagopa.pn.notificationcostservice.exception;

public class EventBridgeSendException extends RuntimeException {
    public EventBridgeSendException(String message) {
        super(message);
    }
}
