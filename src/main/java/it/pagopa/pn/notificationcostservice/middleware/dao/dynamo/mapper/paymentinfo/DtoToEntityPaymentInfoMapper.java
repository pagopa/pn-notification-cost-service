package it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.paymentinfo;

import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DtoToEntityPaymentInfoMapper {
    public PaymentInfoEntity dtoToEntity(PaymentInfo dto) {
        if (Objects.isNull(dto)) {
            return null;
        }
        PaymentInfoEntity entity = new PaymentInfoEntity();
        entity.setIun(dto.getIun());
        entity.setRecIndex(dto.getRecIndex());
        entity.setIuv(dto.getIuv());
        entity.setApplyCost(dto.isApplyCost());
        return entity;
    }
}
