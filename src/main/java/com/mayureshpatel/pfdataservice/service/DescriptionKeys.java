package com.mayureshpatel.pfdataservice.service;

import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * The one folding rule that decides whether two raw transaction descriptions are "the same text":
 * collapse every run of white space (including no-break and other Unicode spaces) to one space, trim, lower-case. Merchant description links match
 * on exactly this key, and recurring-pattern detection and dismissals reuse it so a pattern, a
 * dismissal and a link can never disagree about what counts as the same description.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DescriptionKeys {

    /** Every kind of white space that counts as a gap: ASCII white space plus the no-break, en/em/thin and ideographic spaces, which Java and PostgreSQL otherwise disagree about. */
    private static final String GAP = "[\\s\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]+";

    /**
     * Folds a raw description into its comparison key.
     *
     * @param raw the raw description; may be null
     * @return the folded key, or {@code ""} for a null input
     */
    public static String fold(String raw) {
        return raw == null ? "" : raw.replaceAll(GAP, " ").strip().toLowerCase(Locale.ROOT);
    }

    /**
     * The same fold as an SQL expression, so a query can never drift from {@link #fold}.
     *
     * @param column the SQL expression holding the raw description, such as {@code transactions.description}
     * @return an expression yielding the folded key
     */
    public static String sqlFold(String column) {
        return "lower(btrim(regexp_replace(" + column + ", '" + GAP + "', ' ', 'g')))";
    }
}
