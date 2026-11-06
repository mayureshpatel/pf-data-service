package com.mayureshpatel.pfdataservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Decides what "today" means for one explorer request, so the range the client shows and the data the
 * server returns use the same date. A client may send its own local date; it is accepted only within a
 * day of the server's UTC date (which covers every time zone), so an arbitrary date is never trusted.
 */
@Component
@RequiredArgsConstructor
public class ExploreToday {

    private static final long MAX_DRIFT_DAYS = 1;

    private final Clock clock;

    /**
     * @param requested the client's local date, or null to use the server's
     * @return the date to treat as today
     * @throws IllegalArgumentException if the requested date is more than a day from the server's UTC date
     */
    public LocalDate resolve(LocalDate requested) {
        LocalDate server = LocalDate.now(clock);
        if (requested == null) {
            return server;
        }
        if (Math.abs(ChronoUnit.DAYS.between(server, requested)) > MAX_DRIFT_DAYS) {
            throw new IllegalArgumentException("The today date " + requested + " is not within a day of the server's date " + server);
        }
        return requested;
    }
}
