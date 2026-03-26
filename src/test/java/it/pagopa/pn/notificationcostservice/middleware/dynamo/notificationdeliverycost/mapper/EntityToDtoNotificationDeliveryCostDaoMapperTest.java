package it.pagopa.pn.notificationcostservice.middleware.dynamo.notificationdeliverycost.mapper;

import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.BaseCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.FirstAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.analogcost.SecondAnalogCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.notificationdeliverycost.EntityToDtoNotificationDeliveryCostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EntityToDtoNotificationDeliveryCostDaoMapperTest {
    private  EntityToDtoNotificationDeliveryCostMapper mapper;

    @BeforeEach
    void before() {
        mapper = new EntityToDtoNotificationDeliveryCostMapper();
    }

    @Test
    void entity2Dto() {
        Instant now = Instant.now();

        FirstAnalogCostEntity firstCost = FirstAnalogCostEntity.builder()
                .cost(50)
                .productType("AR")
                .build();

        SecondAnalogCostEntity secondCost = SecondAnalogCostEntity.builder()
                .cost(30)
                .productType("890")
                .build();

        BaseCostEntity baseCostEntity = BaseCostEntity.builder()
                .paFee(50)
                .sendFee(50)
                .build();

        NotificationDeliveryCostEntity entity = NotificationDeliveryCostEntity.builder()
                .iun("IUN123")
                .recIndex(0)
                .recipientInternalId("recipientId")
                .baseCost(baseCostEntity)
                .senderPaId("sender")
                .firstAnalogCost(firstCost)
                .secondAnalogCost(secondCost)
                .isDeleted(false)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .vat(22)
                .lastUpdate(now)
                .ttl(3600L)
                .senderTaxId("taxId")
                .build();

        NotificationDeliveryCost dto = mapper.entity2Dto(entity);

        assertEquals(entity.getIun(), dto.getIun());
        assertEquals(entity.getRecIndex(), dto.getRecIndex());
        assertEquals(entity.getRecipientInternalId(), dto.getRecipientInternalId());
        assertEquals(entity.getSenderPaId(), dto.getSenderPaId());

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
        assertNull(dto.getSimpleRegisteredLetterCost());

        assertEquals(entity.getIsDeleted(), dto.getIsDeleted());
        assertEquals(entity.getNotificationFeePolicy(), dto.getNotificationFeePolicy());
        assertEquals(entity.getPagoPaIntMode(), dto.getPagoPaIntMode());
        assertEquals(entity.getVat(), dto.getVat());
        assertEquals(entity.getLastUpdate(), dto.getLastUpdate());
        assertEquals(entity.getTtl(), dto.getTtl());
    }

    @Test
    void entity2DtoWithNullAnalogCosts() {
        Instant now = Instant.now();

        BaseCostEntity baseCostEntity = BaseCostEntity.builder()
                .paFee(50)
                .sendFee(50)
                .build();

        NotificationDeliveryCostEntity entity = NotificationDeliveryCostEntity.builder()
                .iun("IUN123")
                .recIndex(0)
                .recipientInternalId("recipientId")
                .baseCost(baseCostEntity)
                .senderPaId("sender")
                .firstAnalogCost(null)
                .secondAnalogCost(null)
                .simpleRegisteredLetterCost(null)
                .isDeleted(false)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .vat(22)
                .lastUpdate(now)
                .ttl(3600L)
                .senderTaxId("taxId")
                .build();

        NotificationDeliveryCost dto = mapper.entity2Dto(entity);

        assertEquals(entity.getIun(), dto.getIun());
        assertEquals(entity.getRecIndex(), dto.getRecIndex());
        assertNotNull(dto.getBaseCost());
        assertNull(dto.getFirstAnalogCost());
        assertNull(dto.getSecondAnalogCost());
        assertNull(dto.getSimpleRegisteredLetterCost());
    }
}
