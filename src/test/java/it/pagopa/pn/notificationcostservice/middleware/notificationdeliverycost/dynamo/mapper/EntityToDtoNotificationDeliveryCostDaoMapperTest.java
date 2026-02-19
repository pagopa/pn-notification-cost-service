package it.pagopa.pn.notificationcostservice.middleware.notificationdeliverycost.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.mapper.EntityToDtoNotificationDeliveryCostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EntityToDtoNotificationDeliveryCostDaoMapperTest {
    private  EntityToDtoNotificationDeliveryCostMapper mapper;

    @BeforeEach
    void before() {
        mapper = new EntityToDtoNotificationDeliveryCostMapper();
    }

    @Test
    void entity2Dto() {
        Instant now = Instant.now();

        FirstAnalogCost firstCost = FirstAnalogCost.builder()
                .cost(50)
                .productType("AR")
                .build();

        SecondAnalogCost secondCost = SecondAnalogCost.builder()
                .cost(30)
                .productType("890")
                .build();

        SimpleRegisteredLetterCost simpleCost = SimpleRegisteredLetterCost.builder()
                .cost(20)
                .productType("RS")
                .build();

        BaseCost baseCost = BaseCost.builder()
                .paFee(50)
                .sendFee(50)
                .build();

        NotificationDeliveryCostEntity entity = NotificationDeliveryCostEntity.builder()
                .iun("IUN123")
                .recIndex(0)
                .recipientInternalId("recipientId")
                .baseCost(baseCost)
                .senderInternalId("sender")
                .firstAnalogCost(firstCost)
                .secondAnalogCost(secondCost)
                .simpleRegisteredLetterCost(simpleCost)
                .isDeleted(false)
                .sendFee(50)
                .paFee(50)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .vat(22)
                .lastUpdate(now)
                .ttl(3600L)
                .build();

        NotificationDeliveryCostDto dto = mapper.entity2Dto(entity);

        assertEquals(entity.getIun(), dto.getIun());
        assertEquals(entity.getRecIndex(), dto.getRecIndex());
        assertEquals(entity.getRecipientInternalId(), dto.getRecipientInternalId());
        assertEquals(entity.getSenderInternalId(), dto.getSenderInternalId());

        // Verifica BaseCost
        assertEquals(entity.getBaseCost().getSendFee(), dto.getBaseCost().getSendFee());
        assertEquals(entity.getBaseCost().getPaFee(), dto.getBaseCost().getPaFee());

        // Verifica FirstAnalogCost
        assertNotNull(dto.getFirstAnalogCost());
        assertEquals(entity.getFirstAnalogCost().getCost(), dto.getFirstAnalogCost().getCost());
        assertEquals(entity.getFirstAnalogCost().getProductType(), dto.getFirstAnalogCost().getProductType());

        // Verifica SecondAnalogCost
        assertNotNull(dto.getSecondAnalogCost());
        assertEquals(entity.getSecondAnalogCost().getCost(), dto.getSecondAnalogCost().getCost());
        assertEquals(entity.getSecondAnalogCost().getProductType(), dto.getSecondAnalogCost().getProductType());

        // Verifica SimpleRegisteredLetterCost
        assertNotNull(dto.getSimpleRegisteredLetterCost());
        assertEquals(entity.getSimpleRegisteredLetterCost().getCost(), dto.getSimpleRegisteredLetterCost().getCost());
        assertEquals(entity.getSimpleRegisteredLetterCost().getProductType(), dto.getSimpleRegisteredLetterCost().getProductType());

        assertEquals(entity.getIsDeleted(), dto.getIsDeleted());
        assertEquals(entity.getSendFee(), dto.getSendFee());
        assertEquals(entity.getPaFee(), dto.getPaFee());
        assertEquals(entity.getNotificationFeePolicy(), dto.getNotificationFeePolicy());
        assertEquals(entity.getPagoPaIntMode(), dto.getPagoPaIntMode());
        assertEquals(entity.getVat(), dto.getVat());
        assertEquals(entity.getLastUpdate(), dto.getLastUpdate());
        assertEquals(entity.getTtl(), dto.getTtl());
    }
}
