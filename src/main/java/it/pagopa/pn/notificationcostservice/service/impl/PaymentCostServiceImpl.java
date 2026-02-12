package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationCostRecipientResponseDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotificationDeliveryCostBadRequestException;
import it.pagopa.pn.notificationcostservice.middleware.dao.notificationdeliverycost.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.service.PaymentCostService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationDeliveryCostMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_BADREQUEST;
import static it.pagopa.pn.notificationcostservice.utils.CostUtils.getTotalCost;

@Slf4j
@AllArgsConstructor
@Service
public class PaymentCostServiceImpl implements PaymentCostService {

    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final NotificationDeliveryCostMapper notificationDeliveryCostMapper;

    @Override
    public Mono<NotificationCostRecipientResponseDto> getNotificationCostRecipient(String iun, Integer recIndex) {
        log.info("Start to get notification cost recipient for iun: {} e RecIndex: {}", iun, recIndex);
        if (Strings.isNotBlank(iun) && recIndex != null) {
            return notificationDeliveryCostDao.getNotificationDeliveryCostItem(iun, recIndex)
                    .map(dto -> {
                        log.info("Item retrieved from DB for iun: {} and RecIndex: {}", iun, recIndex);
                        //Calcolo Totale: Base + Costi Analogici con IVA
                        Integer totalCost = getTotalCost(Objects.requireNonNull(dto).getBaseCost(), dto.getFirstAnalogCost(), dto.getSecondAnalogCost(), dto.getSimpleRegisteredLetterCost(), dto.getVat(), dto.getNotificationFeePolicy());
                        log.info("End to get notification cost recipient for iun: {} and RecIndex: {} with totalCost: {}", iun, recIndex, totalCost);
                        return notificationDeliveryCostMapper.mapDtoToResponseDto(dto, totalCost);
                    });
        } else {
            log.error("Bad Request: Iun and RecIndex must not be null");
            return Mono.error(new PnNotificationDeliveryCostBadRequestException("Bad Request", "Iun and RecIndex must not be null", ERROR_CODE_NOTIFICATIONDELIVERYCOST_BADREQUEST));
        }
    }
}
