package com.mayureshpatel.pfdataservice.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
 * PF-832: "doesn't detect city names" means this never *removes* one -- it doesn't mean a city's
 * mere presence should block removing a number/state that's otherwise clearly identifiable. The
 * dominant real-world shape is "<store> #<number> <city> <state>"; stripping the trailing state
 * first, then only stripping a numeric token from the new trailing position, left the store
 * number stuck whenever a city sat between them -- confirmed live, 44.7% of real merchants still
 * carried a raw store number, one chain split across 15 separate merchant records. See below.
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

    // a whole token that's nothing but a numeric/reference code: optional #/* marker, then a
    // digit, then more digits/dashes. Matched per-token (see tokenize()), not against the whole
    // string, so it can be found and removed regardless of what comes after it.
    private static final Pattern NUMERIC_TOKEN = Pattern.compile("^[#*]?\\d[\\d\\-]*$");

    // splits on runs of whitespace and/or commas -- the same separators the original
    // whole-string patterns treated as token boundaries.
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[\\s,]+");

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

        List<String> tokens = tokenize(trimmed);
        boolean stateStripped = stripTrailingStateCode(tokens);

        if (stateStripped) {
            // Only once a real trailing state code confirms this is genuinely a
            // location-suffixed record do we look *past* intervening tokens (a city) for a
            // number -- otherwise a merchant name that simply starts with a number (e.g. "24
            // Hour Fitness") would have its leading digits misread as a store code. See PF-832.
            stripNumericTokenPastTrailingCity(tokens);
        } else {
            stripTrailingNumericTokens(tokens);
        }

        String result = String.join(" ", tokens);
        return toTitleCase(result.isEmpty() ? trimmed : result);
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>(List.of(TOKEN_SEPARATOR.split(input)));
        tokens.removeIf(String::isEmpty);
        return tokens;
    }

    /**
     * Removes one trailing US state code, if the last token is a standalone recognized one.
     *
     * @return true if a state code was found and removed
     */
    private boolean stripTrailingStateCode(List<String> tokens) {
        if (tokens.isEmpty()) {
            return false;
        }
        String last = tokens.get(tokens.size() - 1);
        if (last.length() == 2 && US_STATE_CODES.contains(last.toUpperCase(Locale.ROOT))) {
            tokens.remove(tokens.size() - 1);
            return true;
        }
        return false;
    }

    /**
     * Repeatedly strips trailing numeric/reference-code tokens from the current end of the list
     * -- real bank descriptions can carry more than one (e.g. a store number followed by a
     * separate transaction code). Used when no trailing state code was found, preserving the
     * original end-anchored-only behavior exactly (never reaches past a non-numeric token).
     */
    private void stripTrailingNumericTokens(List<String> tokens) {
        while (!tokens.isEmpty() && NUMERIC_TOKEN.matcher(tokens.get(tokens.size() - 1)).matches()) {
            tokens.remove(tokens.size() - 1);
        }
    }

    /**
     * Scans backward from the end, skipping (keeping) any non-numeric tokens -- a city name, left
     * alone per this class's documented scope -- until either a numeric token is found or the
     * list is exhausted. Once found, removes that token and any immediately preceding numeric
     * tokens too (a store number followed by a separate code), then stops -- it does not keep
     * searching past a real word for a second, non-adjacent number.
     */
    private void stripNumericTokenPastTrailingCity(List<String> tokens) {
        int i = tokens.size() - 1;
        while (i >= 0 && !NUMERIC_TOKEN.matcher(tokens.get(i)).matches()) {
            i--;
        }
        if (i < 0) {
            return; // no numeric token anywhere in the remaining tokens
        }
        int lastNumericIndex = i;
        while (i > 0 && NUMERIC_TOKEN.matcher(tokens.get(i - 1)).matches()) {
            i--;
        }
        tokens.subList(i, lastNumericIndex + 1).clear();
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
                result.append(' ');
            }
        }
        return result.toString();
    }
}
