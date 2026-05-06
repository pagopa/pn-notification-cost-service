package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.notificationcostservice.model.updatenotification.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
public class UpdateNotificationCostMapper {

    public UpdateNotificationCostRequestInt fromRequestDtoToInt(UpdateNotificationCostRequestDto dto) {
        if (dto == null) return null;

        return UpdateNotificationCostRequestInt.builder()
                .notificationStepCost(dto.getNotificationStepCost())
                .eventTimestamp(dto.getEventTimestamp())
                .eventStorageTimestamp(dto.getEventStorageTimestamp())
                .notificationSentAt(dto.getNotificationSentAt())
                .updateCostPhase(mapUpdateCostPhase(dto.getUpdateCostPhase()))
                .build();
    }

    private UpdateCostPhaseInt mapUpdateCostPhase(UpdateCostPhaseDto dto) {
        if (dto == null) return null;
        return switch (dto) {
            case REFUSED -> UpdateCostPhaseInt.REFUSED;
            case CANCELLED -> UpdateCostPhaseInt.CANCELLED;
        };
    }

    public UpdateNotificationCostResponseDto fromIntToDtoResponse(UpdateNotificationCostResponseInt responseInt) {
        if (responseInt == null) return null;

        List<UpdateNotificationCostResultDto> resultDtoList = Optional.ofNullable(responseInt.getUpdateNotificationCostResultIntList())
                .orElse(List.of())
                .stream()
                .map(this::mapResultIntToDto)
                .toList();

        return new UpdateNotificationCostResponseDto(resultDtoList);
    }

    private UpdateNotificationCostResultDto mapResultIntToDto(UpdateNotificationCostResultInt resultInt) {
        if (resultInt == null) return null;

        return new UpdateNotificationCostResultDto(
                resultInt.getIuv(),
                resultInt.getRecIndex(),
                mapCommunicationResultGroup(resultInt.getResult())
        );
    }

    private ResultEnumDto mapCommunicationResultGroup(CommunicationResultGroupInt result) {
        if (result == null) return null;
        return switch (result) {
            case OK -> ResultEnumDto.OK;
            case KO -> ResultEnumDto.KO;
            case RETRY -> ResultEnumDto.RETRY;
        };
    }
}