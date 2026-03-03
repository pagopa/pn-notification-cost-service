package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.dto.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.AnalogCostDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class NotificationDeliveryCostMapper {

    public NotificationCostRecipientResponse mapDtoToResponse(NotificationDeliveryCostDto dto, CalculatedCosts calculatedCosts) {
        return new NotificationCostRecipientResponse()
                .lastUpdate(dto.getLastUpdate())
                .pagoPaIntMode(mapPagoPaMode(dto.getPagoPaIntMode()))
                .totalCost(new TotalCost()
                        .costWithVat(calculatedCosts.getTotalCostWithVat())
                        .details(mapTotalCostDetails(dto, calculatedCosts)));
    }

    private TotalCostDetails mapTotalCostDetails(NotificationDeliveryCostDto dto, CalculatedCosts calculatedCosts) {
        return new TotalCostDetails()
                .notificationFeePolicy(mapFeePolicy(dto.getNotificationFeePolicy()))
                .baseCostDetail(new BaseCostDetail()
                        .cost(calculatedCosts.getBaseCost())
                        .baseCostComponents(List.of(
                                new BaseCostComponent().costName(BaseCostName.SEND_FEE).cost(dto.getBaseCost().getSendFee()),
                                new BaseCostComponent().costName(BaseCostName.PA_FEE).cost(dto.getBaseCost().getPaFee())
                        )))
                .analogCostDetail(mapAnalogCostDetail(dto, calculatedCosts));
    }

    private AnalogCostDetail mapAnalogCostDetail(NotificationDeliveryCostDto dto, CalculatedCosts calculatedCosts) {
        List<AnalogCostComponent> components = mapAnalogComponents(dto);

        if (components.isEmpty()) return null;

        return new AnalogCostDetail()
                .costWithVat(calculatedCosts.getAnalogCostWithVat())
                .vat(dto.getVat())
                .analogCostComponents(components);
    }

    private List<AnalogCostComponent> mapAnalogComponents(NotificationDeliveryCostDto dto) {
        List<AnalogCostComponent> components = new ArrayList<>();

        // Gestione primo tentativo o raccomandata semplice
        Optional<AnalogCostComponent> firstAnalogCost = Optional.ofNullable(dto.getSimpleRegisteredLetterCost())
                .map(c -> toAnalogComponent(c, AnalogCostName.FIRST_ATTEMPT))
                .or(() -> Optional.ofNullable(dto.getFirstAnalogCost())
                        .map(c -> toAnalogComponent(c, AnalogCostName.FIRST_ATTEMPT)));

        firstAnalogCost.ifPresent(components::add);

        // Gestione secondo tentativo
        Optional.ofNullable(dto.getSecondAnalogCost())
                .ifPresent(c -> components.add(toAnalogComponent(c, AnalogCostName.SECOND_ATTEMPT)));

        return components;
    }

    private AnalogCostComponent toAnalogComponent(AnalogCostDto dto, AnalogCostName costName) {
        return new AnalogCostComponent()
                .cost(dto.getCost())
                .costName(costName)
                .productType(dto.getProductType());
    }

    private PagoPaIntMode mapPagoPaMode(Enum<?> source) {
        return source != null ? PagoPaIntMode.fromValue(source.name()) : null;
    }

    private NotificationFeePolicy mapFeePolicy(Enum<?> source) {
        return source != null ? NotificationFeePolicy.fromValue(source.name()) : null;
    }
}
