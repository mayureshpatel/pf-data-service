package com.mayureshpatel.pfdataservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies {@link ExploreToday}: the server clock is the default and a client date is accepted only within a day of it. */
@DisplayName("ExploreToday Unit Tests")
class ExploreTodayTest {

    private static ExploreToday at(String instant) {
        return new ExploreToday(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("should use the server's UTC date when the client sends none")
    void resolve_shouldUseTheClockWhenAbsent() {
        // arrange
        ExploreToday today = at("2026-10-25T10:00:00Z");

        // act
        LocalDate result = today.resolve(null);

        // assert & verify
        assertEquals(LocalDate.of(2026, 10, 25), result);
    }

    @Test
    @DisplayName("should accept the client's date one day either side of the server's")
    void resolve_shouldAcceptPlusOrMinusOneDay() {
        // arrange
        ExploreToday today = at("2026-10-25T10:00:00Z");

        // act & assert & verify
        assertEquals(LocalDate.of(2026, 10, 24), today.resolve(LocalDate.of(2026, 10, 24)));
        assertEquals(LocalDate.of(2026, 10, 25), today.resolve(LocalDate.of(2026, 10, 25)));
        assertEquals(LocalDate.of(2026, 10, 26), today.resolve(LocalDate.of(2026, 10, 26)));
    }

    @Test
    @DisplayName("should reject a client date more than a day from the server's")
    void resolve_shouldRejectOutsideOneDay() {
        // arrange
        ExploreToday today = at("2026-10-25T10:00:00Z");

        // act
        IllegalArgumentException early = assertThrows(IllegalArgumentException.class, () -> today.resolve(LocalDate.of(2026, 10, 23)));
        IllegalArgumentException late = assertThrows(IllegalArgumentException.class, () -> today.resolve(LocalDate.of(2026, 10, 27)));

        // assert & verify
        assertEquals("The today date 2026-10-23 is not within a day of the server's date 2026-10-25", early.getMessage());
        assertEquals("The today date 2026-10-27 is not within a day of the server's date 2026-10-25", late.getMessage());
    }

    @Test
    @DisplayName("should let a client ahead of the server near midnight see the new month")
    void resolve_shouldHonourAClientAheadNearMidnight() {
        // arrange -- 23:30 UTC on Oct 31 is already Nov 1 in Tokyo
        ExploreToday today = at("2026-10-31T23:30:00Z");

        // act
        LocalDate server = today.resolve(null);
        LocalDate client = today.resolve(LocalDate.of(2026, 11, 1));

        // assert & verify
        assertEquals(LocalDate.of(2026, 10, 31), server);
        assertEquals(LocalDate.of(2026, 11, 1), client);
    }

    @Test
    @DisplayName("should let a client behind the server at a month start see the old month")
    void resolve_shouldHonourAClientBehindAtAMonthStart() {
        // arrange -- 00:30 UTC on Nov 1 is still Oct 31 in New York
        ExploreToday today = at("2026-11-01T00:30:00Z");

        // act
        LocalDate client = today.resolve(LocalDate.of(2026, 10, 31));

        // assert & verify
        assertEquals(LocalDate.of(2026, 10, 31), client);
    }
}
