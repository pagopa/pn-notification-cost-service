package it.pagopa.pn.notificationcostservice.config.springbootcfg;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.api.dto.events.notificationcost.utils.ValidationStatus;
import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEvent;
import it.pagopa.pn.api.dto.events.notificationcost.validation.PnNotificationCostValidationEventPayload;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogCostComponentDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogCostDetailDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogCostNameDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.BaseCostComponentDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.BaseCostDetailDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.BaseCostNameDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationFeePolicyDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.PagoPaIntModeDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.TotalCostDetailsDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.TotalCostDto;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.InternalEventType;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.NotificationCostInitializationEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.consumer.event.notificationcost.UpdateNotificationCostEvent;
import it.pagopa.pn.notificationcostservice.middleware.queue.producer.sqs.InitializeCostProducer;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.AnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@RegisterReflectionForBinding({
        // internal queue payloads
        InternalEvent.class,
        InternalEventType.class,
        NotificationCostInitializationEvent.class,
        NotificationCostInitializationEvent.Payload.class,
        UpdateNotificationCostEvent.class,
        UpdateNotificationCostEvent.Payload.class,

        // domain models used in SQS payloads
        NotificationDeliveryCost.class,
        BaseCost.class,
        NotificationFeePolicy.class,
        PagoPaIntMode.class,
        AnalogCost.class,
        FirstAnalogCost.class,
        SecondAnalogCost.class,
        SimpleRegisteredLetterCost.class,
        PaymentInfo.class,

        // external pn-api event models used for EventBridge serialization
        GenericEvent.class,
        GenericEventHeader.class,
        EventPublisher.class,
        PnNotificationCostValidationEvent.class,
        PnNotificationCostValidationEvent.Detail.class,
        PnNotificationCostValidationEventPayload.class,
        ValidationStatus.class,

        // OpenAPI response DTOs serialized by REST API
        NotificationCostRecipientResponseDto.class,
        TotalCostDto.class,
        TotalCostDetailsDto.class,
        BaseCostDetailDto.class,
        BaseCostComponentDto.class,
        BaseCostNameDto.class,
        AnalogCostDetailDto.class,
        AnalogCostComponentDto.class,
        AnalogCostNameDto.class,
        NotificationFeePolicyDto.class,
        PagoPaIntModeDto.class
})
@RegisterReflection(classes = {
        InitializeCostProducer.class,
        NotificationCostInitializationEvent.class,
        NotificationCostInitializationEvent.Payload.class,
        UpdateNotificationCostEvent.class,
        UpdateNotificationCostEvent.Payload.class,
        PnNotificationCostValidationEvent.class,
        PnNotificationCostValidationEvent.Detail.class,
        PnNotificationCostValidationEventPayload.class,
        net.logstash.logback.encoder.LogstashEncoder.class,
        net.logstash.logback.stacktrace.ShortenedThrowableConverter.class,
        net.logstash.logback.argument.StructuredArgument.class
})
public class NativeBindingClassConfig {
}
