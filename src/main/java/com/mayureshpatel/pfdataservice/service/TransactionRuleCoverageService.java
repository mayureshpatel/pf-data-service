package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.rule.MatchableRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionRuleCoverageDto;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tells the ledger which of a page of transactions the user's saved rules already cover (PF-919),
 * so rows that still need a rule can be marked without a request per row.
 *
 * <p>"Covers" is decided by the shared {@link RuleMatcher}, the same matcher import, the match
 * previews and "Apply rules" use (case-insensitive keyword match, match any/all, amount range), so
 * this answer can never disagree with what applying the rules would do. Rules and transactions are
 * each loaded once per call, so the number of queries does not grow with the number of ids. An id
 * that does not exist or is not the user's is silently left out of the answer, because the
 * user-scoped transaction lookup skips it.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionRuleCoverageService {

    private final CategoryRuleRepository categoryRuleRepository;
    private final MerchantRuleRepository merchantRuleRepository;
    private final TransactionRepository transactionRepository;
    private final RuleMatcher ruleMatcher;

    /**
     * Reports, for each of the user's transactions among the given ids, whether a category rule
     * and whether a merchant rule covers it.
     *
     * @param userId         the signed-in user
     * @param transactionIds the ids of one page of transactions; null or empty returns an empty list
     * @return one entry per owned transaction found (ids that are not the user's are omitted)
     */
    public List<TransactionRuleCoverageDto> findCoverage(Long userId, List<Long> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return List.of();
        }
        List<CategoryRule> categoryRules = categoryRuleRepository.findByUserId(userId);
        List<MerchantRule> merchantRules = merchantRuleRepository.findByUserId(userId);

        return transactionRepository.findAllById(userId, transactionIds).stream()
                .map(transaction -> new TransactionRuleCoverageDto(
                        transaction.getId(),
                        coveredBy(transaction, categoryRules),
                        coveredBy(transaction, merchantRules)))
                .toList();
    }

    /** True if any of the rules matches the transaction's description and amount. */
    private boolean coveredBy(Transaction transaction, List<? extends MatchableRule> rules) {
        return rules.stream()
                .anyMatch(rule -> ruleMatcher.matches(transaction.getDescription(), transaction.getAmount(), rule));
    }
}
