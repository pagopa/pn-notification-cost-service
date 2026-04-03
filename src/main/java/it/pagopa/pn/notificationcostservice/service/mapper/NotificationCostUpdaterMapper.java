package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import org.springframework.stereotype.Component;

@Component
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
}
