package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.transaction.DescriptionGroupDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDescriptionDto;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Folds a flat list of descriptions into {@link DescriptionGroupDto}s for the merchants page's
 * Unlinked tab: one group per {@link DescriptionGroupKeys#key}, filtered by a search with optional
 * {@code -exclude} terms, with look-alike groups marked similar and ordered next to each other.
 * <p>
 * Grouping happens in memory over the user's whole description list (about a thousand rows for a
 * heavy user) because the group key is not something sql can index or page by; the caller pages
 * the result.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DescriptionGrouper {

    private static final Comparator<DescriptionGroupDto> BY_USAGE_THEN_KEY =
            Comparator.comparingLong(DescriptionGroupDto::transactionCount).reversed()
                    .thenComparing(DescriptionGroupDto::key);

    /**
     * Groups and filters the descriptions.
     * <p>
     * Similar keys are worked out before the search is applied, so "kroger -fuel" still shows that
     * fuel groups exist. The search is case-insensitive: every word not starting with {@code -}
     * must appear somewhere in the description (in any order), and no word of the description may
     * start with an exclude term (a word starting with {@code -}), so {@code -fuel} drops
     * "KROGER FUEL CTR" but not "BIOFUEL". Groups left with no variants drop out and the rest are
     * recounted.
     * <p>
     * Ordering: groups sharing a first word form a family; families sort by combined usage, then
     * groups within a family by usage, ties by key.
     *
     * @param descriptions the user's descriptions, most used first (that order is kept per group)
     * @param search       the search, or {@code null}/blank for none
     * @return the ordered groups
     */
    public static List<DescriptionGroupDto> group(List<TransactionDescriptionDto> descriptions, String search) {
        Map<String, List<TransactionDescriptionDto>> byKey = new LinkedHashMap<>();
        for (TransactionDescriptionDto description : descriptions) {
            byKey.computeIfAbsent(DescriptionGroupKeys.key(description.description()), k -> new ArrayList<>())
                    .add(description);
        }
        Map<String, Set<String>> similar = similarKeys(byKey.keySet());
        Search parsed = Search.parse(search);

        List<DescriptionGroupDto> groups = new ArrayList<>();
        for (Map.Entry<String, List<TransactionDescriptionDto>> entry : byKey.entrySet()) {
            List<TransactionDescriptionDto> kept = entry.getValue().stream().filter(parsed::matches).toList();
            if (kept.isEmpty()) {
                continue;
            }
            long total = kept.stream().mapToLong(TransactionDescriptionDto::transactionCount).sum();
            groups.add(new DescriptionGroupDto(
                    entry.getKey(),
                    DescriptionGroupKeys.displayName(kept.get(0).description()),
                    kept.size(),
                    total,
                    kept,
                    List.copyOf(similar.getOrDefault(entry.getKey(), Set.of()))));
        }
        return orderByFamily(groups);
    }

    private static Map<String, Set<String>> similarKeys(Set<String> keys) {
        Map<String, Set<String>> similar = new HashMap<>();
        for (String key : keys) {
            // every shorter whole-word prefix of this key that is itself a group is a look-alike
            int space = key.indexOf(' ');
            while (space > 0) {
                String prefix = key.substring(0, space);
                if (keys.contains(prefix)) {
                    similar.computeIfAbsent(key, k -> new TreeSet<>()).add(prefix);
                    similar.computeIfAbsent(prefix, k -> new TreeSet<>()).add(key);
                }
                space = key.indexOf(' ', space + 1);
            }
        }
        return similar;
    }

    private static List<DescriptionGroupDto> orderByFamily(List<DescriptionGroupDto> groups) {
        Map<String, Long> familyUsage = new HashMap<>();
        for (DescriptionGroupDto group : groups) {
            familyUsage.merge(family(group.key()), group.transactionCount(), Long::sum);
        }
        List<DescriptionGroupDto> ordered = new ArrayList<>(groups);
        ordered.sort(Comparator
                .comparing((DescriptionGroupDto g) -> familyUsage.get(family(g.key())), Comparator.reverseOrder())
                .thenComparing(g -> family(g.key()))
                .thenComparing(BY_USAGE_THEN_KEY));
        return ordered;
    }

    private static String family(String key) {
        int space = key.indexOf(' ');
        return space < 0 ? key : key.substring(0, space);
    }

    /** A parsed search: include words and exclude terms, all lowercase. */
    private record Search(List<String> includes, List<String> excludes) {

        static Search parse(String search) {
            if (search == null || search.isBlank()) {
                return new Search(List.of(), List.of());
            }
            List<String> words = new ArrayList<>();
            List<String> excludes = new ArrayList<>();
            for (String word : DescriptionKeys.fold(search).split(" ")) {
                if (word.length() > 1 && word.startsWith("-")) {
                    excludes.add(word.substring(1));
                } else {
                    words.add(word);
                }
            }
            return new Search(words, excludes);
        }

        boolean matches(TransactionDescriptionDto description) {
            String folded = DescriptionKeys.fold(description.description());
            // a leading space lets " term" match the start of any word, the first one included
            String wordStarts = " " + folded;
            return includes.stream().allMatch(folded::contains)
                    && excludes.stream().noneMatch(term -> wordStarts.contains(" " + term));
        }
    }
}
