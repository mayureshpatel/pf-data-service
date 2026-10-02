package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.tag.TagRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Backs the {@code @PreAuthorize("@ss.isXOwner(...)")} checks on controller methods -- registered
 * under the short bean name {@code ss} specifically so it's referenceable from that SpEL
 * expression. Each {@code isXOwner} method below answers the same question for a different entity
 * type: does the given id exist, and does it belong to the authenticated caller?
 */
@Service("ss")
@RequiredArgsConstructor
public class SecurityService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryRuleRepository categoryRuleRepository;
    private final BudgetRepository budgetRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final MerchantRepository merchantRepository;
    private final TagRepository tagRepository;

    /**
     * @param accountId the account to check
     * @param userDetails the authenticated caller
     * @return true if the account exists and belongs to {@code userDetails}; false if either
     *     argument is null or the account doesn't exist or belongs to someone else
     */
    public boolean isAccountOwner(Long accountId, CustomUserDetails userDetails) {
        if (accountId == null || userDetails == null) return false;
        return accountRepository.findById(accountId)
                .map(account -> account.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }

    /**
     * Unlike the other {@code isXOwner} checks, ownership here is enforced directly by the
     * repository's user-scoped {@link TransactionRepository#findById(Long, Long)} overload rather
     * than a fetch-then-compare.
     *
     * @param transactionId the transaction to check
     * @param userDetails the authenticated caller
     * @return true if the transaction exists and belongs to {@code userDetails}
     */
    public boolean isTransactionOwner(Long transactionId, CustomUserDetails userDetails) {
        if (transactionId == null || userDetails == null) return false;
        return transactionRepository.findById(transactionId, userDetails.getId()).isPresent();
    }

    /**
     * @param categoryId the category to check
     * @param userDetails the authenticated caller
     * @return true if the category exists and belongs to {@code userDetails}
     */
    public boolean isCategoryOwner(Long categoryId, CustomUserDetails userDetails) {
        if (categoryId == null || userDetails == null) return false;
        return categoryRepository.findById(categoryId)
                .map(category -> category.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }

    /**
     * Unlike its siblings here, {@link com.mayureshpatel.pfdataservice.domain.category.CategoryRule}
     * exposes ownership via a full {@code getUser()} association rather than a flat
     * {@code getUserId()}, so the comparison goes through {@code rule.getUser().getId()}.
     *
     * @param ruleId the category rule to check
     * @param userDetails the authenticated caller
     * @return true if the rule exists and belongs to {@code userDetails}
     */
    public boolean isRuleOwner(Long ruleId, CustomUserDetails userDetails) {
        if (ruleId == null || userDetails == null) return false;
        return categoryRuleRepository.findById(ruleId)
                .map(rule -> rule.getUser().getId().equals(userDetails.getId()))
                .orElse(false);
    }

    /**
     * @param budgetId the budget to check
     * @param userDetails the authenticated caller
     * @return true if the budget exists and belongs to {@code userDetails}
     */
    public boolean isBudgetOwner(Long budgetId, CustomUserDetails userDetails) {
        if (budgetId == null || userDetails == null) return false;
        return budgetRepository.findById(budgetId)
                .map(budget -> budget.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }

    /**
     * @param recurringId the recurring transaction to check
     * @param userDetails the authenticated caller
     * @return true if the recurring transaction exists and belongs to {@code userDetails}
     */
    public boolean isRecurringTransactionOwner(Long recurringId, CustomUserDetails userDetails) {
        if (recurringId == null || userDetails == null) return false;
        return recurringTransactionRepository.findById(recurringId)
                .map(recurringTransaction -> recurringTransaction.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }

    /**
     * @param merchantId the merchant to check
     * @param userDetails the authenticated caller
     * @return true if the merchant exists and belongs to {@code userDetails}
     */
    public boolean isMerchantOwner(Long merchantId, CustomUserDetails userDetails) {
        if (merchantId == null || userDetails == null) return false;
        return merchantRepository.findById(merchantId)
                .map(merchant -> merchant.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }

    /**
     * @param tagId the tag to check
     * @param userDetails the authenticated caller
     * @return true if the tag exists and belongs to {@code userDetails}
     */
    public boolean isTagOwner(Long tagId, CustomUserDetails userDetails) {
        if (tagId == null || userDetails == null) return false;
        return tagRepository.findById(tagId)
                .map(tag -> tag.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }
}
