package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notificationcostservice.NotificationDeliveryCostTestBuilder;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.FirstAnalogCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.SecondAnalogCost;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCostUpdaterMapperTest {

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
}
