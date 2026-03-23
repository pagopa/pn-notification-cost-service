package it.pagopa.pn.notificationcostservice.middleware.paymentinfo.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.paymentinfo.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.mapper.paymentinfo.EntityToDtoPaymentInfoMapper;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityToDtoPaymentInfoMapperTest {

    private EntityToDtoPaymentInfoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EntityToDtoPaymentInfoMapper();
    }

    @Test
    void entityToDtoTest() {
        PaymentInfoEntity entity = new PaymentInfoEntity();
        entity.setIun("iun");
        entity.setRecIndex(1);
        entity.setIuv("iuv");
        entity.setApplyCost(true);

        PaymentInfo dto = mapper.entityToDto(entity);

        assertNotNull(dto);
        assertEquals("iun", dto.getIun());
        assertEquals(1, dto.getRecIndex());
        assertEquals("iuv", dto.getIuv());
        assertTrue(dto.isApplyCost());
    }

    @Test
    void entityToDtoNullTest() {
        PaymentInfo dto = mapper.entityToDto(null);
        assertNull(dto);
    }
}

