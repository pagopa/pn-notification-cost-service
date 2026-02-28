package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.dto.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDeliveryCostMapperTest {

    private NotificationDeliveryCostMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NotificationDeliveryCostMapper();
    }

    @Test
    void mapDtoToResponse_ReturnsNullFieldsWhenDtoFieldsAreNull() {
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder().build();

        CalculatedCosts calculatedCosts = CalculatedCosts.builder().build();

        var response = mapper.mapDtoToResponse(dto, calculatedCosts);

        assertNull(response.getTotalCost().getDetails().getBaseCost().getDetails().getPaFee().getCost());
        assertNull(response.getTotalCost().getDetails().getBaseCost().getDetails().getSendFee().getCost());
        assertNull(response.getTotalCost().getDetails().getFirstAnalogCost());
        assertNull(response.getTotalCost().getDetails().getSecondAnalogCost());
        assertNull(response.getTotalCost().getDetails().getVat());
        assertNull(response.getTotalCost().getDetails().getNotificationFeePolicy());
    }

    @Test
    void mapDtoToResponse_UsesSimpleRegisteredLetterCostWhenFirstAnalogCostIsNull() {
        NotificationDeliveryCostDto dto = createCompleteDto();
        dto.setFirstAnalogCost(null);

        CalculatedCosts calculatedCosts = CalculatedCosts.builder().build();

        var response = mapper.mapDtoToResponse(dto, calculatedCosts);

        assertNotNull(response.getTotalCost().getDetails().getFirstAnalogCost());
        assertEquals(dto.getSimpleRegisteredLetterCost().getCost(),
                response.getTotalCost().getDetails().getFirstAnalogCost().getCost());
    }

    @Test
    void mapDtoToResponse_ReturnsNullForAnalogCostsWhenBothAreNull() {
        NotificationDeliveryCostDto dto = createCompleteDto();
        dto.setFirstAnalogCost(null);
        dto.setSimpleRegisteredLetterCost(null);
        dto.setSecondAnalogCost(null);

        CalculatedCosts calculatedCosts = CalculatedCosts.builder().build();

        var response = mapper.mapDtoToResponse(dto, calculatedCosts);

        assertNull(response.getTotalCost().getDetails().getFirstAnalogCost());
        assertNull(response.getTotalCost().getDetails().getSecondAnalogCost());
    }

    @Test
    void mapDtoToResponse_MapsAllFieldsCorrectlyForCompleteDto() {
        NotificationDeliveryCostDto dto = createCompleteDto();

        CalculatedCosts calculatedCosts = CalculatedCosts.builder().build();

        var response = mapper.mapDtoToResponse(dto, calculatedCosts);

        assertEquals(dto.getVat(), response.getTotalCost().getDetails().getVat());
        assertEquals(dto.getNotificationFeePolicy().name(), response.getTotalCost().getDetails().getNotificationFeePolicy().name());
        assertEquals(dto.getFirstAnalogCost().getCost(), response.getTotalCost().getDetails().getFirstAnalogCost().getCost());
        assertEquals(dto.getSecondAnalogCost().getCost(), response.getTotalCost().getDetails().getSecondAnalogCost().getCost());
        assertEquals(dto.getBaseCost().getPaFee(), response.getTotalCost().getDetails().getBaseCost().getDetails().getPaFee().getCost());
        assertEquals(dto.getBaseCost().getSendFee(), response.getTotalCost().getDetails().getBaseCost().getDetails().getSendFee().getCost());
    }

    private NotificationDeliveryCostDto createCompleteDto() {
        return NotificationDeliveryCostDto.builder()
                .iun("TEST-IUN-COMPLETE")
                .recIndex(0)
                .recipientInternalId("RECIPIENT-TEST")
                .baseCost(BaseCostDto.builder()
                        .sendFee(50)
                        .paFee(50)
                        .build())
                .firstAnalogCost(FirstAnalogCostDto.builder()
                        .cost(200)
                        .build())
                .secondAnalogCost(SecondAnalogCostDto.builder()
                        .cost(300)
                        .build())
                .simpleRegisteredLetterCost(SimpleRegisteredLetterCostDto.builder()
                        .cost(150)
                        .build())
                .sendFee(50)
                .paFee(50)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .vat(22)
                .build();
    }
}
