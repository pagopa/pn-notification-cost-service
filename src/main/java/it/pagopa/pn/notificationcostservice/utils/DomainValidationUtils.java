package it.pagopa.pn.notificationcostservice.utils;

import java.util.List;

public class DomainValidationUtils {
    private DomainValidationUtils() {
    }

    public static void validateNonNullableField(Object field, String fieldName, List<String> violations) {
        if (field == null) {
            violations.add(String.format("Field %s is required and cannot be null", fieldName));
        }
    }

    public static void validatePositiveIntField(Integer field, String fieldName, List<String> violations) {
        if(field == null) {
            violations.add(String.format("Field %s is required and cannot be null", fieldName));
        } else if(field < 0) {
            violations.add(String.format("Field %s cannot be negative", fieldName));
        }
    }
}
