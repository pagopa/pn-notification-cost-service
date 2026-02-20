package it.pagopa.pn.notificationcostservice.utils;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.BaseCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.FirstAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SecondAnalogCostDto;
import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost.SimpleRegisteredLetterCostDto;
import it.pagopa.pn.notificationcostservice.exception.PnNotFoundException;

import java.util.Objects;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND_BASECOST;

public class CostUtils {
    private CostUtils() {
    }

    /**
     * Calcola il costo totale di una notifica, sommando il costo base e i costi degli analogici con l'IVA applicata se e solo se la policy è uguale a DELIVERY_MODE.
     */
    public static Integer getTotalCost(BaseCostDto baseCostDto, FirstAnalogCostDto firsAnalogCost, SecondAnalogCostDto secondAnalogCost, SimpleRegisteredLetterCostDto simpleRegisteredLetterCost, Integer vat, NotificationFeePolicy notificationFeePolicy) {
        int totalCost = 0;
        if (Objects.isNull(baseCostDto)) {
            throw new PnNotFoundException("Not found", "baseCost must not be null", ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND_BASECOST);
        }
        if (NotificationFeePolicy.DELIVERY_MODE.equals(notificationFeePolicy)) {
            if (Objects.nonNull(baseCostDto.getPaFee()) && Objects.nonNull(baseCostDto.getSendFee())) {
                totalCost = getTotalCost(baseCostDto, firsAnalogCost, secondAnalogCost, simpleRegisteredLetterCost, vat);
            }
            else {
                throw new PnNotFoundException("Not found", "baseCost must have both paFee and sendFee not null", ERROR_CODE_NOTIFICATIONDELIVERYCOST_NOTFOUND_BASECOST);
            }
        }
        return totalCost;
    }

    private static int getTotalCost(BaseCostDto baseCostDto, FirstAnalogCostDto firsAnalogCost, SecondAnalogCostDto secondAnalogCost, SimpleRegisteredLetterCostDto simpleRegisteredLetterCost, Integer vat) {
        int totalCost;
        int baseCost = baseCostDto.getPaFee() + baseCostDto.getSendFee();
        Integer firstAnalogCostValue = Objects.nonNull(firsAnalogCost) ? firsAnalogCost.getCost() : null;
        Integer secondAnalogCostValue = Objects.nonNull(secondAnalogCost) ? secondAnalogCost.getCost() : null;
        Integer simpleRegisteredCostValue = Objects.nonNull(simpleRegisteredLetterCost) ? simpleRegisteredLetterCost.getCost() : null;
        totalCost = baseCost
                + getCostWithVat(firstAnalogCostValue, vat)
                + getCostWithVat(secondAnalogCostValue, vat)
                + getCostWithVat(simpleRegisteredCostValue, vat);
        return totalCost;
    }

    /**
     * Calcola e applica l'IVA a un costo
     */
    public static Integer getCostWithVat(Integer cost, Integer vat) {
        int costWithVat = 0;
        if (vat != null && cost != null) {
            double completeCostWithVat = cost.doubleValue() + (cost.doubleValue() * vat.doubleValue() / 100);
            costWithVat = Math.toIntExact(Math.round(completeCostWithVat));
        }
        return costWithVat;
    }

}
