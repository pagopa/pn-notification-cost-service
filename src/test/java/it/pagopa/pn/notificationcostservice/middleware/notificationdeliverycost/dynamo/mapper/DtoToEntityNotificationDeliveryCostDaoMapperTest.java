package it.pagopa.pn.notificationcostservice.middleware.notificationdeliverycost.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.mapper.DtoToEntityNotificationDeliveryCostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DtoToEntityNotificationDeliveryCostDaoMapperTest {
    private DtoToEntityNotificationDeliveryCostMapper mapper;

    @BeforeEach
    void before() {
        mapper = new DtoToEntityNotificationDeliveryCostMapper();
    }

    @Test
    void dto2Entity(){
        Instant now = Instant.now();
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("IUN123")
                .recIndex(0)
                .recipientInternalId("recipientId")
                .baseCost(100)
                .firstAnalogCost(10)
                .secondAnalogCost(20)
                .simpleRegisteredLetterCost(30)
                .isRefused(true)
                .isCancelled(false)
                .refinementDate(now)
                .notificationViewDate(now)
                .sendFee(5)
                .paFee(2)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .vat(22)
                .lastUpdate(now)
                .ttl(3600L)
                .build();

        NotificationDeliveryCostEntity entity = mapper.dto2Entity(dto);

        assertEquals(entity.getPk(), dto.getIun());
        assertEquals(entity.getSk(), dto.getRecIndex());
        assertEquals(entity.getRecipientInternalId(), dto.getRecipientInternalId());
        assertEquals(entity.getBaseCost(), dto.getBaseCost());
        assertEquals(entity.getFirstAnalogCost(), dto.getFirstAnalogCost());
        assertEquals(entity.getSecondAnalogCost(), dto.getSecondAnalogCost());
        assertEquals(entity.getSimpleRegisteredLetterCost(), dto.getSimpleRegisteredLetterCost());
        assertEquals(entity.getIsRefused(), dto.getIsRefused());
        assertEquals(entity.getIsCancelled(), dto.getIsCancelled());
        assertEquals(entity.getRefinementDate(), dto.getRefinementDate());
        assertEquals(entity.getNotificationViewDate(), dto.getNotificationViewDate());
        assertEquals(entity.getSendFee(), dto.getSendFee());
        assertEquals(entity.getPaFee(), dto.getPaFee());
        assertEquals(entity.getNotificationFeePolicy(), dto.getNotificationFeePolicy());
        assertEquals(entity.getPagoPaIntMode(), dto.getPagoPaIntMode());
        assertEquals(entity.getVat(), dto.getVat());
        assertEquals(entity.getLastUpdate(), dto.getLastUpdate());
        assertEquals(entity.getTtl(), dto.getTtl());
    }

}
