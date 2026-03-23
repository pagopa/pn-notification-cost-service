package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationCostUpdaterMapperTest {

    private final NotificationCostUpdaterMapper mapper = new NotificationCostUpdaterMapper();

    @Test
    @DisplayName("mapNotificationCostUpdater deve mappare i campi previsti in fase VALIDATION")
    void shouldMapNotificationDeliveryCostToEntityForValidationPhase() {
        NotificationDeliveryCost dto = NotificationDeliveryCostTestBuilder.builder()
                .withIun("IUN-123")
                .withRecIndex(2)
                .withBaseCost(BaseCost.builder()
                        .sendFee(120)
                        .paFee(80)
                        .build())
                .withVat(22)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withFirstAnalogCost(FirstAnalogCost.builder()
                        .cost(100)
                        .productType("AR")
                        .build())
                .withSecondAnalogCost(SecondAnalogCost.builder()
                        .cost(150)
                        .productType("890")
                        .build())
                .withIsDeleted(true)
                .build();

        NotificationDeliveryCostEntity result =
                mapper.mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, dto);

        assertThat(result).isNotNull();
        assertThat(result.getIun()).isEqualTo("IUN-123");
        assertThat(result.getRecIndex()).isEqualTo(2);
        assertThat(result.getBaseCost()).isNotNull();
        assertThat(result.getBaseCost().getSendFee()).isEqualTo(120);
        assertThat(result.getBaseCost().getPaFee()).isEqualTo(80);
        assertThat(result.getVat()).isEqualTo(22);
        assertThat(result.getNotificationFeePolicy()).isEqualTo(NotificationFeePolicy.DELIVERY_MODE);
        assertThat(result.getPagoPaIntMode()).isEqualTo(PagoPaIntMode.ASYNC);

        // campi volutamente non aggiornati dal mapper
        assertThat(result.getRecipientInternalId()).isNull();
        assertThat(result.getSenderInternalId()).isNull();
        assertThat(result.getFirstAnalogCost()).isNull();
        assertThat(result.getSecondAnalogCost()).isNull();
        assertThat(result.getSimpleRegisteredLetterCost()).isNull();
        assertThat(result.getIsDeleted()).isNull();
    }

    @Test
    @DisplayName("mapNotificationCostUpdater deve lanciare NullPointerException se updateCostPhase è null")
    void shouldThrowWhenUpdateCostPhaseIsNull() {
        NotificationDeliveryCost dto = NotificationDeliveryCostTestBuilder.builder()
                .withIun("IUN-123")
                .withRecIndex(0)
                .build();

        assertThatThrownBy(() -> mapper.mapNotificationCostUpdater(null, dto))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("mapNotificationCostUpdater deve lanciare NullPointerException se notificationDeliveryCost è null")
    void shouldThrowWhenNotificationDeliveryCostIsNull() {
        assertThatThrownBy(() ->
                mapper.mapNotificationCostUpdater(CostUpdatePhaseInt.VALIDATION, null)
        ).isInstanceOf(NullPointerException.class);
    }
}
