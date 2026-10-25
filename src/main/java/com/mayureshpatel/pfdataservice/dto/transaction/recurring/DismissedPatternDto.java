package com.mayureshpatel.pfdataservice.dto.transaction.recurring;

import java.time.OffsetDateTime;

/**
 * A recurring pattern the user dismissed from the detected-patterns review.
 *
 * @param id          the dismissal id (used to restore it)
 * @param patternKey  the folded pattern key detection excludes
 * @param name        the detected name as the user saw it
 * @param dismissedAt when it was dismissed
 */
public record DismissedPatternDto(
        Long id,
        String patternKey,
        String name,
        OffsetDateTime dismissedAt
) {
}
