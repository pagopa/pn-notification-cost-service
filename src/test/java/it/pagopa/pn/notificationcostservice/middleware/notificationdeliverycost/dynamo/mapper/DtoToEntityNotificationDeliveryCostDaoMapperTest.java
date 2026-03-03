package it.pagopa.pn.notificationcostservice.middleware.notificationdeliverycost.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.dynamo.mapper.DtoToEntityNotificationDeliveryCostMapper;
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

        FirstAnalogCostDto firstCost = FirstAnalogCostDto.builder()
                .cost(50)
                .productType("AR")
                .build();

        SecondAnalogCostDto secondCost = SecondAnalogCostDto.builder()
                .cost(30)
                .productType("890")
                .build();

        SimpleRegisteredLetterCostDto simpleCost = SimpleRegisteredLetterCostDto.builder()
                .cost(20)
                .productType("RS")
                .build();

        BaseCostDto baseCost = BaseCostDto.builder()
                .paFee(50)
                .sendFee(50)
                .build();

        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("IUN123")
                .recIndex(0)
                .recipientInternalId("recipientId")
                .baseCost(baseCost)
                .firstAnalogCost(firstCost)
                .secondAnalogCost(secondCost)
                .simpleRegisteredLetterCost(simpleCost)
                .isDeleted(false)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .vat(22)
                .senderInternalId("sender")
                .lastUpdate(now)
                .ttl(3600L)
                .build();

        NotificationDeliveryCostEntity entity = mapper.dto2Entity(dto);

        assertEquals(dto.getIun(), entity.getIun());
        assertEquals(dto.getRecIndex(), entity.getRecIndex());
        assertEquals(dto.getRecipientInternalId(), entity.getRecipientInternalId());
        assertEquals(dto.getSenderInternalId(), entity.getSenderInternalId());

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
        assertEquals(dto.getSimpleRegisteredLetterCost().getCost(), entity.getSimpleRegisteredLetterCost().getCost());
        assertEquals(dto.getSimpleRegisteredLetterCost().getProductType(), entity.getSimpleRegisteredLetterCost().getProductType());

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

        BaseCostDto baseCost = BaseCostDto.builder()
                .paFee(50)
                .sendFee(50)
                .build();

        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
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
                .senderInternalId("sender")
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
