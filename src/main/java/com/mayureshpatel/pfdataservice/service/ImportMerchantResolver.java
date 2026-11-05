package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Chooses the merchant for each row of a CSV import (PF-906), in a fixed order: the row
 * description's exact link first (today's behavior, {@code merchant_description_links}), then the
 * highest-ranked of the user's merchant rules that matches the row, otherwise none. An exact link
 * always beats a rule. Lookup-only: a rule never creates a merchant or a description link, and
 * nothing here teaches the app anything. Extracted from {@link TransactionImportService} to keep
 * that class from growing.
 *
 * <p>Matching keys, pinned by tests: links go through {@link MerchantService}, which folds each
 * raw description exactly as it does when a link is recorded; rules are matched by the shared
 * {@link RuleMatcher} against the raw description, like every other rule consumer. Rules are loaded
 * once per call and links in one batch, so the query count does not grow with the number of rows.
 * {@link MerchantRuleRepository#findByUserId} already returns rules in rank order (priority
 * descending, summed keyword length descending, id ascending; the same order as
 * {@code RuleOrdering}), so the first matching rule wins.
 */
@Component
@RequiredArgsConstructor
public class ImportMerchantResolver {

    private final MerchantService merchantService;
    private final MerchantRuleRepository merchantRuleRepository;
    private final RuleMatcher ruleMatcher;

    /**
     * What a rule needs to know about one import row. Rule results are per row, not per
     * description, because an amount-range rule can treat two rows with one description differently.
     *
     * @param description the row's raw description
     * @param amount      the row's amount, may be null
     */
    public record ImportRow(String description, BigDecimal amount) {
    }

    /**
     * Resolves each row to a merchant (the import preview's suggestion).
     *
     * @param userId the signed-in user; only their links and rules are read
     * @param rows   the rows, in file order
     * @return one entry per row, in the same order; a null entry means no merchant
     */
    public List<Merchant> resolveMerchants(Long userId, List<ImportRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, Merchant> links = merchantService.findMatchingMerchants(userId, descriptionsOf(rows));
        return resolve(userId, rows, links, Function.identity());
    }

    /**
     * Resolves each row to a merchant id (the legacy save path, which stores ids only).
     *
     * @param userId the signed-in user; only their links and rules are read
     * @param rows   the rows, in save order
     * @return one entry per row, in the same order; a null entry means no merchant
     */
    public List<Long> resolveMerchantIds(Long userId, List<ImportRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, Long> links = merchantService.findMatchingMerchantIds(userId, descriptionsOf(rows));
        return resolve(userId, rows, links, Merchant::getId);
    }

    /**
     * Verifies that every given merchant id belongs to the user; see
     * {@link MerchantService#requireOwnedMerchants}.
     *
     * @param userId      the user id
     * @param merchantIds the merchant ids to check; may contain nulls
     */
    public void requireOwnedMerchants(Long userId, Collection<Long> merchantIds) {
        merchantService.requireOwnedMerchants(userId, merchantIds);
    }

    /**
     * Link first, then rule, per row. The rules are only loaded when at least one row has no link.
     */
    private <T> List<T> resolve(Long userId, List<ImportRow> rows, Map<String, T> links, Function<Merchant, T> fromRule) {
        List<MerchantRule> rules = null;
        List<T> resolved = new ArrayList<>(rows.size());
        for (ImportRow row : rows) {
            T match = links.get(row.description());
            if (match == null) {
                if (rules == null) {
                    rules = merchantRuleRepository.findByUserId(userId);
                }
                match = firstMatchingRuleMerchant(rules, row, fromRule);
            }
            resolved.add(match);
        }
        return resolved;
    }

    /**
     * The merchant of the first rule (in rank order) that matches the row. A rule whose merchant
     * is missing is skipped, never an error, and matching carries on with the next rule.
     */
    private <T> T firstMatchingRuleMerchant(List<MerchantRule> rules, ImportRow row, Function<Merchant, T> fromRule) {
        MerchantRule rule = firstMatchingRule(rules, row);
        return rule == null ? null : fromRule.apply(rule.getMerchant());
    }

    /**
     * The first rule (in rank order) that matches the row and points at a merchant, or null. Shared
     * with applying rules to existing transactions (PF-907) so both pick winners identically.
     *
     * @param rules the user's rules in rank order, as {@link MerchantRuleRepository#findByUserId} returns them
     * @param row   the description and amount to match
     * @return the winning rule, or null when none matches
     */
    public MerchantRule firstMatchingRule(List<MerchantRule> rules, ImportRow row) {
        for (MerchantRule rule : rules) {
            Merchant merchant = rule.getMerchant();
            if (merchant != null && merchant.getId() != null && ruleMatcher.matches(row.description(), row.amount(), rule)) {
                return rule;
            }
        }
        return null;
    }

    /** The rows' raw descriptions, in row order. */
    private static List<String> descriptionsOf(List<ImportRow> rows) {
        return rows.stream().map(ImportRow::description).toList();
    }
}
