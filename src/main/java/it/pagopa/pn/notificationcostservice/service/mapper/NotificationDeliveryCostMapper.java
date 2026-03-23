package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.model.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.BaseCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost.AnalogCost;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class NotificationDeliveryCostMapper {

    public List<NotificationDeliveryCost> mapDtoToNotificationDeliveryCost(String iun, NewNotificationCostRequestDto dto) {
        return dto.getCostRecipients().stream()
                .map(recipient -> mapRecipientToNotificationDeliveryCost(iun, recipient))
                .toList();
    }

    private NotificationDeliveryCost mapRecipientToNotificationDeliveryCost(String iun, RecipientCostDataDto recipient) {

        return NotificationDeliveryCost.builder()
                .iun(iun)
                .recipientInternalId(recipient.getRecipientInternalId())
                .senderInternalId(recipient.getSenderInternalId())
                .recIndex(recipient.getRecIndex())
                .vat(recipient.getVat())
                .pagoPaIntMode(PagoPaIntMode.valueOf(recipient.getPagoPaIntMode().name()))
                .notificationFeePolicy(NotificationFeePolicy.valueOf(recipient.getNotificationFeePolicy().name()))
                .baseCost(BaseCost.builder()
                        .sendFee(recipient.getSendFee())
                        .paFee(recipient.getPaFee())
                        .build())
                .build();
    }
    public NotificationCostRecipientResponseDto mapDtoToResponse(NotificationDeliveryCost dto, CalculatedCosts calculatedCosts) {
        return new NotificationCostRecipientResponseDto()
                .lastUpdate(dto.getLastUpdate())
                .pagoPaIntMode(PagoPaIntModeDto.fromValue(dto.getPagoPaIntMode().name()))
                .totalCost(new TotalCostDto()
                        .costWithVat(calculatedCosts.getTotalCostWithVat())
                        .details(mapTotalCostDetails(dto, calculatedCosts)));
    }

    private TotalCostDetailsDto mapTotalCostDetails(NotificationDeliveryCost dto, CalculatedCosts calculatedCosts) {
        return new TotalCostDetailsDto()
                .notificationFeePolicy(NotificationFeePolicyDto.fromValue(dto.getNotificationFeePolicy().name()))
                .baseCostDetail(new BaseCostDetailDto()
                        .cost(calculatedCosts.getBaseCost())
                        .baseCostComponents(List.of(
                                new BaseCostComponentDto().costName(BaseCostNameDto.SEND_FEE).cost(dto.getBaseCost().getSendFee()),
                                new BaseCostComponentDto().costName(BaseCostNameDto.PA_FEE).cost(dto.getBaseCost().getPaFee())
                        )))
                .analogCostDetail(mapAnalogCostDetail(dto, calculatedCosts));
    }

    private AnalogCostDetailDto mapAnalogCostDetail(NotificationDeliveryCost dto, CalculatedCosts calculatedCosts) {
        List<AnalogCostComponentDto> components = mapAnalogComponents(dto);

        if (components.isEmpty()) return null;

        return new AnalogCostDetailDto()
                .costWithVat(calculatedCosts.getAnalogCostWithVat())
                .vat(dto.getVat())
                .analogCostComponents(components);
    }

    private List<AnalogCostComponentDto> mapAnalogComponents(NotificationDeliveryCost dto) {
        List<AnalogCostComponentDto> components = new ArrayList<>();

        Optional<AnalogCostComponentDto> firstAnalogCost = Optional.ofNullable(dto.getSimpleRegisteredLetterCost())
                .map(c -> toAnalogComponent(c, AnalogCostNameDto.FIRST_ATTEMPT))
                .or(() -> Optional.ofNullable(dto.getFirstAnalogCost())
                        .map(c -> toAnalogComponent(c, AnalogCostNameDto.FIRST_ATTEMPT)));

        firstAnalogCost.ifPresent(components::add);

        // Gestione secondo tentativo
        Optional.ofNullable(dto.getSecondAnalogCost())
                .ifPresent(c -> components.add(toAnalogComponent(c, AnalogCostNameDto.SECOND_ATTEMPT)));

        return components;
    }

    private AnalogCostComponentDto toAnalogComponent(AnalogCost dto, AnalogCostNameDto costName) {
        return new AnalogCostComponentDto()
                .cost(dto.getCost())
                .costName(costName)
                .productType(dto.getProductType());
    }
}
