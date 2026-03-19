package it.pagopa.pn.notificationcostservice.middleware.dao.paymentinfo.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.middleware.dao.paymentinfo.dynamo.entity.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EntityToDtoPaymentInfoMapper {

    public PaymentInfo entityToDto(PaymentInfoEntity entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return PaymentInfo.builder()
                .iun(entity.getIun())
                .recIndex(entity.getRecIndex())
                .iuv(entity.getIuv())
                .applyCost(entity.isApplyCost())
                .build();
    }
}