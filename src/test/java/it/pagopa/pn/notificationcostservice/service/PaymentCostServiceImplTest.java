package it.pagopa.pn.notificationcostservice.service;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.PagoPaIntMode;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotificationDeliveryCostBadRequestException;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.service.impl.PaymentCostServiceImpl;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCostServiceImplTest {

    @Mock
    private NotificationDeliveryCostDao notificationDeliveryCostDao;

    @Mock
    private NotificationDeliveryCostMapper notificationDeliveryCostMapper;

    @InjectMocks
    private PaymentCostServiceImpl paymentCostService;

    private static final String IUN = "TEST-IUN-12345";
    private static final Integer REC_INDEX = 0;
    private static final Integer VAT = 22;

    private NotificationDeliveryCostDto notificationDeliveryCostDto;
    private NotificationCostRecipientResponseDto expectedResponse;

    @BeforeEach
    void setUp() {
        notificationDeliveryCostDto = NotificationDeliveryCostDto.builder()
                .iun(IUN)
                .recIndex(REC_INDEX)
                .baseCost(BaseCostDto.builder()
                        .sendFee(50)
                        .paFee(50)
                        .build())
                .firstAnalogCost(FirstAnalogCostDto.builder()
                        .cost(200)
                        .build())
                .secondAnalogCost(SecondAnalogCostDto.builder()
                        .cost(150)
                        .build())
                .simpleRegisteredLetterCost(SimpleRegisteredLetterCostDto.builder()
                        .cost(50)
                        .build())
                .vat(VAT)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .build();

        expectedResponse = NotificationCostRecipientResponseDto.builder()
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .build();
    }

    @Test
    void getNotificationCostRecipient_Success_WithDeliveryMode() {
        // Given
        when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX))
                .thenReturn(Mono.just(notificationDeliveryCostDto));
        when(notificationDeliveryCostMapper.mapDtoToResponseDto(any(NotificationDeliveryCostDto.class), anyInt()))
                .thenReturn(expectedResponse);

        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient(IUN, REC_INDEX);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(notificationDeliveryCostDao, times(1)).getNotificationDeliveryCostItem(IUN, REC_INDEX);
        verify(notificationDeliveryCostMapper, times(1)).mapDtoToResponseDto(eq(notificationDeliveryCostDto), anyInt());
    }

    @Test
    void getNotificationCostRecipient_Success_WithFlatRate() {
        // Given
        NotificationDeliveryCostDto flatRateDto = notificationDeliveryCostDto.toBuilder()
                .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
                .build();

        when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX))
                .thenReturn(Mono.just(flatRateDto));
        when(notificationDeliveryCostMapper.mapDtoToResponseDto(any(NotificationDeliveryCostDto.class), eq(0)))
                .thenReturn(expectedResponse);

        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient(IUN, REC_INDEX);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(notificationDeliveryCostDao, times(1)).getNotificationDeliveryCostItem(IUN, REC_INDEX);
        verify(notificationDeliveryCostMapper, times(1)).mapDtoToResponseDto(eq(flatRateDto), eq(0));
    }

    @Test
    void getNotificationCostRecipient_Success_WithNullAnalogCosts() {
        // Given
        NotificationDeliveryCostDto dtoWithNullCosts = notificationDeliveryCostDto.toBuilder()
                .firstAnalogCost(null)
                .secondAnalogCost(null)
                .simpleRegisteredLetterCost(null)
                .build();

        when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX))
                .thenReturn(Mono.just(dtoWithNullCosts));

        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient(IUN, REC_INDEX);

        // Then
        StepVerifier.create(result)
                .expectError(NullPointerException.class)
                .verify();

        verify(notificationDeliveryCostDao, times(1)).getNotificationDeliveryCostItem(IUN, REC_INDEX);
        verify(notificationDeliveryCostMapper, never()).mapDtoToResponseDto(any(), anyInt());
    }

    @Test
    void getNotificationCostRecipient_Error_WhenIunIsNull() {
        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient(null, REC_INDEX);

        // Then
        StepVerifier.create(result)
                .expectError(PnNotificationDeliveryCostBadRequestException.class)
                .verify();

        verify(notificationDeliveryCostDao, never()).getNotificationDeliveryCostItem(anyString(), anyInt());
        verify(notificationDeliveryCostMapper, never()).mapDtoToResponseDto(any(), anyInt());
    }

    @Test
    void getNotificationCostRecipient_Error_WhenIunIsEmpty() {
        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient("", REC_INDEX);

        // Then
        StepVerifier.create(result)
                .expectError(PnNotificationDeliveryCostBadRequestException.class)
                .verify();

        verify(notificationDeliveryCostDao, never()).getNotificationDeliveryCostItem(anyString(), anyInt());
        verify(notificationDeliveryCostMapper, never()).mapDtoToResponseDto(any(), anyInt());
    }

    @Test
    void getNotificationCostRecipient_Error_WhenIunIsBlank() {
        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient("   ", REC_INDEX);

        // Then
        StepVerifier.create(result)
                .expectError(PnNotificationDeliveryCostBadRequestException.class)
                .verify();

        verify(notificationDeliveryCostDao, never()).getNotificationDeliveryCostItem(anyString(), anyInt());
        verify(notificationDeliveryCostMapper, never()).mapDtoToResponseDto(any(), anyInt());
    }

    @Test
    void getNotificationCostRecipient_Error_WhenRecIndexIsNull() {
        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient(IUN, null);

        // Then
        StepVerifier.create(result)
                .expectError(PnNotificationDeliveryCostBadRequestException.class)
                .verify();

        verify(notificationDeliveryCostDao, never()).getNotificationDeliveryCostItem(anyString(), anyInt());
        verify(notificationDeliveryCostMapper, never()).mapDtoToResponseDto(any(), anyInt());
    }

    @Test
    void getNotificationCostRecipient_Error_WhenBothIunAndRecIndexAreNull() {
        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient(null, null);

        // Then
        StepVerifier.create(result)
                .expectError(PnNotificationDeliveryCostBadRequestException.class)
                .verify();

        verify(notificationDeliveryCostDao, never()).getNotificationDeliveryCostItem(anyString(), anyInt());
        verify(notificationDeliveryCostMapper, never()).mapDtoToResponseDto(any(), anyInt());
    }

    @Test
    void getNotificationCostRecipient_Success_WithZeroCosts() {
        // Given
        NotificationDeliveryCostDto dtoWithZeroCosts = notificationDeliveryCostDto.toBuilder()
                .baseCost(BaseCostDto.builder()
                        .sendFee(0)
                        .paFee(0)
                        .build())
                .firstAnalogCost(FirstAnalogCostDto.builder()
                        .cost(0)
                        .build())
                .secondAnalogCost(SecondAnalogCostDto.builder()
                        .cost(0)
                        .build())
                .simpleRegisteredLetterCost(SimpleRegisteredLetterCostDto.builder()
                        .cost(0)
                        .build())
                .build();

        when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX))
                .thenReturn(Mono.just(dtoWithZeroCosts));
        when(notificationDeliveryCostMapper.mapDtoToResponseDto(any(NotificationDeliveryCostDto.class), eq(0)))
                .thenReturn(expectedResponse);

        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient(IUN, REC_INDEX);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(notificationDeliveryCostDao, times(1)).getNotificationDeliveryCostItem(IUN, REC_INDEX);
        verify(notificationDeliveryCostMapper, times(1)).mapDtoToResponseDto(eq(dtoWithZeroCosts), eq(0));
    }

    @Test
    void getNotificationCostRecipient_Error_WhenIsDeletedTrue() {
        // Given
        NotificationDeliveryCostDto deletedDto = notificationDeliveryCostDto.toBuilder()
                .isDeleted(true)
                .build();

        when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX))
                .thenReturn(Mono.just(deletedDto));

        // When
        Mono<NotificationCostRecipientResponseDto> result = paymentCostService.getNotificationCostRecipient(IUN, REC_INDEX);

        // Then
        StepVerifier.create(result)
                .expectError(PnNotFoundException.class)
                .verify();

        verify(notificationDeliveryCostDao, times(1)).getNotificationDeliveryCostItem(IUN, REC_INDEX);
        verify(notificationDeliveryCostMapper, never()).mapDtoToResponseDto(any(), anyInt());
    }
}