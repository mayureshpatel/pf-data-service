package com.mayureshpatel.pfdataservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("ss")
@RequiredArgsConstructor
public class SecurityService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public boolean isAccountOwner(Long accountId, CustomUserDetails userDetails) {
        if (accountId == null || userDetails == null) return false;
        return accountRepository.findById(accountId)
                .map(account -> account.getUser().getId().equals(userDetails.getId()))
                .orElse(false);
    }

    public boolean isTransactionOwner(Long transactionId, CustomUserDetails userDetails) {
        if (transactionId == null || userDetails == null) return false;
        return transactionRepository.findById(transactionId)
                .map(t -> t.getAccount().getUser().getId().equals(userDetails.getId()))
                .orElse(false);
    }
}
