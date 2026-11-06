package com.mayureshpatel.pfdataservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies {@link ExploreRangeValidator}: nonsense ranges are refused with a clear message and every sensible one, boundaries included, passes. */
@DisplayName("ExploreRangeValidator Unit Tests")
class ExploreRangeValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    private static String reject(LocalDate start, LocalDate end, LocalDate today) {
        return assertThrows(IllegalArgumentException.class, () -> ExploreRangeValidator.validate(start, end, today)).getMessage();
    }

    @Test
    @DisplayName("should reject a year 0002 start")
    void validate_shouldRejectYear0002() {
        // act
        String message = reject(LocalDate.of(2, 1, 1), LocalDate.of(2026, 1, 31), TODAY);

        // assert & verify
        assertTrue(message.contains("2000-01-01"));
    }

    @Test
    @DisplayName("should reject a start before 2000 and accept 2000-01-01 itself")
    void validate_shouldBoundTheStart() {
        // act & assert & verify
        reject(LocalDate.of(1999, 12, 31), LocalDate.of(2000, 1, 31), TODAY);
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 31), TODAY));
    }

    @Test
    @DisplayName("should reject an end more than a year past today and accept exactly a year")
    void validate_shouldBoundTheEnd() {
        // act
        String message = reject(LocalDate.of(2026, 1, 1), TODAY.plusYears(1).plusDays(1), TODAY);

        // assert & verify
        assertTrue(message.contains("2027-10-25"));
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(LocalDate.of(2026, 1, 1), TODAY.plusYears(1), TODAY));
    }

    @Test
    @DisplayName("should reject a start after the end and accept a single day")
    void validate_shouldRejectAReversedRange() {
        // act
        String message = reject(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31), TODAY);

        // assert & verify
        assertTrue(message.contains("after the end"));
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(TODAY, TODAY, TODAY));
    }

    @Test
    @DisplayName("should reject a range with too many buckets and accept one at the cap")
    void validate_shouldCapTheBucketCount() {
        // arrange -- monthly buckets once the range passes 92 days; at today 2040 the range 2000..2040 is 481 months
        LocalDate farToday = LocalDate.of(2040, 1, 15);
        LocalDate atCap = LocalDate.of(2000, 1, 1).plusMonths(ExploreRangeValidator.MAX_BUCKETS - 1).withDayOfMonth(1);

        // act
        String message = reject(LocalDate.of(2000, 1, 1), farToday, farToday);

        // assert & verify
        assertTrue(message.contains("too many month buckets"));
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(LocalDate.of(2000, 1, 1), atCap, farToday));
        reject(LocalDate.of(2000, 1, 1), atCap.plusMonths(1), farToday);
    }

    @Test
    @DisplayName("should accept ordinary ranges: a month, a quarter, a year, all time since 2000 and a future end")
    void validate_shouldAcceptSensibleRanges() {
        // act & assert & verify
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), TODAY));
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30), TODAY));
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), TODAY));
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(LocalDate.of(2000, 1, 1), TODAY, TODAY));
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), TODAY));
    }

    @Test
    @DisplayName("should accept a range that starts in the future, since it has nothing to bucket")
    void validate_shouldAcceptAFutureRange() {
        // act & assert & verify
        assertDoesNotThrow(() -> ExploreRangeValidator.validate(TODAY.plusDays(5), TODAY.plusDays(40), TODAY));
    }
}
