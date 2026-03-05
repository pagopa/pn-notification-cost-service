package it.pagopa.pn.notificationcostservice.utils;

public class CostUtils {
    private CostUtils() {
    }

    /**
     * Calcola e applica l'IVA a un costo
     */
    public static Integer getCostWithVat(int cost, int vat) {
        double completeCostWithVat = (double) cost + ((double) cost * ((double)vat / 100));
        return Math.toIntExact(Math.round(completeCostWithVat));
    }
}
