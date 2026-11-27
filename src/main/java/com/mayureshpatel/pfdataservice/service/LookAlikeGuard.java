package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.LookAlikeWarningDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Warns before descriptions are linked to a merchant whose name they only partly fit (PF-983): the
 * case of linking "KROGER FUEL CTR #1" to Kroger while Kroger Fuel Center exists.
 * <p>
 * Each selected description is reduced to its group key ({@link DescriptionGroupKeys#key}), which
 * already drops store numbers and the city after them, and split into words. A word is extra when
 * no word of the target merchant's name fits it ({@link MerchantNameSimilarity#fits}); words under
 * three letters are ignored. A group with extra words is flagged only when those words point
 * somewhere else:
 * <ul>
 *     <li>another of the user's merchants has a word fitting them and fits the group better than
 *     the target does ({@link MerchantNameSimilarity#score}), or</li>
 *     <li>the selection also holds a shorter look-alike group the flagged one only adds whole words
 *     to ("KROGER" next to "KROGER FUEL CTR"), the same pairing the Unlinked tab marks as similar.</li>
 * </ul>
 * A group whose words all fit the merchant ("KROGER #431" for Kroger) is never flagged, and nor is
 * one with extra words that nothing else claims, so a deliberate link draws no false alarm. Only
 * words the descriptions add count: "KROGER #431" linked to Kroger Fuel Center is not flagged for
 * lacking FUEL.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class LookAlikeGuard {

    /** Shorter words ("of", "us") are too common to say anything about the merchant. */
    private static final int MIN_WORD_LENGTH = 3;

    /**
     * Checks the descriptions about to be linked to a merchant.
     *
     * @param target       the merchant the descriptions would be linked to
     * @param merchants    every merchant the user has; the target among them is skipped
     * @param descriptions the raw descriptions, in the order the warnings keep within a group
     * @return one warning per flagged group, most descriptions first, then by group name
     */
    public static List<LookAlikeWarningDto> check(Merchant target, List<Merchant> merchants, List<String> descriptions) {
        Map<String, List<String>> byKey = groupByKey(descriptions);
        List<String> targetWords = MerchantNameSimilarity.words(target.getName());
        // split every other merchant's name once, not once per group and word
        List<Named> others = merchants.stream()
                .filter(merchant -> !Objects.equals(merchant.getId(), target.getId()))
                .map(merchant -> new Named(merchant, MerchantNameSimilarity.words(merchant.getName())))
                .toList();

        List<LookAlikeWarningDto> warnings = new ArrayList<>();
        for (String key : byKey.keySet()) {
            LookAlikeWarningDto warning = warningFor(key, byKey, targetWords, target, others);
            if (warning != null) {
                warnings.add(warning);
            }
        }
        warnings.sort(Comparator
                .comparingInt((LookAlikeWarningDto w) -> w.descriptions().size()).reversed()
                .thenComparing(LookAlikeWarningDto::groupName, String.CASE_INSENSITIVE_ORDER));
        return warnings;
    }

    /**
     * Groups the descriptions by their group key, keeping their order; descriptions with an empty
     * key are left out.
     *
     * @param descriptions the raw descriptions
     * @return the descriptions by group key, in the order each key first appears
     */
    private static Map<String, List<String>> groupByKey(List<String> descriptions) {
        Map<String, List<String>> byKey = new LinkedHashMap<>();
        for (String description : descriptions) {
            String key = DescriptionGroupKeys.key(description);
            if (!key.isEmpty()) {
                byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(description);
            }
        }
        return byKey;
    }

    /**
     * The warning for one group, if any of its extra words are flagged.
     *
     * @param key         the group key
     * @param byKey       every selected description by group key
     * @param targetWords the words of the target's name
     * @param target      the target merchant
     * @param others      the user's other merchants, with their names already split into words
     * @return the group's warning, or {@code null} when nothing in it is flagged
     */
    private static LookAlikeWarningDto warningFor(
            String key, Map<String, List<String>> byKey, List<String> targetWords, Merchant target, List<Named> others) {
        List<String> keyWords = MerchantNameSimilarity.words(key);
        List<String> extras = keyWords.stream()
                .filter(word -> word.length() >= MIN_WORD_LENGTH)
                .filter(word -> targetWords.stream().noneMatch(t -> MerchantNameSimilarity.fits(word, t)))
                .toList();
        if (extras.isEmpty()) {
            return null;
        }

        List<Named> rivals = rivals(key, extras, target, others);
        Set<String> flagged = new LinkedHashSet<>();
        extras.stream()
                .filter(extra -> rivals.stream().anyMatch(rival -> rival.hasFittingWord(extra)))
                .forEach(flagged::add);
        String nextTo = shorterLookAlike(key, byKey.keySet());
        if (nextTo != null) {
            List<String> added = MerchantNameSimilarity.words(key.substring(nextTo.length()));
            extras.stream().filter(added::contains).forEach(flagged::add);
        }
        if (flagged.isEmpty()) {
            return null;
        }

        List<String> groupDescriptions = byKey.get(key);
        List<String> words = keyWords.stream()
                .filter(flagged::contains)
                .distinct()
                .map(word -> word.toUpperCase(Locale.ROOT))
                .toList();
        List<MerchantDto> rivalDtos = rivals.stream()
                .filter(rival -> flagged.stream().anyMatch(rival::hasFittingWord))
                .map(rival -> MerchantDtoMapper.toDto(rival.merchant()))
                .toList();
        return new LookAlikeWarningDto(
                DescriptionGroupKeys.displayName(groupDescriptions.get(0)),
                words,
                List.copyOf(groupDescriptions),
                rivalDtos,
                nextTo == null ? null : DescriptionGroupKeys.displayName(byKey.get(nextTo).get(0)));
    }

    /**
     * The user's other merchants that fit a group better than the target and share one of its
     * extra words, best fit first, then by name.
     *
     * @param key    the group key
     * @param extras the group's words the target's name lacks
     * @param target the target merchant
     * @param others the user's other merchants, with their names already split into words
     * @return the competing merchants, possibly empty
     */
    private static List<Named> rivals(String key, List<String> extras, Merchant target, List<Named> others) {
        double targetScore = MerchantNameSimilarity.score(key, target.getName());
        record Scored(Named named, double score) {
        }
        return others.stream()
                .filter(named -> extras.stream().anyMatch(named::hasFittingWord))
                .map(named -> new Scored(named, MerchantNameSimilarity.score(key, named.merchant().getName())))
                .filter(scored -> scored.score() > targetScore)
                .sorted(Comparator.comparingDouble(Scored::score).reversed()
                        .thenComparing(scored -> scored.named().merchant().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(Scored::named)
                .toList();
    }

    /**
     * A merchant with its name split into words once per check.
     *
     * @param merchant the merchant
     * @param words    the lowercase words of its name
     */
    private record Named(Merchant merchant, List<String> words) {

        /**
         * Whether any word of the merchant's name fits the given word.
         *
         * @param word a lowercase word
         * @return true when the merchant's name has a fitting word
         */
        boolean hasFittingWord(String word) {
            return words.stream().anyMatch(own -> MerchantNameSimilarity.fits(word, own));
        }
    }

    /**
     * The longest other selected group key that this key only adds whole words to.
     *
     * @param key  the group key
     * @param keys every selected group key
     * @return that shorter key, or {@code null} when none is selected
     */
    private static String shorterLookAlike(String key, Set<String> keys) {
        String best = null;
        for (String other : keys) {
            if (key.startsWith(other + " ") && (best == null || other.length() > best.length())) {
                best = other;
            }
        }
        return best;
    }
}
