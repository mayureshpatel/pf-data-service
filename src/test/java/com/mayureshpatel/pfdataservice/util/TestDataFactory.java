package com.mayureshpatel.pfdataservice.util;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TimestampAudit;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.CreatedAtAudit;
import com.mayureshpatel.pfdataservice.domain.SoftDeleteAudit;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A helper factory for creating test data.
 * Useful for Integration Tests to quickly seed the database with valid, related entities.
 */
@Component
public class TestDataFactory {

    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountTypeRepository accountTypeRepository;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MerchantRepository merchantRepository;
    @Autowired private TransactionRepository transactionRepository;

    public User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hash_" + username);
        return userRepository.save(user);
    }

    public AccountType ensureAccountType(String code) {
        // Simple check/save mostly for test isolation
        // In real apps, these might be Pre-loaded via Flyway
        try {
            return accountTypeRepository.save(new AccountType(code, code, null, true, 1, true, new TimestampAudit()));
        } catch (Exception e) {
            // Assume exists
            return new AccountType(code, code, null, true, 1, true, new TimestampAudit());
        }
    }

    public Currency ensureCurrency(String code) {
        try {
            return currencyRepository.save(new Currency(code, code + " Name", "$", true, new CreatedAtAudit()));
        } catch (Exception e) {
            return new Currency(code, code + " Name", "$", true, new CreatedAtAudit());
        }
    }

    public Account createAccount(User user, String name) {
        AccountType type = ensureAccountType("CHECKING");
        Currency currency = ensureCurrency("USD");

        Account account = new Account();
        account.setUser(user);
        account.setName(name);
        account.setType(type);
        account.setCurrency(currency);
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setAudit(new TableAudit());
        
        return accountRepository.save(account);
    }

    public Category createCategory(User user, String name, CategoryType type) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setType(type);
        category.setIconography(new Iconography("icon", "color"));
        category.setAudit(new TimestampAudit());
        return categoryRepository.save(category);
    }

    public Merchant createMerchant(User user, String name) {
        Merchant merchant = new Merchant();
        merchant.setUser(user);
        merchant.setCleanName(name);
        merchant.setOriginalName(name);
        merchant.setAudit(new TimestampAudit());
        return merchantRepository.save(merchant);
    }

    public Transaction createTransaction(Account account, Merchant merchant, Category category, BigDecimal amount, OffsetDateTime date, TransactionType type) {
        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setMerchant(merchant);
        tx.setCategory(category);
        tx.setAmount(amount);
        tx.setTransactionDate(date);
        tx.setType(type);
        tx.setDescription("Test Transaction " + UUID.randomUUID().toString().substring(0, 8));
        tx.setAudit(new SoftDeleteAudit());
        
        return transactionRepository.save(tx);
    }
}
