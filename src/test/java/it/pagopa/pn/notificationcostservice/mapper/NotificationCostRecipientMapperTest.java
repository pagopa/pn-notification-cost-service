package it.pagopa.pn.notificationcostservice.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDetailsDto;
import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDto;
import it.pagopa.pn.notificationcostservice.dto.cost.analogcost.AnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.cost.basecost.BaseCostDetailsDto;
import it.pagopa.pn.notificationcostservice.dto.cost.basecost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationCostRecipientMapperTest {

    private NotificationCostRecipientMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NotificationCostRecipientMapper();
    }

    @Test
    void testDtoResponse2Response_WithNullDto_ReturnsNull() {
        NotificationCostRecipientResponse response = mapper.dtoResponse2Response(null);
        assertNull(response);
    }

    @Test
    void testDtoResponse2Response_WithCompleteDto_MapsAllFields() {
        NotificationCostRecipientResponseDto dto = createCompleteDto();

        NotificationCostRecipientResponse response = mapper.dtoResponse2Response(dto);

        assertNotNull(response);
        assertNotNull(response.getTotalCost());
        assertEquals(1500, response.getTotalCost().getCost());
        assertEquals(NotificationCostRecipientResponse.PagoPaIntModeEnum.ASYNC, response.getPagoPaIntMode());

        TotalCostDetails details = response.getTotalCost().getDetails();
        assertNotNull(details);
        assertNotNull(details.getBaseCost());
        assertEquals(100, details.getBaseCost().getCost());
        assertNotNull(details.getBaseCost().getDetails());
        assertEquals(50, details.getBaseCost().getDetails().getPaFee());
        assertEquals(50, details.getBaseCost().getDetails().getSendFee());

        assertNotNull(details.getFirstAnalogCost());
        assertEquals(300, details.getFirstAnalogCost().getCost());

        assertNotNull(details.getSecondAnalogCost());
        assertEquals(400, details.getSecondAnalogCost().getCost());

        assertNotNull(details.getSimpleRegisteredLetterCost());
        assertEquals(200, details.getSimpleRegisteredLetterCost().getCost());

        assertEquals(22, details.getVat());
        assertEquals(TotalCostDetails.NotificationFeePolicyEnum.DELIVERY_MODE, details.getNotificationFeePolicy());
    }

    @Test
    void testDtoResponse2Response_WithNullTotalCost_HandlesGracefully() {
        NotificationCostRecipientResponseDto dto = new NotificationCostRecipientResponseDto();
        dto.setPagoPaIntMode(PagoPaIntMode.SYNC);

        NotificationCostRecipientResponse response = mapper.dtoResponse2Response(dto);

        assertNotNull(response);
        assertNull(response.getTotalCost());
        assertEquals(NotificationCostRecipientResponse.PagoPaIntModeEnum.SYNC, response.getPagoPaIntMode());
    }

    @Test
    void testDtoResponse2Response_WithNullDetails_HandlesGracefully() {
        NotificationCostRecipientResponseDto dto = new NotificationCostRecipientResponseDto();
        TotalCostDto totalCost = new TotalCostDto();
        totalCost.setCost(1000);
        dto.setTotalCost(totalCost);

        NotificationCostRecipientResponse response = mapper.dtoResponse2Response(dto);

        assertNotNull(response);
        assertNotNull(response.getTotalCost());
        assertEquals(1000, response.getTotalCost().getCost());
        assertNull(response.getTotalCost().getDetails());
    }

    @Test
    void testDtoResponse2Response_WithNullBaseCostDetails_HandlesGracefully() {
        NotificationCostRecipientResponseDto dto = new NotificationCostRecipientResponseDto();
        TotalCostDto totalCost = new TotalCostDto();
        totalCost.setCost(1000);
        TotalCostDetailsDto detailsDto = new TotalCostDetailsDto();
        BaseCostDto baseCostDto = new BaseCostDto();
        baseCostDto.setCost(100);
        detailsDto.setBaseCost(baseCostDto);
        totalCost.setDetails(detailsDto);
        dto.setTotalCost(totalCost);

        NotificationCostRecipientResponse response = mapper.dtoResponse2Response(dto);

        assertNotNull(response);
        assertNotNull(response.getTotalCost());
        assertNotNull(response.getTotalCost().getDetails());
        assertNotNull(response.getTotalCost().getDetails().getBaseCost());
        assertEquals(100, response.getTotalCost().getDetails().getBaseCost().getCost());
        assertNull(response.getTotalCost().getDetails().getBaseCost().getDetails());
    }

    @Test
    void testDtoResponse2Response_WithNullAnalogCosts_HandlesGracefully() {
        NotificationCostRecipientResponseDto dto = new NotificationCostRecipientResponseDto();
        TotalCostDto totalCost = new TotalCostDto();
        TotalCostDetailsDto detailsDto = new TotalCostDetailsDto();
        totalCost.setDetails(detailsDto);
        dto.setTotalCost(totalCost);

        NotificationCostRecipientResponse response = mapper.dtoResponse2Response(dto);

        assertNotNull(response);
        assertNotNull(response.getTotalCost());
        TotalCostDetails details = response.getTotalCost().getDetails();
        assertNotNull(details);
        assertNull(details.getFirstAnalogCost());
        assertNull(details.getSecondAnalogCost());
        assertNull(details.getSimpleRegisteredLetterCost());
    }

    private NotificationCostRecipientResponseDto createCompleteDto() {
        NotificationCostRecipientResponseDto dto = new NotificationCostRecipientResponseDto();

        TotalCostDto totalCost = new TotalCostDto();
        totalCost.setCost(1500);

        TotalCostDetailsDto details = new TotalCostDetailsDto();

        BaseCostDto baseCost = new BaseCostDto();
        baseCost.setCost(100);
        BaseCostDetailsDto baseCostDetails = new BaseCostDetailsDto();
        baseCostDetails.setPaFee(50);
        baseCostDetails.setSendFee(50);
        baseCost.setDetails(baseCostDetails);
        details.setBaseCost(baseCost);

        AnalogCostDto firstAnalog = new AnalogCostDto();
        firstAnalog.setCost(300);
        details.setFirstAnalogCost(firstAnalog);

        AnalogCostDto secondAnalog = new AnalogCostDto();
        secondAnalog.setCost(400);
        details.setSecondAnalogCost(secondAnalog);
        AnalogCostDto simpleRegistered = new AnalogCostDto();
        simpleRegistered.setCost(200);
        details.setSimpleRegisteredLetterCost(simpleRegistered);

        details.setVat(22);
        details.setNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);

        totalCost.setDetails(details);
        dto.setTotalCost(totalCost);
        dto.setPagoPaIntMode(PagoPaIntMode.ASYNC);

        return dto;
    }
}

