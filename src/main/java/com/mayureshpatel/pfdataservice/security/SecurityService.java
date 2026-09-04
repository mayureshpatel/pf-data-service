package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

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

    public boolean isAccountOwner(Long accountId, CustomUserDetails userDetails) {
        if (accountId == null || userDetails == null) return false;
        return accountRepository.findById(accountId)
                .map(account -> account.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }

    public boolean isTransactionOwner(Long transactionId, CustomUserDetails userDetails) {
        if (transactionId == null || userDetails == null) return false;
        return transactionRepository.findById(transactionId, userDetails.getId()).isPresent();
    }

    public boolean isCategoryOwner(Long categoryId, CustomUserDetails userDetails) {
        if (categoryId == null || userDetails == null) return false;
        return categoryRepository.findById(categoryId)
                .map(category -> category.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }

    public boolean isRuleOwner(Long ruleId, CustomUserDetails userDetails) {
        if (ruleId == null || userDetails == null) return false;
        return categoryRuleRepository.findById(ruleId)
                .map(rule -> rule.getUser().getId().equals(userDetails.getId()))
                .orElse(false);
    }

    public boolean isBudgetOwner(Long budgetId, CustomUserDetails userDetails) {
        if (budgetId == null || userDetails == null) return false;
        return budgetRepository.findById(budgetId)
                .map(budget -> budget.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }

    public boolean isRecurringTransactionOwner(Long recurringId, CustomUserDetails userDetails) {
        if (recurringId == null || userDetails == null) return false;
        return recurringTransactionRepository.findById(recurringId)
                .map(recurringTransaction -> recurringTransaction.getUserId().equals(userDetails.getId()))
                .orElse(false);
    }

    /**
     * Unlike this class's other owner checks, a merchant's {@code userId} can legitimately be
     * null (global merchants, e.g. "Whole Foods" -- shared reference data, not owned by any one
     * user). {@link Objects#equals} rather than {@code .equals()} so that case correctly resolves
     * to false instead of throwing.
     */
    public boolean isMerchantOwner(Long merchantId, CustomUserDetails userDetails) {
        if (merchantId == null || userDetails == null) return false;
        return merchantRepository.findById(merchantId)
                .map(merchant -> Objects.equals(merchant.getUserId(), userDetails.getId()))
                .orElse(false);
    }
}
