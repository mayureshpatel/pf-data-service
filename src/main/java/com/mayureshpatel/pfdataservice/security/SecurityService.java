package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("ss")
@RequiredArgsConstructor
public class SecurityService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryRuleRepository categoryRuleRepository;

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
}
