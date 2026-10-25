package com.mayureshpatel.pfdataservice.dto.report.explore;

import java.time.OffsetDateTime;

/**
 * A half-open UTC time window {@code [start, end)}, the bound shape every report query uses.
 *
 * @param start the first instant included
 * @param end   the first instant excluded
 */
public record DateWindow(OffsetDateTime start, OffsetDateTime end) {
}
