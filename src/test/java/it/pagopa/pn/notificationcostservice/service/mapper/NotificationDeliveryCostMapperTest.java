package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogCostComponent;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogCostDetail;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.BaseCostDetail;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponse;
import it.pagopa.pn.notificationcostservice.dto.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationDeliveryCostMapperTest {

    private final NotificationDeliveryCostMapper mapper = new NotificationDeliveryCostMapper();

    @Test
    @DisplayName("Dovrebbe mappare correttamente i costi base e i metadati generali")
    void shouldMapBaseCostsAndMetadata() {
        // Given
        Instant now = Instant.now();
        NotificationDeliveryCostDto dto = createBaseDto();
        dto.setLastUpdate(now);
        dto.setVat(22);

        CalculatedCosts calculated = CalculatedCosts.builder()
                .totalCostWithVat(1000)
                .baseCost(500)
                .build();

        // When
        NotificationCostRecipientResponse response = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertThat(response.getLastUpdate()).isEqualTo(now);
        assertThat(response.getTotalCost().getCostWithVat()).isEqualTo(1000);
        assertThat(response.getTotalCost().getDetails().getVat()).isEqualTo(22);

        BaseCostDetail baseDetail = response.getTotalCost().getDetails().getBaseCostDetail();
        assertThat(baseDetail.getCost()).isEqualTo(500L);
        assertThat(baseDetail.getBaseCostComponents()).hasSize(2);
    }

    @Test
    @DisplayName("Caso impossibile: SimpleRegisteredLetterCost deve vincere su FirstAnalogCost")
    void shouldPrioritizeSimpleRegisteredLetterOverFirstAttempt() {
        // Given
        NotificationDeliveryCostDto dto = createBaseDto();

        SimpleRegisteredLetterCostDto simpleLetter = SimpleRegisteredLetterCostDto.builder().cost(100).productType("SIMPLE").build();
        FirstAnalogCostDto firstAttempt = FirstAnalogCostDto.builder().cost(200).productType("AR").build();

        dto.setSimpleRegisteredLetterCost(simpleLetter);
        dto.setFirstAnalogCost(firstAttempt);

        CalculatedCosts calculated = CalculatedCosts.builder()
                .analogCostWithVat(150)
                .build();

        // When
        NotificationCostRecipientResponse response = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertNotNull(response.getTotalCost().getDetails()
                .getAnalogCostDetail());
        List<AnalogCostComponent> components = response.getTotalCost().getDetails()
                .getAnalogCostDetail().getAnalogCostComponents();

        assertThat(components).hasSize(1);
        assertThat(components.getFirst().getCost()).isEqualTo(100L);
        assertThat(components.getFirst().getProductType()).isEqualTo("SIMPLE");
        assertThat(components.getFirst().getAttemptIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("Dovrebbe mappare entrambi i tentativi analogici se presenti (senza Simple Letter)")
    void shouldMapBothAnalogAttempts() {
        // Given
        NotificationDeliveryCostDto dto = createBaseDto();
        dto.setFirstAnalogCost(FirstAnalogCostDto.builder().cost(100).productType("AR").build());
        dto.setSecondAnalogCost(SecondAnalogCostDto.builder().cost(150).productType("AR").build());

        CalculatedCosts calculated = CalculatedCosts.builder()
                .analogCostWithVat(300)
                .build();

        // When
        NotificationCostRecipientResponse response = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertNotNull(response.getTotalCost());
        assertNotNull(response.getTotalCost().getDetails());
        AnalogCostDetail analogDetail = response.getTotalCost().getDetails().getAnalogCostDetail();
        assertNotNull(analogDetail);
        assertThat(analogDetail.getAnalogCostComponents()).hasSize(2);
        assertThat(analogDetail.getAnalogCostComponents().get(1).getAttemptIndex()).isEqualTo(1);
        assertThat(analogDetail.getAnalogCostComponents().get(1).getCost()).isEqualTo(150L);
    }

    @Test
    @DisplayName("Dovrebbe restituire AnalogCostDetail null se non ci sono costi analogici nel DTO")
    void shouldReturnNullAnalogDetailWhenNoAnalogCostsPresent() {
        // Given
        NotificationDeliveryCostDto dto = createBaseDto();
        // Nessun costo analogico impostato

        CalculatedCosts calculated = CalculatedCosts.builder().build();

        // When
        NotificationCostRecipientResponse response = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertThat(response.getTotalCost().getDetails().getAnalogCostDetail()).isNull();
    }

    @Test
    @DisplayName("Dovrebbe gestire correttamente le Enum e i valori null")
    void shouldHandleEnumsAndNulls() {
        // Given
        NotificationDeliveryCostDto dto = createBaseDto();
        dto.setPagoPaIntMode(null); // Caso null

        CalculatedCosts calculated = CalculatedCosts.builder().build();

        // When
        NotificationCostRecipientResponse response = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertThat(response.getPagoPaIntMode()).isNull();
    }

    // Helper per creare un DTO minimo valido
    private NotificationDeliveryCostDto createBaseDto() {
        BaseCostDto baseCost = BaseCostDto.builder().sendFee(100).paFee(50).build();
        return  NotificationDeliveryCostDto.builder().baseCost(baseCost).build();
    }
}
