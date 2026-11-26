package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.dto.transaction.DescriptionAmountDto;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Counts how much of a ledger each merchant rule matches (PF-981), with the same
 * {@link RuleMatcher} import uses, so a rule's counts mean exactly "the descriptions and
 * transactions this rule's keywords and amount range fit". A match is counted whether or not the
 * rule wins it; who wins is the overlap preview's job.
 */
@Component
@RequiredArgsConstructor
public class MerchantRuleMatchCounter {

    private final RuleMatcher ruleMatcher;

    /**
     * How many distinct descriptions and transactions one rule matches.
     *
     * @param descriptionCount distinct descriptions, case and spacing folded
     * @param transactionCount transactions
     */
    public record Counts(int descriptionCount, int transactionCount) {
    }

    /**
     * Counts each rule's matches over the given rows.
     *
     * @param rules the rules to count, each with an id
     * @param rows  the ledger, one row per transaction
     * @return each rule id mapped to its counts; every rule is present, with zeros when it matches nothing
     */
    public Map<Long, Counts> count(List<MerchantRule> rules, List<DescriptionAmountDto> rows) {
        // fold each description once instead of once per rule, for the distinct count
        int size = rows.size();
        String[] keys = new String[size];
        for (int i = 0; i < size; i++) {
            keys[i] = DescriptionKeys.fold(rows.get(i).description());
        }

        Map<Long, Counts> counts = new HashMap<>();
        for (MerchantRule rule : rules) {
            Set<String> descriptions = new HashSet<>();
            int transactions = 0;
            for (int i = 0; i < size; i++) {
                DescriptionAmountDto row = rows.get(i);
                // match the raw text, as import does; the folded key only dedupes descriptions
                if (ruleMatcher.matches(row.description(), row.amount(), rule)) {
                    transactions++;
                    descriptions.add(keys[i]);
                }
            }
            counts.put(rule.getId(), new Counts(descriptions.size(), transactions));
        }
        return counts;
    }
}
