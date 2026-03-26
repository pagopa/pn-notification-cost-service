package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.config.PnNotificationCostServiceConfigs;
import it.pagopa.pn.notificationcostservice.model.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SimpleRegisteredLetterCost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationDeliveryCostMapperTest {

    private PnNotificationCostServiceConfigs configs;

    private NotificationDeliveryCostMapper mapper;

    @BeforeEach
    void init(){
        configs= Mockito.mock(PnNotificationCostServiceConfigs.class);
        Integer sendFee = 100;
        Mockito.when(configs.getSendFee()).thenReturn(sendFee);
        mapper= new NotificationDeliveryCostMapper(configs);
    }

    @Test
    @DisplayName("mapDtoToNotificationDeliveryCost mappa tutti i recipient in NotificationDeliveryCost")
    void shouldMapDtoToNotificationDeliveryCost() {
        RecipientCostDataDto recipient1 = new RecipientCostDataDto()
                .recIndex(0)
                .recipientInternalId("recipient-1")
                .senderInternalId("sender-1")
                .payments(List.of(new PaymentDataDto("IUV-1", true)))
                .paFee(50)
                .notificationFeePolicy(NotificationFeePolicyDto.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntModeDto.SYNC)
                .vat(22);
        RecipientCostDataDto recipient2 = new RecipientCostDataDto()
                .recIndex(1)
                .recipientInternalId("recipient-2")
                .senderInternalId("sender-2")
                .payments(List.of(new PaymentDataDto("IUV-2", true)))
                .paFee(80)
                .notificationFeePolicy(NotificationFeePolicyDto.FLAT_RATE)
                .pagoPaIntMode(PagoPaIntModeDto.ASYNC)
                .vat(10);


        NewNotificationCostRequestDto request = new NewNotificationCostRequestDto()
                .costRecipients(List.of(
                        recipient1,
                        recipient2
                ));

        List<NotificationDeliveryCost> result = mapper.mapDtoToNotificationDeliveryCost("IUN-123", request);

        assertThat(result).hasSize(2);

        assertThat(result.getFirst().getIun()).isEqualTo("IUN-123");
        assertThat(result.getFirst().getRecIndex()).isEqualTo(0);
        assertThat(result.getFirst().getRecipientInternalId()).isEqualTo("recipient-1");
        assertThat(result.getFirst().getSenderInternalId()).isEqualTo("sender-1");
        assertThat(result.getFirst().getVat()).isEqualTo(22);
        assertThat(result.getFirst().getPagoPaIntMode()).isEqualTo(PagoPaIntMode.SYNC);
        assertThat(result.getFirst().getNotificationFeePolicy()).isEqualTo(NotificationFeePolicy.DELIVERY_MODE);
        assertThat(result.getFirst().getBaseCost().getSendFee()).isEqualTo(100);
        assertThat(result.getFirst().getBaseCost().getPaFee()).isEqualTo(50);

        assertThat(result.get(1).getIun()).isEqualTo("IUN-123");
        assertThat(result.get(1).getRecIndex()).isEqualTo(1);
        assertThat(result.get(1).getRecipientInternalId()).isEqualTo("recipient-2");
        assertThat(result.get(1).getSenderInternalId()).isEqualTo("sender-2");
        assertThat(result.get(1).getVat()).isEqualTo(10);
        assertThat(result.get(1).getPagoPaIntMode()).isEqualTo(PagoPaIntMode.ASYNC);
        assertThat(result.get(1).getNotificationFeePolicy()).isEqualTo(NotificationFeePolicy.FLAT_RATE);
        assertThat(result.get(1).getBaseCost().getSendFee()).isEqualTo(100);
        assertThat(result.get(1).getBaseCost().getPaFee()).isEqualTo(80);
    }

    @Test
    @DisplayName("mapDtoToNotificationDeliveryCost lancia NullPointerException se dto è null")
    void shouldThrowWhenDtoIsNullInMapDtoToNotificationDeliveryCost() {
        assertThatThrownBy(() -> mapper.mapDtoToNotificationDeliveryCost("IUN-123", null))
                .isInstanceOf(NullPointerException.class);
    }

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
