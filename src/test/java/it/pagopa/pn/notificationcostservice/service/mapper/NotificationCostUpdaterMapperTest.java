package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class NotificationCostUpdaterMapperTest {

    private static final String IUN = "IUN-123";
    private static final Integer REC_INDEX = 2;
    private static final Integer COST = 120;
    private static final String PRODUCT_TYPE = "AR";

    private final NotificationCostUpdaterMapper mapper = new NotificationCostUpdaterMapper();

    @Test
    void toEntityForBaseCostUpdate_shouldMapBaseCostAndRelatedFieldsCorrectly() {
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
                .withSenderPaId("TEST-SENDER-PA-ID")
                .withSenderTaxId("TEST-SENDER-TAX-ID")
                .withLastUpdate(Instant.now())
                .build();

        NotificationDeliveryCostEntity result =
                mapper.toEntityForBaseCostUpdate(dto);

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
        assertThat(result.getFirstAnalogCost()).isNull();
        assertThat(result.getSecondAnalogCost()).isNull();
        assertThat(result.getSimpleRegisteredLetterCost()).isNull();
        assertThat(result.getIsDeleted()).isNull();
    }

    @Test
    void mapNotificationCostUpdater_shouldMapSimpleRegisteredLetter() {
        NotificationCostUpdate dto = buildUpdate(CostUpdatePhaseInt.SEND_SIMPLE_REGISTERED_LETTER);

        NotificationDeliveryCostEntity result = mapper.mapNotificationCostUpdater(dto);

        assertThat(result.getIun()).isEqualTo(IUN);
        assertThat(result.getRecIndex()).isEqualTo(REC_INDEX);
        assertThat(result.getSimpleRegisteredLetterCost()).isNotNull();
        assertThat(result.getSimpleRegisteredLetterCost().getCost()).isEqualTo(COST);
        assertThat(result.getSimpleRegisteredLetterCost().getProductType()).isEqualTo(PRODUCT_TYPE);
        assertThat(result.getFirstAnalogCost()).isNull();
        assertThat(result.getSecondAnalogCost()).isNull();
        assertThat(result.getIsDeleted()).isNull();
    }

    @Test
    void mapNotificationCostUpdater_shouldMapSendAnalogDomicileAttemptZero() {
        NotificationCostUpdate dto = buildUpdate(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0);

        NotificationDeliveryCostEntity result = mapper.mapNotificationCostUpdater(dto);

        assertThat(result.getIun()).isEqualTo(IUN);
        assertThat(result.getRecIndex()).isEqualTo(REC_INDEX);
        assertThat(result.getFirstAnalogCost()).isNotNull();
        assertThat(result.getFirstAnalogCost().getCost()).isEqualTo(COST);
        assertThat(result.getFirstAnalogCost().getProductType()).isEqualTo(PRODUCT_TYPE);
        assertThat(result.getSecondAnalogCost()).isNull();
        assertThat(result.getSimpleRegisteredLetterCost()).isNull();
        assertThat(result.getIsDeleted()).isNull();

    }

    @Test
    void mapNotificationCostUpdater_shouldMapSendAnalogDomicileAttemptOne() {
        NotificationCostUpdate dto = buildUpdate(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_1);

        NotificationDeliveryCostEntity result = mapper.mapNotificationCostUpdater(dto);

        assertThat(result.getIun()).isEqualTo(IUN);
        assertThat(result.getRecIndex()).isEqualTo(REC_INDEX);
        assertThat(result.getSecondAnalogCost()).isNotNull();
        assertThat(result.getSecondAnalogCost().getCost()).isEqualTo(COST);
        assertThat(result.getSecondAnalogCost().getProductType()).isEqualTo(PRODUCT_TYPE);
        assertThat(result.getFirstAnalogCost()).isNull();
        assertThat(result.getSimpleRegisteredLetterCost()).isNull();
        assertThat(result.getIsDeleted()).isNull();
    }

    @Test
    void mapNotificationCostUpdater_shouldMapRequestRefused() {
        NotificationCostUpdate dto = buildUpdate(CostUpdatePhaseInt.REQUEST_REFUSED);

        NotificationDeliveryCostEntity result = mapper.mapNotificationCostUpdater(dto);

        assertThat(result.getIun()).isEqualTo(IUN);
        assertThat(result.getRecIndex()).isEqualTo(REC_INDEX);
        assertThat(result.getFirstAnalogCost()).isNotNull();
        assertThat(result.getFirstAnalogCost().getCost()).isZero();
        assertThat(result.getFirstAnalogCost().getProductType()).isNull();
        assertThat(result.getSecondAnalogCost()).isNotNull();
        assertThat(result.getSecondAnalogCost().getCost()).isZero();
        assertThat(result.getSecondAnalogCost().getProductType()).isNull();
        assertThat(result.getIsDeleted()).isTrue();
        assertThat(result.getSimpleRegisteredLetterCost()).isNotNull();
        assertThat(result.getSimpleRegisteredLetterCost().getCost()).isZero();
        assertThat(result.getSimpleRegisteredLetterCost().getProductType()).isNull();
        assertThat(result.getBaseCost()).isNotNull();
        assertThat(result.getBaseCost().getPaFee()).isZero();
        assertThat(result.getBaseCost().getSendFee()).isZero();
    }

    @Test
    void mapNotificationCostUpdater_shouldMapNotificationCancelled() {
        NotificationCostUpdate dto = buildUpdate(CostUpdatePhaseInt.NOTIFICATION_CANCELLED);

        NotificationDeliveryCostEntity result = mapper.mapNotificationCostUpdater(dto);

        assertThat(result.getIun()).isEqualTo(IUN);
        assertThat(result.getRecIndex()).isEqualTo(REC_INDEX);
        assertThat(result.getFirstAnalogCost()).isNotNull();
        assertThat(result.getFirstAnalogCost().getCost()).isZero();
        assertThat(result.getFirstAnalogCost().getProductType()).isNull();
        assertThat(result.getSecondAnalogCost()).isNotNull();
        assertThat(result.getSecondAnalogCost().getCost()).isZero();
        assertThat(result.getSecondAnalogCost().getProductType()).isNull();
        assertThat(result.getIsDeleted()).isTrue();
        assertThat(result.getSimpleRegisteredLetterCost()).isNotNull();
        assertThat(result.getSimpleRegisteredLetterCost().getCost()).isZero();
        assertThat(result.getSimpleRegisteredLetterCost().getProductType()).isNull();
        assertThat(result.getSecondAnalogCost()).isNotNull();
        assertThat(result.getSecondAnalogCost().getCost()).isZero();
        assertThat(result.getSecondAnalogCost().getProductType()).isNull();
        assertThat(result.getBaseCost()).isNotNull();
        assertThat(result.getBaseCost().getPaFee()).isZero();
        assertThat(result.getBaseCost().getSendFee()).isZero();
    }

    @Test
    void mapNotificationCostUpdater_shouldThrowForValidationPhase() {
        NotificationCostUpdate dto = buildUpdate(CostUpdatePhaseInt.VALIDATION);

        assertThatThrownBy(() -> mapper.mapNotificationCostUpdater(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown cost update phase: VALIDATION");
    }

    private NotificationCostUpdate buildUpdate(CostUpdatePhaseInt phase) {
        return NotificationCostUpdate.builder()
                .iun(IUN)
                .recIndex(REC_INDEX)
                .cost(COST)
                .productType(PRODUCT_TYPE)
                .costUpdatePhase(phase)
                .build();
    }
}
