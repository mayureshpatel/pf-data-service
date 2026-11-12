package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.dto.ValidatorTestBase;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies {@link BalanceEntryRequest}'s bean-validation constraints, in particular the NUMERIC(19,2) bounds on {@code balance}. */
@DisplayName("BalanceEntryRequest Unit Tests")
class BalanceEntryRequestTest extends ValidatorTestBase {

    private static BalanceEntryRequest withBalance(BigDecimal balance) {
        return new BalanceEntryRequest(1L, balance, LocalDate.of(2026, 11, 1), 0L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "12500.00", "-1200.50", "9999999999.99", "-9999999999.99"})
    @DisplayName("should pass for balances within the NUMERIC(19,2) range")
    void shouldPassWithinBounds(String balance) {
        // act
        Set<ConstraintViolation<BalanceEntryRequest>> violations = validator().validate(withBalance(new BigDecimal(balance)));

        // assert & verify
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("should fail when balance is above the maximum")
    void shouldFailAboveMax() {
        // act
        Set<ConstraintViolation<BalanceEntryRequest>> violations = validator().validate(withBalance(new BigDecimal("10000000000.00")));

        // assert & verify
        assertViolation(violations, "balance", "Balance must be less than or equal to 9999999999.99");
    }

    @Test
    @DisplayName("should fail when balance is below the minimum")
    void shouldFailBelowMin() {
        // act
        Set<ConstraintViolation<BalanceEntryRequest>> violations = validator().validate(withBalance(new BigDecimal("-10000000000.00")));

        // assert & verify
        assertViolation(violations, "balance", "Balance must be greater than or equal to -9999999999.99");
    }

    @Test
    @DisplayName("should fail when balance is null")
    void shouldFailWhenNull() {
        // act
        Set<ConstraintViolation<BalanceEntryRequest>> violations = validator().validate(withBalance(null));

        // assert & verify
        assertViolation(violations, "balance", "Balance is required");
    }

    private void assertViolation(Set<ConstraintViolation<BalanceEntryRequest>> violations, String property, String message) {
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(property) && v.getMessage().equals(message)),
                "expected " + property + ": " + message + " but got " + violations);
    }
}
