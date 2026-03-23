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

        NotificationDeliveryCostEntity entity = buildEntityToUpdate(notificationDeliveryCost);

        switch (updateCostPhase) {
            // verranno aggiunti altri stati
            case VALIDATION:
                return entity;
            default:
                throw new IllegalArgumentException("Invalid updateCostPhase: " + updateCostPhase);
        }
    }

    private NotificationDeliveryCostEntity buildEntityToUpdate(NotificationDeliveryCost notificationDeliveryCost) {
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

        // non voglio aggiornarli
        entity.setFirstAnalogCost(null);
        entity.setSecondAnalogCost(null);
        entity.setSimpleRegisteredLetterCost(null);
        entity.setIsDeleted(null);
        return entity;
    }
}
