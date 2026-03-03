package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponse;
import it.pagopa.pn.notificationcostservice.dto.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationDeliveryCostDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.service.CostCalculator;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCostServiceImplTest {

    @Mock
    private NotificationDeliveryCostDao notificationDeliveryCostDao;

    @Mock
    private NotificationDeliveryCostMapper notificationDeliveryCostMapper;

    @Mock
    private CostCalculator costCalculator;

    @InjectMocks
    private NotificationCostServiceImpl notificationCostService;

    private static final String IUN = "TEST-IUN-12345";
    private static final Integer REC_INDEX = 0;

    @Test
    void getNotificationCostRecipient_ReturnsMappedResponseWhenItemExistsAndNotDeleted() {
       NotificationDeliveryCostDto dto = new NotificationDeliveryCostDto();
       dto.setIun(IUN);
       dto.setRecIndex(REC_INDEX);
       dto.setIsDeleted(false);


       when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX)).thenReturn(Mono.just(dto));

       CalculatedCosts calculatedCosts = CalculatedCosts.builder().build();
       when(costCalculator.calculateCosts(dto)).thenReturn(calculatedCosts);

       NotificationCostRecipientResponse mappedResponse = new NotificationCostRecipientResponse();
       when(notificationDeliveryCostMapper.mapDtoToResponse(eq(dto), any())).thenReturn(mappedResponse);

       StepVerifier.create(notificationCostService.getNotificationCostRecipient(IUN, REC_INDEX))
               .expectNext(mappedResponse)
               .verifyComplete();

       verify(notificationDeliveryCostDao).getNotificationDeliveryCostItem(IUN, REC_INDEX);
       verify(costCalculator).calculateCosts(dto);
       verify(notificationDeliveryCostMapper).mapDtoToResponse(eq(dto), any());
    }

    @Test
    void getNotificationCostRecipient_ReturnsNotFoundErrorWhenItemIsDeleted() {
       NotificationDeliveryCostDto dto = new NotificationDeliveryCostDto();
       dto.setIun(IUN);
       dto.setRecIndex(REC_INDEX);
       dto.setIsDeleted(true);

       when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX)).thenReturn(Mono.just(dto));

       StepVerifier.create(notificationCostService.getNotificationCostRecipient(IUN, REC_INDEX))
               .expectError(PnNotFoundException.class)
               .verify();

       verify(notificationDeliveryCostDao).getNotificationDeliveryCostItem(IUN, REC_INDEX);
       verifyNoInteractions(costCalculator);
       verifyNoInteractions(notificationDeliveryCostMapper);

    }

    @Test
    void getNotificationCostRecipient_PropagatesErrorWhenDaoReturnsError() {
       when(notificationDeliveryCostDao.getNotificationDeliveryCostItem(IUN, REC_INDEX))
               .thenReturn(Mono.error(new RuntimeException("DB error")));

       StepVerifier.create(notificationCostService.getNotificationCostRecipient(IUN, REC_INDEX))
               .expectError(RuntimeException.class)
               .verify();

       verify(notificationDeliveryCostDao).getNotificationDeliveryCostItem(IUN, REC_INDEX);
       verifyNoInteractions(costCalculator);
       verifyNoInteractions(notificationDeliveryCostMapper);
    }
}