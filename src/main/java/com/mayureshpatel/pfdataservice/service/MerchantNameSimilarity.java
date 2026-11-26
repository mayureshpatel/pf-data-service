package com.mayureshpatel.pfdataservice.service;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Scores how well a merchant's name fits a description group's name (PF-982), so the Unlinked
 * tab can suggest the likeliest merchant first. Both names are split into lowercase words of
 * letters and digits; each group word is paired with the best unused merchant word:
 * <ul>
 *     <li>the same word scores 1 ({@code kroger} / {@code Kroger}),</li>
 *     <li>one word starting the other scores 0.9 ({@code wal} / {@code Walmart}),</li>
 *     <li>an abbreviation, the group word's letters appearing in order in the merchant word with
 *     the same first letter, scores 0.75 ({@code ctr} / {@code Center}, {@code amzn} /
 *     {@code Amazon}).</li>
 * </ul>
 * The paired weights are divided by the number of distinct words across both names, so extra
 * words on either side lower the score: for the group {@code kroger fuel ctr},
 * {@code Kroger Fuel Center} scores about 0.92 and plain {@code Kroger} about 0.33, while for the
 * group {@code kroger} the order flips. Only ranking uses the score; nothing is stored or merged.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class MerchantNameSimilarity {

    private static final double EXACT = 1.0;
    private static final double PREFIX = 0.9;
    private static final double ABBREVIATION = 0.75;
    /** Shorter words match only exactly, so "a" or "us" never count as a prefix or abbreviation. */
    private static final int MIN_PARTIAL_LENGTH = 3;

    /**
     * Scores a merchant name against a group name.
     *
     * @param groupName    the description group's display name, may be {@code null}
     * @param merchantName the merchant's name, may be {@code null}
     * @return a score from 0 (no word in common) to 1 (the same words)
     */
    public static double score(String groupName, String merchantName) {
        List<String> group = words(groupName);
        List<String> merchant = words(merchantName);
        if (group.isEmpty() || merchant.isEmpty()) {
            return 0;
        }
        boolean[] used = new boolean[merchant.size()];
        double total = 0;
        int paired = 0;
        for (String word : group) {
            int best = -1;
            double bestWeight = 0;
            for (int i = 0; i < merchant.size(); i++) {
                double weight = used[i] ? 0 : weight(word, merchant.get(i));
                if (weight > bestWeight) {
                    bestWeight = weight;
                    best = i;
                }
            }
            if (best >= 0) {
                used[best] = true;
                total += bestWeight;
                paired++;
            }
        }
        return total / (group.size() + merchant.size() - paired);
    }

    /**
     * Splits a name into lowercase words of letters and digits; everything else separates words.
     *
     * @param name the name, may be {@code null}
     * @return the words, possibly empty
     */
    static List<String> words(String name) {
        List<String> words = new ArrayList<>();
        if (name == null) {
            return words;
        }
        for (String word : name.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    /**
     * The weight of pairing one group word with one merchant word.
     *
     * @param group    a group word
     * @param merchant a merchant word
     * @return 1 for the same word, 0.9 for a prefix, 0.75 for an abbreviation, otherwise 0
     */
    private static double weight(String group, String merchant) {
        if (group.equals(merchant)) {
            return EXACT;
        }
        if (Math.min(group.length(), merchant.length()) < MIN_PARTIAL_LENGTH) {
            return 0;
        }
        if (group.startsWith(merchant) || merchant.startsWith(group)) {
            return PREFIX;
        }
        return isAbbreviation(group, merchant) ? ABBREVIATION : 0;
    }

    /**
     * Whether {@code shorter}'s letters appear in order in {@code longer}, starting with the same
     * letter: {@code ctr} in {@code center}.
     *
     * @param shorter the possible abbreviation
     * @param longer  the full word
     * @return true when {@code shorter} abbreviates {@code longer}
     */
    private static boolean isAbbreviation(String shorter, String longer) {
        if (shorter.length() >= longer.length() || shorter.charAt(0) != longer.charAt(0)) {
            return false;
        }
        int next = 1;
        for (int i = 1; i < longer.length() && next < shorter.length(); i++) {
            if (longer.charAt(i) == shorter.charAt(next)) {
                next++;
            }
        }
        return next == shorter.length();
    }
}
