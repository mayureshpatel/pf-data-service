package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.AccountDto;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public AccountDto createAccount(Long userId, AccountDto accountDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Account account = new Account();
        account.setName(accountDto.name());
        account.setType(accountDto.type());
        account.setCurrentBalance(accountDto.currentBalance());
        account.setUser(user);

        return mapToDto(accountRepository.save(account));
    }

    @Transactional
    public AccountDto updateAccount(Long userId, Long accountId, AccountDto dto) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
             throw new RuntimeException("Access denied"); // Better to use AccessDeniedException
        }

        account.setName(dto.name());
        account.setType(dto.type());
        // We generally do NOT update balance directly here as it invalidates transaction history, 
        // but for a simple CRUD it might be allowed if the user wants to correct the starting balance.
        // Ideally, balance is derived or only initial balance is editable.
        // Assuming user knows what they are doing for now or this updates the "current" balance.
        account.setCurrentBalance(dto.currentBalance());

        return mapToDto(accountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        accountRepository.delete(account);
    }

    private AccountDto mapToDto(Account account) {
        return new AccountDto(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrentBalance()
        );
    }
}
