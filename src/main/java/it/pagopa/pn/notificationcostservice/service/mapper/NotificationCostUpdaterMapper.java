package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostEntity;
import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class NotificationCostUpdaterMapper {
    public NotificationDeliveryCostEntity toEntityForBaseCostUpdate(
            NotificationDeliveryCost notificationDeliveryCost
    ) {
        return NotificationDeliveryCostEntity.builder()
                .iun(notificationDeliveryCost.getIun())
                .recIndex(notificationDeliveryCost.getRecIndex())
                .baseCost(BaseCostEntity.builder()
                        .sendFee(notificationDeliveryCost.getBaseCost().getSendFee())
                        .paFee(notificationDeliveryCost.getBaseCost().getPaFee())
                        .build())
                .vat(notificationDeliveryCost.getVat())
                .notificationFeePolicy(notificationDeliveryCost.getNotificationFeePolicy())
                .pagoPaIntMode(notificationDeliveryCost.getPagoPaIntMode())
                .senderPaId(notificationDeliveryCost.getSenderPaId())
                .senderTaxId(notificationDeliveryCost.getSenderTaxId())
                .recipientInternalId(notificationDeliveryCost.getRecipientInternalId())
                .build();
    }

    public NotificationDeliveryCostEntity mapNotificationCostUpdater(
            NotificationCostUpdate notificationDeliveryCost
    ) {
        return switch (notificationDeliveryCost.getCostUpdatePhase()) {
            case SEND_SIMPLE_REGISTERED_LETTER -> {
                log.info("Mapping notification delivery cost for phase: SEND_SIMPLE_REGISTERED_LETTER");
                yield buildSimpleRegisteredLetter(notificationDeliveryCost);
            }
            case SEND_ANALOG_DOMICILE_ATTEMPT_0 -> {
                log.info("Mapping notification delivery cost for phase: SEND_ANALOG_DOMICILE_ATTEMPT_0");
                yield buildSendAnalogDomicileAttemptZero(notificationDeliveryCost);
            }
            case SEND_ANALOG_DOMICILE_ATTEMPT_1 -> {
                log.info("Mapping notification delivery cost for phase: SEND_ANALOG_DOMICILE_ATTEMPT_1");
                yield buildSendAnalogDomicileAttemptOne(notificationDeliveryCost);
            }
            case REQUEST_REFUSED -> {
                log.info("Mapping notification delivery cost for phase: REQUEST_REFUSED");
                yield buildRefusedAndCancelled(notificationDeliveryCost);
            }
            case NOTIFICATION_CANCELLED -> {
                log.info("Mapping notification delivery cost for phase: NOTIFICATION_CANCELLED");
                yield buildRefusedAndCancelled(notificationDeliveryCost);
            }
            default -> {
                log.warn("Received unknown update cost phase: {}", notificationDeliveryCost.getCostUpdatePhase());
                throw new IllegalArgumentException("Unknown cost update phase: " + notificationDeliveryCost.getCostUpdatePhase());
            }
        };
    }

    private NotificationDeliveryCostEntity buildSendAnalogDomicileAttemptOne(NotificationCostUpdate dto) {
        NotificationDeliveryCostEntity entity = new NotificationDeliveryCostEntity();
        entity.setIun(dto.getIun());
        entity.setRecIndex(dto.getRecIndex());

        FirstAnalogCostEntity firstAnalogCostEntity = new FirstAnalogCostEntity();
        firstAnalogCostEntity.setCost(dto.getCost());
        firstAnalogCostEntity.setProductType(dto.getProductType());
        entity.setFirstAnalogCost(firstAnalogCostEntity);

        return entity;
    }

    private NotificationDeliveryCostEntity buildSendAnalogDomicileAttemptZero(NotificationCostUpdate dto) {
        NotificationDeliveryCostEntity entity = new NotificationDeliveryCostEntity();
        entity.setIun(dto.getIun());
        entity.setRecIndex(dto.getRecIndex());
        SecondAnalogCostEntity secondAnalogCostEntity = new SecondAnalogCostEntity();
        secondAnalogCostEntity.setCost(dto.getCost());
        secondAnalogCostEntity.setProductType(dto.getProductType());
        entity.setSecondAnalogCost(secondAnalogCostEntity);
        return entity;
    }

    private NotificationDeliveryCostEntity buildSimpleRegisteredLetter(NotificationCostUpdate dto) {
        NotificationDeliveryCostEntity entity = new NotificationDeliveryCostEntity();
        entity.setIun(dto.getIun());
        entity.setRecIndex(dto.getRecIndex());
        SimpleRegisteredLetterCostEntity simpleRegisteredLetterCostEntity = new SimpleRegisteredLetterCostEntity();
        simpleRegisteredLetterCostEntity.setCost(dto.getCost());
        simpleRegisteredLetterCostEntity.setProductType(dto.getProductType());
        entity.setSimpleRegisteredLetterCost(simpleRegisteredLetterCostEntity);
        return entity;
    }

    private NotificationDeliveryCostEntity buildRefusedAndCancelled(NotificationCostUpdate dto) {
        NotificationDeliveryCostEntity entity = new NotificationDeliveryCostEntity();
        entity.setIun(dto.getIun());
        entity.setRecIndex(dto.getRecIndex());

        FirstAnalogCostEntity firstAnalogCostEntity = new FirstAnalogCostEntity();
        firstAnalogCostEntity.setCost(0);
        firstAnalogCostEntity.setProductType(null);
        entity.setFirstAnalogCost(firstAnalogCostEntity);

        SecondAnalogCostEntity secondAnalogCostEntity = new SecondAnalogCostEntity();
        secondAnalogCostEntity.setCost(0);
        secondAnalogCostEntity.setProductType(null);
        entity.setSecondAnalogCost(secondAnalogCostEntity);

        entity.setIsDeleted(true);
        return entity;
    }
}
