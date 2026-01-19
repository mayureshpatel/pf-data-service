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

    private AccountDto mapToDto(Account account) {
        return new AccountDto(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrentBalance()
        );
    }
}
