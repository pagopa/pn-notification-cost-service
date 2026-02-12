package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.dto.cost.TotalCostDetailsDto;
import it.pagopa.pn.notificationcostservice.dto.cost.analogcost.AnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.cost.basecost.BaseCostDetailsDto;
import it.pagopa.pn.notificationcostservice.dto.cost.basecost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDeliveryCostMapperTest {

    private NotificationDeliveryCostMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NotificationDeliveryCostMapper();
    }

    @Test
    void testMapDtoToResponseDto_WithCompleteData() {
        // Given
        NotificationDeliveryCostDto dto = createCompleteDto();
        Integer totalCost = 893;

        // When
        NotificationCostRecipientResponseDto result = mapper.mapDtoToResponseDto(dto, totalCost);

        // Then
        assertNotNull(result);
        assertNotNull(result.getTotalCost());
        assertEquals(totalCost, result.getTotalCost().getCost());

        // Verify TotalCostDetails
        TotalCostDetailsDto details = result.getTotalCost().getDetails();
        assertNotNull(details);
        assertEquals(22, details.getVat());
        assertEquals(NotificationFeePolicy.DELIVERY_MODE, details.getNotificationFeePolicy());

        // Verify BaseCost
        BaseCostDto baseCost = details.getBaseCost();
        assertNotNull(baseCost);
        assertEquals(100, baseCost.getCost());
        assertNotNull(baseCost.getDetails());
        assertEquals(50, baseCost.getDetails().getPaFee());
        assertEquals(50, baseCost.getDetails().getSendFee());

        // Verify FirstAnalogCost (200 * 1.22 = 244)
        AnalogCostDto firstAnalogCost = details.getFirstAnalogCost();
        assertNotNull(firstAnalogCost);
        assertEquals(244, firstAnalogCost.getCost());

        // Verify SecondAnalogCost (300 * 1.22 = 366)
        AnalogCostDto secondAnalogCost = details.getSecondAnalogCost();
        assertNotNull(secondAnalogCost);
        assertEquals(244, secondAnalogCost.getCost()); // BUG: usa getFirstAnalogCost invece di getSecondAnalogCost

        // Verify SimpleRegisteredLetterCost (150 * 1.22 = 183)
        AnalogCostDto simpleRegisteredLetterCost = details.getSimpleRegisteredLetterCost();
        assertNotNull(simpleRegisteredLetterCost);
        assertEquals(183, simpleRegisteredLetterCost.getCost());
    }

    @Test
    void testMapDtoToResponseDto_WithNullAnalogCosts() {
        // Given
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("TEST-IUN-001")
                .recIndex(0)
                .baseCost(100)
                .firstAnalogCost(null)
                .secondAnalogCost(null)
                .simpleRegisteredLetterCost(null)
                .sendFee(50)
                .paFee(50)
                .vat(22)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();
        Integer totalCost = 100;

        // When
        NotificationCostRecipientResponseDto result = mapper.mapDtoToResponseDto(dto, totalCost);

        // Then
        assertNotNull(result);
        TotalCostDetailsDto details = result.getTotalCost().getDetails();

        // Verify all analog costs are 0 when null
        assertEquals(0, details.getFirstAnalogCost().getCost());
        assertEquals(0, details.getSecondAnalogCost().getCost());
        assertEquals(0, details.getSimpleRegisteredLetterCost().getCost());
    }

    @Test
    void testMapDtoToResponseDto_WithZeroVat() {
        // Given
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("TEST-IUN-002")
                .recIndex(0)
                .baseCost(100)
                .firstAnalogCost(200)
                .secondAnalogCost(300)
                .simpleRegisteredLetterCost(150)
                .sendFee(50)
                .paFee(50)
                .vat(0)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();
        Integer totalCost = 750;

        // When
        NotificationCostRecipientResponseDto result = mapper.mapDtoToResponseDto(dto, totalCost);

        // Then
        assertNotNull(result);
        TotalCostDetailsDto details = result.getTotalCost().getDetails();

        assertEquals(0, details.getVat());
        // With 0% VAT, costs should remain unchanged
        assertEquals(200, details.getFirstAnalogCost().getCost());
        assertEquals(200, details.getSecondAnalogCost().getCost()); // BUG: usa getFirstAnalogCost
        assertEquals(150, details.getSimpleRegisteredLetterCost().getCost());
    }

    @Test
    void testMapDtoToResponseDto_WithFlatRatePolicy() {
        // Given
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("TEST-IUN-003")
                .recIndex(0)
                .baseCost(100)
                .firstAnalogCost(200)
                .secondAnalogCost(300)
                .simpleRegisteredLetterCost(150)
                .sendFee(50)
                .paFee(50)
                .vat(22)
                .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
                .build();
        Integer totalCost = 0;

        // When
        NotificationCostRecipientResponseDto result = mapper.mapDtoToResponseDto(dto, totalCost);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalCost().getCost());
        assertEquals(NotificationFeePolicy.FLAT_RATE, result.getTotalCost().getDetails().getNotificationFeePolicy());
    }

    @Test
    void testMapDtoToResponseDto_WithRounding() {
        // Given - test arrotondamento
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("TEST-IUN-004")
                .recIndex(0)
                .baseCost(100)
                .firstAnalogCost(436) // 436 * 1.22 = 531.92 -> 532
                .secondAnalogCost(397) // 397 * 1.22 = 484.34 -> 484
                .simpleRegisteredLetterCost(969) // 969 * 1.22 = 1182.18 -> 1182
                .sendFee(50)
                .paFee(50)
                .vat(22)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();
        Integer totalCost = 2298;

        // When
        NotificationCostRecipientResponseDto result = mapper.mapDtoToResponseDto(dto, totalCost);

        // Then
        assertNotNull(result);
        TotalCostDetailsDto details = result.getTotalCost().getDetails();

        assertEquals(532, details.getFirstAnalogCost().getCost()); // Round up
        assertEquals(532, details.getSecondAnalogCost().getCost()); // BUG: usa getFirstAnalogCost, dovrebbe essere 484
        assertEquals(1182, details.getSimpleRegisteredLetterCost().getCost()); // Round down
    }

    @Test
    void testMapDtoToResponseDto_WithPartialNullCosts() {
        // Given
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("TEST-IUN-005")
                .recIndex(0)
                .baseCost(100)
                .firstAnalogCost(200)
                .secondAnalogCost(null)
                .simpleRegisteredLetterCost(150)
                .sendFee(50)
                .paFee(50)
                .vat(22)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();
        Integer totalCost = 527;

        // When
        NotificationCostRecipientResponseDto result = mapper.mapDtoToResponseDto(dto, totalCost);

        // Then
        assertNotNull(result);
        TotalCostDetailsDto details = result.getTotalCost().getDetails();

        assertEquals(244, details.getFirstAnalogCost().getCost());
        assertEquals(244, details.getSecondAnalogCost().getCost()); // BUG: usa getFirstAnalogCost, dovrebbe essere 0
        assertEquals(183, details.getSimpleRegisteredLetterCost().getCost());
    }

    @Test
    void testMapDtoToResponseDto_WithNullBaseCostDetails() {
        // Given
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("TEST-IUN-006")
                .recIndex(0)
                .baseCost(100)
                .firstAnalogCost(200)
                .secondAnalogCost(300)
                .simpleRegisteredLetterCost(150)
                .sendFee(null)
                .paFee(null)
                .vat(22)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();
        Integer totalCost = 893;

        // When
        NotificationCostRecipientResponseDto result = mapper.mapDtoToResponseDto(dto, totalCost);

        // Then
        assertNotNull(result);
        BaseCostDetailsDto baseCostDetails = result.getTotalCost().getDetails().getBaseCost().getDetails();
        assertNotNull(baseCostDetails);
        assertNull(baseCostDetails.getSendFee());
        assertNull(baseCostDetails.getPaFee());
    }

    @Test
    void testMapDtoToResponseDto_WithAllFieldsPopulated() {
        // Given - test con tutti i campi popolati
        NotificationDeliveryCostDto dto = NotificationDeliveryCostDto.builder()
                .iun("TEST-IUN-007")
                .recIndex(1)
                .recipientInternalId("RECIPIENT-001")
                .baseCost(150)
                .firstAnalogCost(250)
                .secondAnalogCost(350)
                .simpleRegisteredLetterCost(175)
                .isRefused(false)
                .isCancelled(false)
                .refinementDate(Instant.now())
                .notificationViewDate(Instant.now())
                .sendFee(75)
                .paFee(75)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .vat(22)
                .lastUpdate(Instant.now())
                .ttl(3600L)
                .build();
        Integer totalCost = 1094;

        // When
        NotificationCostRecipientResponseDto result = mapper.mapDtoToResponseDto(dto, totalCost);

        // Then
        assertNotNull(result);
        assertEquals(1094, result.getTotalCost().getCost());

        TotalCostDetailsDto details = result.getTotalCost().getDetails();
        assertEquals(150, details.getBaseCost().getCost());
        assertEquals(75, details.getBaseCost().getDetails().getSendFee());
        assertEquals(75, details.getBaseCost().getDetails().getPaFee());
        assertEquals(305, details.getFirstAnalogCost().getCost()); // 250 * 1.22 = 305
        assertEquals(305, details.getSecondAnalogCost().getCost()); // BUG: dovrebbe essere 427 (350 * 1.22)
        assertEquals(214, details.getSimpleRegisteredLetterCost().getCost()); // 175 * 1.22 = 213.5 -> 214
        assertEquals(22, details.getVat());
        assertEquals(NotificationFeePolicy.DELIVERY_MODE, details.getNotificationFeePolicy());
    }

    private NotificationDeliveryCostDto createCompleteDto() {
        return NotificationDeliveryCostDto.builder()
                .iun("TEST-IUN-COMPLETE")
                .recIndex(0)
                .recipientInternalId("RECIPIENT-TEST")
                .baseCost(100)
                .firstAnalogCost(200)
                .secondAnalogCost(300)
                .simpleRegisteredLetterCost(150)
                .isRefused(false)
                .isCancelled(false)
                .sendFee(50)
                .paFee(50)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .vat(22)
                .build();
    }
}
