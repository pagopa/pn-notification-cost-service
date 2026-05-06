package it.pagopa.pn.notificationcostservice.service.mapper;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.ResultEnumDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.UpdateCostPhaseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.UpdateNotificationCostRequestDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.UpdateNotificationCostResponseDto;
import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.UpdateNotificationCostResultDto;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.updatenotification.CommunicationResultGroupInt;
import it.pagopa.pn.notificationcostservice.model.updatenotification.UpdateNotificationCostRequestInt;
import it.pagopa.pn.notificationcostservice.model.updatenotification.UpdateNotificationCostResponseInt;
import it.pagopa.pn.notificationcostservice.model.updatenotification.UpdateNotificationCostResultInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpdateNotificationCostMapperTest {

    private UpdateNotificationCostMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UpdateNotificationCostMapper();
    }

    @Test
    void fromRequestDtoToInt_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> mapper.fromRequestDtoToInt(null));
    }

    @Test
    void fromRequestDtoToInt_validRequestRefused_mapsCorrectly() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                100,
                now,
                now.plusSeconds(1),
                now.minusSeconds(10),
                UpdateCostPhaseDto.REQUEST_REFUSED
        );

        UpdateNotificationCostRequestInt result = mapper.fromRequestDtoToInt(dto);

        assertNotNull(result);
        assertEquals(100, result.getNotificationStepCost());
        assertEquals(now, result.getEventTimestamp());
        assertEquals(now.plusSeconds(1), result.getEventStorageTimestamp());
        assertEquals(now.minusSeconds(10), result.getNotificationSentAt());
        assertEquals(CostUpdatePhaseInt.REQUEST_REFUSED, result.getUpdateCostPhase());
    }

    @Test
    void fromRequestDtoToInt_validNotificationCancelled_mapsCorrectly() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                50,
                now,
                now,
                now,
                UpdateCostPhaseDto.NOTIFICATION_CANCELLED
        );

        UpdateNotificationCostRequestInt result = mapper.fromRequestDtoToInt(dto);

        assertNotNull(result);
        assertEquals(CostUpdatePhaseInt.NOTIFICATION_CANCELLED, result.getUpdateCostPhase());
    }

    @Test
    void fromRequestDtoToInt_validValidation_mapsCorrectly() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                10,
                now,
                now,
                now,
                UpdateCostPhaseDto.VALIDATION
        );

        UpdateNotificationCostRequestInt result = mapper.fromRequestDtoToInt(dto);

        assertNotNull(result);
        assertEquals(CostUpdatePhaseInt.VALIDATION, result.getUpdateCostPhase());
    }

    @Test
    void fromRequestDtoToInt_validCancellation_mapsCorrectly() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                10,
                now,
                now,
                now,
                UpdateCostPhaseDto.NOTIFICATION_CANCELLED
        );

        UpdateNotificationCostRequestInt result = mapper.fromRequestDtoToInt(dto);

        assertNotNull(result);
        assertEquals(CostUpdatePhaseInt.NOTIFICATION_CANCELLED, result.getUpdateCostPhase());
    }

    @Test
    void fromRequestDtoToInt_validFirstAnalog_mapsCorrectly() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                10,
                now,
                now,
                now,
                UpdateCostPhaseDto.SEND_ANALOG_DOMICILE_ATTEMPT_0
        );

        UpdateNotificationCostRequestInt result = mapper.fromRequestDtoToInt(dto);

        assertNotNull(result);
        assertEquals(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_0, result.getUpdateCostPhase());
    }

    @Test
    void fromRequestDtoToInt_validSecondAnalog_mapsCorrectly() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                10,
                now,
                now,
                now,
                UpdateCostPhaseDto.SEND_ANALOG_DOMICILE_ATTEMPT_1
        );

        UpdateNotificationCostRequestInt result = mapper.fromRequestDtoToInt(dto);

        assertNotNull(result);
        assertEquals(CostUpdatePhaseInt.SEND_ANALOG_DOMICILE_ATTEMPT_1, result.getUpdateCostPhase());
    }

    @Test
    void fromRequestDtoToInt_validSimpleRegisteredLetter_mapsCorrectly() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                10,
                now,
                now,
                now,
                UpdateCostPhaseDto.SEND_SIMPLE_REGISTERED_LETTER
        );

        UpdateNotificationCostRequestInt result = mapper.fromRequestDtoToInt(dto);

        assertNotNull(result);
        assertEquals(CostUpdatePhaseInt.SEND_SIMPLE_REGISTERED_LETTER, result.getUpdateCostPhase());
    }

    @Test
    void fromRequestDtoToInt_nullUpdateCostPhase_throwsNullPointerException() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                50,
                now,
                now,
                now,
                null
        );

        assertThrows(NullPointerException.class, () -> mapper.fromRequestDtoToInt(dto));
    }

    @Test
    void fromRequestDtoToInt_nullNotificationStepCost_throwsNullPointerException() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                null,
                now,
                now,
                now,
                UpdateCostPhaseDto.REQUEST_REFUSED
        );

        assertThrows(NullPointerException.class, () -> mapper.fromRequestDtoToInt(dto));
    }

    @Test
    void fromRequestDtoToInt_nullEventTimestamp_throwsNullPointerException() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                100,
                null,
                now,
                now,
                UpdateCostPhaseDto.REQUEST_REFUSED
        );

        assertThrows(NullPointerException.class, () -> mapper.fromRequestDtoToInt(dto));
    }

    @Test
    void fromRequestDtoToInt_nullEventStorageTimestamp_throwsNullPointerException() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                100,
                now,
                null,
                now,
                UpdateCostPhaseDto.REQUEST_REFUSED
        );

        assertThrows(NullPointerException.class, () -> mapper.fromRequestDtoToInt(dto));
    }

    @Test
    void fromRequestDtoToInt_nullNotificationSentAt_throwsNullPointerException() {
        Instant now = Instant.now();

        UpdateNotificationCostRequestDto dto = new UpdateNotificationCostRequestDto(
                100,
                now,
                now,
                null,
                UpdateCostPhaseDto.REQUEST_REFUSED
        );

        assertThrows(NullPointerException.class, () -> mapper.fromRequestDtoToInt(dto));
    }

    @Test
    void fromIntToDtoResponse_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> mapper.fromIntToDtoResponse(null));
    }

    @Test
    void fromIntToDtoResponse_nullList_returnsEmptyList() {
        UpdateNotificationCostResponseInt responseInt = new UpdateNotificationCostResponseInt(null);

        UpdateNotificationCostResponseDto result = mapper.fromIntToDtoResponse(responseInt);

        assertNotNull(result);
        assertNotNull(result.getUpdateResults());
        assertTrue(result.getUpdateResults().isEmpty());
    }

    @Test
    void fromIntToDtoResponse_validSingleResult_mapsCorrectly() {
        UpdateNotificationCostResultInt resultInt = new UpdateNotificationCostResultInt();
        resultInt.setIuv("77777777777##362981250372662851");
        resultInt.setRecIndex(0);
        resultInt.setResult(CommunicationResultGroupInt.OK);

        UpdateNotificationCostResponseInt responseInt = new UpdateNotificationCostResponseInt(List.of(resultInt));

        UpdateNotificationCostResponseDto result = mapper.fromIntToDtoResponse(responseInt);

        assertNotNull(result);
        assertEquals(1, result.getUpdateResults().size());

        UpdateNotificationCostResultDto dto = result.getUpdateResults().getFirst();
        assertEquals("77777777777##362981250372662851", dto.getIuv());
        assertEquals(0, dto.getRecIndex());
        assertEquals(ResultEnumDto.OK, dto.getResult());
    }

    @Test
    void fromIntToDtoResponse_multipleResults_mapsAllStatuses() {
        UpdateNotificationCostResultInt r1 = new UpdateNotificationCostResultInt();
        r1.setIuv("77777777777##362981250372662852");
        r1.setRecIndex(0);
        r1.setResult(CommunicationResultGroupInt.OK);

        UpdateNotificationCostResultInt r2 = new UpdateNotificationCostResultInt();
        r2.setIuv("77777777777##362981250372662851");
        r2.setRecIndex(1);
        r2.setResult(CommunicationResultGroupInt.KO);

        UpdateNotificationCostResultInt r3 = new UpdateNotificationCostResultInt();
        r3.setIuv("77777777777##362981250372662853");
        r3.setRecIndex(2);
        r3.setResult(CommunicationResultGroupInt.RETRY);

        UpdateNotificationCostResponseInt responseInt = new UpdateNotificationCostResponseInt(List.of(r1, r2, r3));

        UpdateNotificationCostResponseDto result = mapper.fromIntToDtoResponse(responseInt);

        assertNotNull(result);
        assertEquals(3, result.getUpdateResults().size());
        assertEquals(ResultEnumDto.OK, result.getUpdateResults().get(0).getResult());
        assertEquals(ResultEnumDto.KO, result.getUpdateResults().get(1).getResult());
        assertEquals(ResultEnumDto.RETRY, result.getUpdateResults().get(2).getResult());
    }

    @Test
    void fromIntToDtoResponse_resultWithNullResult_throwsNullPointerException() {
        UpdateNotificationCostResultInt resultInt = new UpdateNotificationCostResultInt();
        resultInt.setIuv("77777777777##362981250372662851");
        resultInt.setRecIndex(0);
        resultInt.setResult(null);

        UpdateNotificationCostResponseInt responseInt = new UpdateNotificationCostResponseInt(List.of(resultInt));

        assertThrows(NullPointerException.class, () -> mapper.fromIntToDtoResponse(responseInt));
    }

    @Test
    void fromIntToDtoResponse_resultWithNullIuv_throwsNullPointerException() {
        UpdateNotificationCostResultInt resultInt = new UpdateNotificationCostResultInt();
        resultInt.setIuv(null);
        resultInt.setRecIndex(0);
        resultInt.setResult(CommunicationResultGroupInt.OK);

        UpdateNotificationCostResponseInt responseInt = new UpdateNotificationCostResponseInt(List.of(resultInt));

        assertThrows(NullPointerException.class, () -> mapper.fromIntToDtoResponse(responseInt));
    }
}