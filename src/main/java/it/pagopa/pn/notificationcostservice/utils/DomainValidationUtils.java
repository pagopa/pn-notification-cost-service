package it.pagopa.pn.notificationcostservice.utils;

import it.pagopa.pn.notificationcostservice.model.utils.IntegerInterval;

import java.util.List;

public class DomainValidationUtils {
    private DomainValidationUtils() {
    }

    public static boolean validateNonNullableField(Object field, String fieldName, List<String> violations) {
        if (field == null) {
            violations.add(String.format("Field %s is required and cannot be null", fieldName));
            return false;
        }
        return true;
    }

    public static void validatePositiveIntField(Integer field, String fieldName, List<String> violations) {
        if (!validateNonNullableField(field, fieldName, violations)) {
            return;
        }
        if (field < 0) {
            violations.add(String.format("Field %s cannot be negative", fieldName));
        }
    }

    public static void validateIntervalIntField(Integer field, String fieldName, List<String> violations, IntegerInterval range) {
        if (!validateNonNullableField(field, fieldName, violations)) {
            return;
        }
        if (field < range.getMin()) {
            violations.add(String.format("Field %s cannot be less than %d", fieldName, range.getMin()));
        }
        if (field > range.getMax()) {
            violations.add(String.format("Field %s cannot be greater than %d", fieldName, range.getMax()));
        }
    }
}
