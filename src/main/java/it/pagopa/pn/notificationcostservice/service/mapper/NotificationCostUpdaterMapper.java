package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import org.springframework.stereotype.Component;

@Component
public class NotificationCostUpdaterMapper {
    public NotificationDeliveryCostEntity mapNotificationCostUpdater(
            CostUpdatePhaseInt updateCostPhase,
            NotificationDeliveryCost notificationDeliveryCost
    ) {
        switch (updateCostPhase) {
            // verranno aggiunti altri stati
            case VALIDATION:
                return buildEntityToUpdatePhaseValidation(notificationDeliveryCost);
            default:
                throw new IllegalArgumentException("Invalid updateCostPhase: " + updateCostPhase);
        }
    }

    private NotificationDeliveryCostEntity buildEntityToUpdatePhaseValidation(NotificationDeliveryCost notificationDeliveryCost) {
        NotificationDeliveryCostEntity entity = new NotificationDeliveryCostEntity();
        entity.setIun(notificationDeliveryCost.getIun());
        entity.setRecIndex(notificationDeliveryCost.getRecIndex());

        BaseCostEntity baseCostEntity = new BaseCostEntity();
        baseCostEntity.setSendFee(notificationDeliveryCost.getBaseCost().getSendFee());
        baseCostEntity.setPaFee(notificationDeliveryCost.getBaseCost().getPaFee());

        entity.setBaseCost(baseCostEntity);
        entity.setVat(notificationDeliveryCost.getVat());
        entity.setNotificationFeePolicy(notificationDeliveryCost.getNotificationFeePolicy());
        entity.setPagoPaIntMode(notificationDeliveryCost.getPagoPaIntMode());
        entity.setSenderInternalId(notificationDeliveryCost.getSenderInternalId());
        entity.setRecipientInternalId(notificationDeliveryCost.getRecipientInternalId());

        // non voglio aggiornarli
        entity.setFirstAnalogCost(null);
        entity.setSecondAnalogCost(null);
        entity.setSimpleRegisteredLetterCost(null);
        entity.setIsDeleted(null);
        return entity;
    }
}
