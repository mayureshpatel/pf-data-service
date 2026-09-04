package com.mayureshpatel.pfdataservice.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a raw bank/CSV transaction description into a readable merchant display name.
 * <p>
 * Rule-based only (PF-EPIC-021's interview deliberately chose this over any ML/learned-matching
 * approach): normalizes case, strips trailing numeric reference codes (store numbers, transaction
 * codes), and strips a trailing recognized US state code. It does not attempt to detect or strip
 * city names, since reliably doing so would need a real gazetteer rather than a fixed rule set --
 * out of this epic's explicitly rule-based scope. One accepted consequence: a real trailing word
 * that happens to match a state code (e.g. a company name ending in "CO") still gets stripped --
 * a deliberately aggressive tradeoff, not an oversight (see the interview notes on PF-EPIC-021).
 * <p>
 * Used both at merchant-creation time ({@link MerchantService}) and by the one-time backfill
 * migration for existing merchants -- both must call this same function so the two never drift
 * into producing different results for the same raw name.
 */
@Component
public class MerchantNameNormalizer {

    private static final Set<String> US_STATE_CODES = Set.of(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA",
            "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT",
            "VA", "WA", "WV", "WI", "WY", "DC"
    );

    // a trailing 2-letter token, preceded by whitespace/comma so it's a standalone "word", not the
    // tail end of a longer one (e.g. matches " WA" in "SHELL OIL WA" but not "...MAXX")
    private static final Pattern TRAILING_TWO_LETTER_TOKEN = Pattern.compile("[\\s,]+([A-Za-z]{2})$");

    // a trailing numeric reference code: optional #/* marker, then a digit, then more digits/dashes.
    // anchored on either a leading separator or the start of the string, so a name that's nothing
    // but a reference code (e.g. "#4523") can still be stripped down to empty and hit the fallback.
    private static final Pattern TRAILING_NUMERIC_TOKEN = Pattern.compile("(?:^|[\\s,]+)[#*]?\\d[\\d\\-]*$");

    /**
     * Normalizes a raw transaction description into a display-ready merchant name.
     *
     * @param originalName the raw bank/CSV description; may be null
     * @return a non-blank, title-cased name -- falls back to the title-cased original if
     * stripping would otherwise leave nothing, and returns "" only for a null/blank input
     */
    public String normalize(String originalName) {
        if (originalName == null) {
            return "";
        }

        String trimmed = originalName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String stripped = stripTrailingLocationCode(trimmed);
        stripped = stripTrailingNumericTokens(stripped);
        stripped = stripped.trim();

        String result = stripped.isEmpty() ? trimmed : stripped;
        return toTitleCase(result);
    }

    /**
     * Strips one trailing US state code, if the string ends with one as a standalone token.
     */
    private String stripTrailingLocationCode(String input) {
        Matcher matcher = TRAILING_TWO_LETTER_TOKEN.matcher(input);
        if (matcher.find() && US_STATE_CODES.contains(matcher.group(1).toUpperCase(Locale.ROOT))) {
            return input.substring(0, matcher.start());
        }
        return input;
    }

    /**
     * Repeatedly strips trailing numeric/reference-code tokens -- real bank descriptions can carry
     * more than one (e.g. a store number followed by a separate transaction code).
     */
    private String stripTrailingNumericTokens(String input) {
        String result = input;
        Matcher matcher = TRAILING_NUMERIC_TOKEN.matcher(result);
        while (matcher.find()) {
            result = result.substring(0, matcher.start());
            matcher = TRAILING_NUMERIC_TOKEN.matcher(result);
        }
        return result;
    }

    /**
     * Title-cases a string: lowercase throughout, then capitalize the first letter of each
     * whitespace-separated word. Words with no letters (pure numbers/symbols) pass through
     * unchanged.
     */
    private String toTitleCase(String input) {
        String[] words = input.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder result = new StringBuilder(input.length());
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            if (i < words.length - 1) {
                result.append(" ");
            }
        }
        return result.toString();
    }
}
