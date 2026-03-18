package it.pagopa.pn.notificationcostservice.middleware.dao.paymentinfo.dynamo.mapper;

import it.pagopa.pn.notificationcostservice.middleware.dao.paymentinfo.dynamo.entity.PaymentInfoEntity;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtoToEntityPaymentInfoMapperTest {

    private DtoToEntityPaymentInfoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DtoToEntityPaymentInfoMapper();
    }

    @Test
    void dtoToEntityTest() {
        PaymentInfo dto = PaymentInfo.builder()
                .iun("iun")
                .recIndex(1)
                .iuv("iuv")
                .applyCost(true)
                .build();

        PaymentInfoEntity entity = mapper.dtoToEntity(dto);

        assertNotNull(entity);
        assertEquals("iun", entity.getIun());
        assertEquals(1, entity.getRecIndex());
        assertEquals("iuv", entity.getIuv());
        assertTrue(entity.isApplyCost());
    }

    @Test
    void dtoToEntityNullTest() {
        PaymentInfoEntity entity = mapper.dtoToEntity(null);
        assertNull(entity);
    }
}

