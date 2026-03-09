package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogCostDetailDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.AnalogCostNameDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.BaseCostDetailDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.model.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationDeliveryCostMapperTest {

    private final NotificationDeliveryCostMapper mapper = new NotificationDeliveryCostMapper();

    @Test
    @DisplayName("Dovrebbe mappare correttamente i costi base e i metadati generali")
    void shouldMapBaseCostsAndMetadata() {
        // Given
        Instant now = Instant.now();
        NotificationDeliveryCost dto = NotificationDeliveryCostTestBuilder.builder().build();
        dto.setLastUpdate(now);

        CalculatedCosts calculated = CalculatedCosts.builder()
                .totalCostWithVat(1000)
                .baseCost(500)
                .build();

        // When
        NotificationCostRecipientResponseDto response = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertThat(response.getLastUpdate()).isEqualTo(now);
        assertThat(response.getTotalCost().getCostWithVat()).isEqualTo(1000);

        BaseCostDetailDto baseDetail = response.getTotalCost().getDetails().getBaseCostDetail();
        assertThat(baseDetail.getCost()).isEqualTo(500L);
        assertThat(baseDetail.getBaseCostComponents()).hasSize(2);
    }

    @Test
    @DisplayName("Dovrebbe mappare correttamente il costo del primo tentativo analogico usando Simple Registered Letter se presente, altrimenti usando First Attempt")
    void shouldMapFirstAnalogAttempt() {
        // Given
        NotificationDeliveryCost dto = NotificationDeliveryCostTestBuilder.builder()
                .withSimpleRegisteredLetterCost(SimpleRegisteredLetterCost.builder().cost(100).productType("AR").build())
                .withVat(22)
                .build();

        CalculatedCosts calculated = CalculatedCosts.builder()
                .analogCostWithVat(300)
                .build();

        // When
        NotificationCostRecipientResponseDto firstResponse = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertNotNull(firstResponse.getTotalCost());
        assertNotNull(firstResponse.getTotalCost().getDetails());
        AnalogCostDetailDto firstResponseAnalogDetail = firstResponse.getTotalCost().getDetails().getAnalogCostDetail();
        assertNotNull(firstResponseAnalogDetail);
        assertThat(firstResponseAnalogDetail.getVat()).isEqualTo(22);
        assertThat(firstResponseAnalogDetail.getAnalogCostComponents()).hasSize(1);
        assertThat(firstResponseAnalogDetail.getAnalogCostComponents().getFirst().getCostName()).isEqualTo(AnalogCostNameDto.FIRST_ATTEMPT);
        assertThat(firstResponseAnalogDetail.getAnalogCostComponents().getFirst().getCost()).isEqualTo(100);

        // Modifico il DTO per rimuovere Simple Registered Letter e aggiungere First Attempt
        dto.setSimpleRegisteredLetterCost(null);
        dto.setFirstAnalogCost(FirstAnalogCost.builder().cost(150).productType("AR").build());
        // When
        NotificationCostRecipientResponseDto secondResponse = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertNotNull(secondResponse.getTotalCost());
        assertNotNull(secondResponse.getTotalCost().getDetails());
        AnalogCostDetailDto analogDetail = secondResponse.getTotalCost().getDetails().getAnalogCostDetail();
        assertNotNull(analogDetail);
        assertThat(analogDetail.getVat()).isEqualTo(22);
        assertThat(analogDetail.getAnalogCostComponents()).hasSize(1);
        assertThat(analogDetail.getAnalogCostComponents().getFirst().getCostName()).isEqualTo(AnalogCostNameDto.FIRST_ATTEMPT);
        assertThat(analogDetail.getAnalogCostComponents().getFirst().getCost()).isEqualTo(150);
    }

    @Test
    @DisplayName("Dovrebbe mappare entrambi i tentativi analogici se presenti (senza Simple Letter)")
    void shouldMapBothAnalogAttempts() {
        // Given
        NotificationDeliveryCost dto = NotificationDeliveryCostTestBuilder.builder()
                .withFirstAnalogCost(FirstAnalogCost.builder().cost(100).productType("AR").build())
                .withSecondAnalogCost(SecondAnalogCost.builder().cost(150).productType("AR").build())
                .withVat(22)
                .build();

        CalculatedCosts calculated = CalculatedCosts.builder()
                .analogCostWithVat(300)
                .build();

        // When
        NotificationCostRecipientResponseDto response = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertNotNull(response.getTotalCost());
        assertNotNull(response.getTotalCost().getDetails());
        AnalogCostDetailDto analogDetail = response.getTotalCost().getDetails().getAnalogCostDetail();
        assertNotNull(analogDetail);
        assertThat(analogDetail.getVat()).isEqualTo(22);
        assertThat(analogDetail.getAnalogCostComponents()).hasSize(2);
        assertThat(analogDetail.getAnalogCostComponents().get(0).getCostName()).isEqualTo(AnalogCostNameDto.FIRST_ATTEMPT);
        assertThat(analogDetail.getAnalogCostComponents().get(0).getCost()).isEqualTo(100);
        assertThat(analogDetail.getAnalogCostComponents().get(1).getCostName()).isEqualTo(AnalogCostNameDto.SECOND_ATTEMPT);
        assertThat(analogDetail.getAnalogCostComponents().get(1).getCost()).isEqualTo(150);
    }

    @Test
    @DisplayName("Dovrebbe restituire AnalogCostDetailDto null se non ci sono costi analogici nel DTO")
    void shouldReturnNullAnalogDetailWhenNoAnalogCostsPresent() {
        // Given
        NotificationDeliveryCost dto = NotificationDeliveryCostTestBuilder.builder().build();

        // Nessun costo analogico impostato
        CalculatedCosts calculated = CalculatedCosts.builder().build();

        // When
        NotificationCostRecipientResponseDto response = mapper.mapDtoToResponse(dto, calculated);

        // Then
        assertThat(response.getTotalCost().getDetails().getAnalogCostDetail()).isNull();
    }
}
