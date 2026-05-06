package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.updatenotification.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@AllArgsConstructor
public class UpdateNotificationCostMapper {

    public UpdateNotificationCostRequestInt fromRequestDtoToInt(UpdateNotificationCostRequestDto dto) {
        Objects.requireNonNull(dto, "UpdateNotificationCostRequestDto must not be null");

        return UpdateNotificationCostRequestInt.builder()
                .notificationStepCost(Objects.requireNonNull(dto.getNotificationStepCost(), "notificationStepCost must not be null"))
                .eventTimestamp(Objects.requireNonNull(dto.getEventTimestamp(), "eventTimestamp must not be null"))
                .eventStorageTimestamp(Objects.requireNonNull(dto.getEventStorageTimestamp(), "eventStorageTimestamp must not be null"))
                .notificationSentAt(Objects.requireNonNull(dto.getNotificationSentAt(), "notificationSentAt must not be null"))
                .updateCostPhase(mapUpdateCostPhase(Objects.requireNonNull(dto.getUpdateCostPhase(), "updateCostPhase must not be null")))
                .build();
    }

    private CostUpdatePhaseInt mapUpdateCostPhase(UpdateCostPhaseDto dto) {
        Objects.requireNonNull(dto,"updateCostPhase must not be null");

        return switch (dto) {
            case VALIDATION -> CostUpdatePhaseInt.VALIDATION;
            case SEND_ANALOG_DOMICILE_ATTEMPT_0 -> CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0;
            case SEND_ANALOG_DOMICILE_ATTEMPT_1 -> CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_1;
            case SEND_SIMPLE_REGISTERED_LETTER -> CostUpdatePhaseInt.SEND_SIMPLE_REGISTERED_LETTER;
            case REQUEST_REFUSED -> CostUpdatePhaseInt.REQUEST_REFUSED;
            case NOTIFICATION_CANCELLED -> CostUpdatePhaseInt.NOTIFICATION_CANCELLED;
        };
    }

    public UpdateNotificationCostResponseDto fromIntToDtoResponse(UpdateNotificationCostResponseInt responseInt) {
        Objects.requireNonNull(responseInt, "UpdateNotificationCostResponseInt must not be null");

        List<UpdateNotificationCostResultDto> resultDtoList = Optional.ofNullable(responseInt.getUpdateNotificationCostResultIntList())
                .orElse(List.of())
                .stream()
                .map(this::mapResultIntToDto)
                .toList();

        return new UpdateNotificationCostResponseDto(resultDtoList);
    }

    private UpdateNotificationCostResultDto mapResultIntToDto(UpdateNotificationCostResultInt resultInt) {
        Objects.requireNonNull(resultInt, "UpdateNotificationCostResultInt must not be null");

        String iuv = Objects.requireNonNull(resultInt.getIuv(), "iuv must not be null");
        CommunicationResultGroupInt result = Objects.requireNonNull(resultInt.getResult(), "result must not be null");

        return new UpdateNotificationCostResultDto(
                iuv,
                resultInt.getRecIndex(),
                mapCommunicationResultGroup(result)
        );
    }

    private ResultEnumDto mapCommunicationResultGroup(CommunicationResultGroupInt result) {
        return switch (result) {
            case OK -> ResultEnumDto.OK;
            case KO -> ResultEnumDto.KO;
            case RETRY -> ResultEnumDto.RETRY;
        };
    }
}