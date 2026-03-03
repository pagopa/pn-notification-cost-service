package it.pagopa.pn.notificationcostservice.utils;

public class CostUtils {
    private CostUtils() {
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
