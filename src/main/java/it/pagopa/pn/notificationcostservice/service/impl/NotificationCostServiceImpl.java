package it.pagopa.pn.notificationcostservice.service.impl;


import it.pagopa.pn.notification_cost_service.generated.openapi.server.v1.dto.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.model.cost.CalculatedCosts;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.service.CostCalculator;
import it.pagopa.pn.notificationcostservice.service.NotificationCostService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_DELETED;

@Slf4j
@AllArgsConstructor
@Service
public class NotificationCostServiceImpl implements NotificationCostService {

    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final NotificationDeliveryCostMapper notificationDeliveryCostMapper;
    private final CostCalculator costCalculator;

    @Override
    public Mono<NotificationCostRecipientResponseDto> getNotificationCostRecipient(String iun, Integer recIndex) {
        log.info("Start to get notification cost recipient for iun: {} and recIndex: {}", iun, recIndex);
        return notificationDeliveryCostDao.getNotificationDeliveryCostItem(iun, recIndex)
                .map(dto -> {
                    if (Boolean.TRUE.equals(dto.getIsDeleted())) {
                        log.info("Notification with iun: {} and recIndex: {} is deleted", iun, recIndex);
                        throw new PnNotFoundException("Not Found",
                                "Notification with iun: " + dto.getIun() + " and recIndex: " + dto.getRecIndex() + " is deleted",
                                ERROR_CODE_NOTIFICATIONDELIVERYCOST_DELETED);
                    }
                    log.info("Item retrieved for iun: {} and recIndex: {}", iun, recIndex);
                    CalculatedCosts calculatedCosts = costCalculator.calculateCosts(dto);
                    return notificationDeliveryCostMapper.mapDtoToResponse(dto, calculatedCosts);
                })
                .doOnError(e -> log.error("Error processing cost recipient for iun: {} and recIndex: {}", iun, recIndex, e));
    }
}