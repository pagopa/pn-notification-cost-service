package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;
import it.pagopa.pn.notificationcostservice.exception.PnNotificationDeliveryCostBadRequestException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.service.PaymentCostService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_BADREQUEST;
import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND;
import static it.pagopa.pn.notificationcostservice.utils.CostUtils.getTotalCost;

@Slf4j
@AllArgsConstructor
@Service
public class PaymentCostServiceImpl implements PaymentCostService {

    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final NotificationDeliveryCostMapper notificationDeliveryCostMapper;

    @Override
    public Mono<NotificationCostRecipientResponseDto> getNotificationCostRecipient(String iun, Integer recIndex) {
        log.info("Start to get notification cost recipient for iun: {} and RecIndex: {}", iun, recIndex);
        if (Strings.isBlank(iun) || recIndex == null) {
            log.error("Bad Request: Iun and RecIndex must not be null");
            return Mono.error(new PnNotificationDeliveryCostBadRequestException(
                    "Bad Request", "Iun and RecIndex must not be null", ERROR_CODE_NOTIFICATIONDELIVERYCOST_BADREQUEST));
        }
        return notificationDeliveryCostDao.getNotificationDeliveryCostItem(iun, recIndex)
                .flatMap(dto -> {
                    if (Boolean.TRUE.equals(dto.getIsCancelled()) || Boolean.TRUE.equals(dto.getIsRefused())) {
                        log.info("Notification with iun: {} and RecIndex: {} is cancelled or refused", iun, recIndex);
                        return Mono.error(new PnNotFoundException("Not Found",
                                "Notification with iun: " + dto.getIun() + " and RecIndex: " + dto.getRecIndex() + " is cancelled or refused",
                                ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND));
                    }
                    log.info("Item retrieved for iun: {} and RecIndex: {}", iun, recIndex);
                    Integer totalCost = getTotalCost(
                            dto.getBaseCost(),
                            dto.getFirstAnalogCost(),
                            dto.getSecondAnalogCost(),
                            dto.getSimpleRegisteredLetterCost(),
                            dto.getVat(),
                            dto.getNotificationFeePolicy()
                    );
                    log.info("End process to get notification cost recipient for iun:{} and RecIndex: {}", iun, recIndex);
                    return Mono.just(notificationDeliveryCostMapper.mapDtoToResponseDto(dto, totalCost));
                })
                .doOnError(e -> log.error("Error processing cost recipient for iun: {} - Error: {}", iun, e.getMessage()));
    }
}