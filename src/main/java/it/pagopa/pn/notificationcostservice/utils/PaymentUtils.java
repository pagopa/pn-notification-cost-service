package it.pagopa.pn.notificationcostservice.utils;

public class PaymentUtils {
    private PaymentUtils() {
    }

    public static String composeIuv(String creditorTaxId, String noticeCode) {
        return creditorTaxId + "##" + noticeCode;
    }
}
