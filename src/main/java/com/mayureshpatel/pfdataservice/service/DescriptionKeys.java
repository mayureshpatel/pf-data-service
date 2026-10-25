package com.mayureshpatel.pfdataservice.service;

import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * The one folding rule that decides whether two raw transaction descriptions are "the same text":
 * trim, collapse every run of whitespace to one space, lower-case. Merchant description links match
 * on exactly this key, and recurring-pattern detection and dismissals reuse it so a pattern, a
 * dismissal and a link can never disagree about what counts as the same description.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DescriptionKeys {

    /**
     * Folds a raw description into its comparison key.
     *
     * @param raw the raw description; may be null
     * @return the folded key, or {@code ""} for a null input
     */
    public static String fold(String raw) {
        return raw == null ? "" : raw.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
