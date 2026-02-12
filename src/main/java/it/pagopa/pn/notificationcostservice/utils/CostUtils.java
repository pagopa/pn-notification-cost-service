package it.pagopa.pn.notificationcostservice.utils;

import it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.NotificationFeePolicy;

public class CostUtils {
    private CostUtils() {
    }

    /**
     * Calcola il costo totale di una notifica, sommando il costo base e i costi degli analogici con l'IVA applicata se e solo se la policy è uguale a DELIVERY_MODE.
     */
    public static Integer getTotalCost(Integer baseCost, Integer firsAnalogCost, Integer secondAnalogCost, Integer simpleRegisteredLetterCost, Integer vat, NotificationFeePolicy notificationFeePolicy) {
        int totalCost = 0;
        if (baseCost != null && NotificationFeePolicy.DELIVERY_MODE.equals(notificationFeePolicy)) {
            totalCost = baseCost + getCostWithVat(firsAnalogCost, vat) + getCostWithVat(secondAnalogCost, vat) + getCostWithVat(simpleRegisteredLetterCost, vat);
        }
        return totalCost;
    }

    /**
     * Calcola e applica l'IVA a un costo
     */
    public static Integer getCostWithVat(Integer cost,Integer vat) {
        int costWithVat = 0;
        if (vat != null && cost != null) {
            double completeCostWithVat = cost.doubleValue() + (cost.doubleValue() * vat.doubleValue() / 100);
            costWithVat = Math.toIntExact(Math.round(completeCostWithVat));
        }
        return costWithVat;
    }

}
