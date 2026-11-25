package com.mayureshpatel.pfdataservice.service;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Derives the group key that gathers raw transaction descriptions sharing the same merchant text,
 * so the merchants page can offer one decision per group ("KROGER # ..." with 11 store variants)
 * instead of one per store.
 * <p>
 * The key keeps the leading words of a description and cuts at the first token that carries a
 * digit (a store number, reference code or the digits glued to a word such as {@code CTR1687}),
 * which also drops the city and state that follow a store number. When nothing is cut, a trailing
 * US state code is dropped. Leading tokens with no letters (reference numbers, dashes) are skipped.
 * <p>
 * Keys that differ by a whole word never merge: {@code kroger} and {@code kroger fuel ctr} are two
 * groups, which is what keeps a fuel center from being linked to the plain store by accident;
 * {@link DescriptionGrouper} marks such pairs as similar instead.
 * <p>
 * Unlike {@link DescriptionKeys#fold}, which is the exact key a description link matches on, this
 * key is only for grouping and display; it is never stored or matched against.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DescriptionGroupKeys {

    /**
     * Returns the lowercase group key for a raw description.
     *
     * @param raw the raw description, may be {@code null}
     * @return the group key, or the whole folded description when it has no letters at all, or an
     *         empty string for a {@code null} or blank description
     */
    public static String key(String raw) {
        return displayName(raw).toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the same words {@link #key} keeps, in the raw description's own case, for showing a
     * group's name ("Kroger Fuel Ctr").
     *
     * @param raw the raw description, may be {@code null}
     * @return the kept words joined by single spaces, the whole whitespace-collapsed description
     *         when no word has letters, or an empty string for a {@code null} or blank description
     */
    public static String displayName(String raw) {
        String collapsed = DescriptionKeys.collapse(raw);
        if (collapsed.isEmpty()) {
            return "";
        }
        List<String> words = keptWords(collapsed.split(" "));
        return words.isEmpty() ? collapsed : String.join(" ", words);
    }

    private static List<String> keptWords(String[] tokens) {
        List<String> words = new ArrayList<>();
        boolean cut = false;
        for (String token : tokens) {
            if (!hasLetter(token)) {
                // a reference number or stray punctuation: skip it before the first word, stop after
                if (words.isEmpty()) {
                    continue;
                }
                cut = true;
                break;
            }
            int firstDigit = firstDigitIndex(token);
            if (firstDigit < 0) {
                words.add(token);
                continue;
            }
            String prefix = stripTrailingPunctuation(token.substring(0, firstDigit));
            if (hasLetter(prefix)) {
                // letters glued to a store number (ctr1687, us*2k3): keep the letters, then stop
                words.add(prefix);
                cut = true;
                break;
            }
            if (words.isEmpty()) {
                // a name that starts with a digit (7-eleven) is the merchant itself
                words.add(token);
                continue;
            }
            cut = true;
            break;
        }
        if (!cut) {
            dropTrailingState(words);
        }
        return words;
    }

    private static void dropTrailingState(List<String> words) {
        if (words.size() < 2) {
            return;
        }
        String last = words.get(words.size() - 1);
        if (last.length() == 2 && MerchantNameNormalizer.US_STATE_CODES.contains(last.toUpperCase(Locale.ROOT))) {
            words.remove(words.size() - 1);
            int lastIndex = words.size() - 1;
            words.set(lastIndex, stripTrailingPunctuation(words.get(lastIndex)));
        }
    }

    private static boolean hasLetter(String token) {
        return token.chars().anyMatch(Character::isLetter);
    }

    private static int firstDigitIndex(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (Character.isDigit(token.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String stripTrailingPunctuation(String value) {
        int end = value.length();
        while (end > 0 && !Character.isLetterOrDigit(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }
}
