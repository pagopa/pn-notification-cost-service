package it.pagopa.pn.notificationcostservice.middleware.dynamo.notificationdeliverycost.mapper;

import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost.DtoToEntityNotificationDeliveryCostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DtoToEntityNotificationDeliveryCostDaoMapperTest {
    private DtoToEntityNotificationDeliveryCostMapper mapper;

    @BeforeEach
    void before() {
        mapper = new DtoToEntityNotificationDeliveryCostMapper();
    }

    @Test
    void dto2Entity(){
        Instant now = Instant.now();

        FirstAnalogCost firstCost = FirstAnalogCost.builder()
                .cost(50)
                .productType("AR")
                .build();

        SecondAnalogCost secondCost = SecondAnalogCost.builder()
                .cost(30)
                .productType("890")
                .build();

        BaseCost baseCost = BaseCost.builder()
                .paFee(50)
                .sendFee(50)
                .build();

        NotificationDeliveryCost dto = NotificationDeliveryCost.builder()
                .iun("IUN123")
                .recIndex(0)
                .recipientInternalId("recipientId")
                .baseCost(baseCost)
                .firstAnalogCost(firstCost)
                .secondAnalogCost(secondCost)
                .isDeleted(false)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .vat(22)
                .senderPaId("sender")
                .senderTaxId("taxId")
                .lastUpdate(now)
                .ttl(3600L)
                .build();

        NotificationDeliveryCostEntity entity = mapper.dto2Entity(dto);

        assertEquals(dto.getIun(), entity.getIun());
        assertEquals(dto.getRecIndex(), entity.getRecIndex());
        assertEquals(dto.getRecipientInternalId(), entity.getRecipientInternalId());
        assertEquals(dto.getSenderPaId(), entity.getSenderPaId());

        // Verifica BaseCost
        assertEquals(dto.getBaseCost().getSendFee(), entity.getBaseCost().getSendFee());
        assertEquals(dto.getBaseCost().getPaFee(), entity.getBaseCost().getPaFee());

        // Verifica FirstAnalogCost
        assertEquals(dto.getFirstAnalogCost().getCost(), entity.getFirstAnalogCost().getCost());
        assertEquals(dto.getFirstAnalogCost().getProductType(), entity.getFirstAnalogCost().getProductType());

        // Verifica SecondAnalogCost
        assertEquals(dto.getSecondAnalogCost().getCost(), entity.getSecondAnalogCost().getCost());
        assertEquals(dto.getSecondAnalogCost().getProductType(), entity.getSecondAnalogCost().getProductType());

        // Verifica SimpleRegisteredLetterCost
        assertNull(entity.getSimpleRegisteredLetterCost());

        assertEquals(dto.getIsDeleted(), entity.getIsDeleted());
        assertEquals(dto.getNotificationFeePolicy(), entity.getNotificationFeePolicy());
        assertEquals(dto.getPagoPaIntMode(), entity.getPagoPaIntMode());
        assertEquals(dto.getVat(), entity.getVat());
        assertEquals(dto.getLastUpdate(), entity.getLastUpdate());
        assertEquals(dto.getTtl(), entity.getTtl());
    }

    @Test
    void dto2EntityWithNullAnalogCosts(){
        Instant now = Instant.now();

        BaseCost baseCost = BaseCost.builder()
                .paFee(50)
                .sendFee(50)
                .build();

        NotificationDeliveryCost dto = NotificationDeliveryCost.builder()
                .iun("IUN123")
                .recIndex(0)
                .recipientInternalId("recipientId")
                .baseCost(baseCost)
                .firstAnalogCost(null)
                .secondAnalogCost(null)
                .simpleRegisteredLetterCost(null)
                .isDeleted(false)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .vat(22)
                .senderPaId("sender")
                .senderTaxId("taxId")
                .lastUpdate(now)
                .ttl(3600L)
                .build();

        NotificationDeliveryCostEntity entity = mapper.dto2Entity(dto);

        assertEquals(dto.getIun(), entity.getIun());
        assertEquals(dto.getRecIndex(), entity.getRecIndex());
        assertNull(entity.getFirstAnalogCost());
        assertNull(entity.getSecondAnalogCost());
        assertNull(entity.getSimpleRegisteredLetterCost());
    }

}
