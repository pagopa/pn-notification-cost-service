package it.pagopa.pn.notificationcostservice.utils;

import it.pagopa.pn.notificationcostservice.model.utils.IntegerInterval;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainValidationUtilsTest {
    @Test
    void validateNonNullableField_AddsViolationWhenFieldIsNull() {
        List<String> violations = new ArrayList<>();
        DomainValidationUtils.validateNonNullableField(null, "testField", violations);
        assertEquals(1, violations.size());
        assertEquals("Field testField is required and cannot be null", violations.getFirst());
    }

    @Test
    void validateNonNullableField_DoesNotAddViolationWhenFieldIsNotNull() {
        List<String> violations = new ArrayList<>();
        Object field = new Object();
        DomainValidationUtils.validateNonNullableField(field, "testField", violations);
        assertEquals(0, violations.size());
    }

    @Test
    void validatePositiveIntField_AddsViolationWhenFieldIsNull() {
        List<String> violations = new ArrayList<>();
        DomainValidationUtils.validatePositiveIntField(null, "testField", violations);
        assertEquals(1, violations.size());
        assertEquals("Field testField is required and cannot be null", violations.getFirst());
    }

    @Test
    void validatePositiveIntField_AddsViolationWhenFieldIsNegative() {
        List<String> violations = new ArrayList<>();
        DomainValidationUtils.validatePositiveIntField(-1, "testField", violations);
        assertEquals(1, violations.size());
        assertEquals("Field testField cannot be negative", violations.getFirst());
    }

    @Test
    void validatePositiveIntField_DoesNotAddViolationWhenFieldIsPositive() {
        List<String> violations = new ArrayList<>();
        DomainValidationUtils.validatePositiveIntField(100, "testField", violations);
        assertEquals(0, violations.size());
    }

    @Test
    void validateIntervalIntField_AddsViolationWhenFieldIsNull() {
        List<String> violations = new ArrayList<>();
        IntegerInterval range = new IntegerInterval(0, 100);
        DomainValidationUtils.validateIntervalIntField(null, "testField", violations, range);
        assertEquals(1, violations.size());
        assertEquals("Field testField is required and cannot be null", violations.getFirst());
    }

    @Test
    void validateIntervalIntField_AddsViolationWhenFieldIsLessThanMin() {
        List<String> violations = new ArrayList<>();
        IntegerInterval range = new IntegerInterval(0, 100);
        DomainValidationUtils.validateIntervalIntField(-1, "testField", violations, range);
        assertEquals(1, violations.size());
        assertEquals("Field testField cannot be less than 0", violations.getFirst());
    }

    @Test
    void validateIntervalIntField_AddsViolationWhenFieldIsGreaterThanMax() {
        List<String> violations = new ArrayList<>();
        IntegerInterval range = new IntegerInterval(0, 100);
        DomainValidationUtils.validateIntervalIntField(101, "testField", violations, range);
        assertEquals(1, violations.size());
        assertEquals("Field testField cannot be greater than 100", violations.getFirst());
    }

    @Test
    void validateIntervalIntField_DoesNotAddViolationWhenFieldEqualsMin() {
        List<String> violations = new ArrayList<>();
        IntegerInterval range = new IntegerInterval(0, 100);
        DomainValidationUtils.validateIntervalIntField(0, "testField", violations, range);
        assertEquals(0, violations.size());
    }

    @Test
    void validateIntervalIntField_DoesNotAddViolationWhenFieldEqualsMax() {
        List<String> violations = new ArrayList<>();
        IntegerInterval range = new IntegerInterval(0, 100);
        DomainValidationUtils.validateIntervalIntField(100, "testField", violations, range);
        assertEquals(0, violations.size());
    }

    @Test
    void validateIntervalIntField_DoesNotAddViolationWhenFieldIsWithinRange() {
        List<String> violations = new ArrayList<>();
        IntegerInterval range = new IntegerInterval(0, 100);
        DomainValidationUtils.validateIntervalIntField(50, "testField", violations, range);
        assertEquals(0, violations.size());
    }
}
