package com.mayureshpatel.pfdataservice.util;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountSnapshotRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import com.mayureshpatel.pfdataservice.repository.file_import_history.FileImportHistoryRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;

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
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private CategoryRuleRepository categoryRuleRepository;
    @Autowired private FileImportHistoryRepository fileImportHistoryRepository;
    @Autowired private AccountSnapshotRepository accountSnapshotRepository;
    @Autowired private RecurringTransactionRepository recurringTransactionRepository;

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

    public Budget createBudget(User user, Category category, BigDecimal amount, int month, int year) {
        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .amount(amount)
                .month(month)
                .year(year)
                .audit(new SoftDeleteAudit())
                .build();
        return budgetRepository.save(budget);
    }

    public CategoryRule createCategoryRule(User user, Category category, String keyword, int priority) {
        CategoryRule rule = new CategoryRule();
        rule.setUser(user);
        rule.setCategory(category);
        rule.setKeyword(keyword);
        rule.setPriority(priority);
        rule.setAudit(new TimestampAudit());
        return categoryRuleRepository.save(rule);
    }

    public FileImportHistory createFileImportHistory(Account account, String fileName, String fileHash, int transactionCount) {
        FileImportHistory history = new FileImportHistory();
        history.setAccount(account);
        history.setFileName(fileName);
        history.setFileHash(fileHash);
        history.setTransactionCount(transactionCount);
        history.setAudit(new CreatedAtAudit());
        return fileImportHistoryRepository.save(history);
    }

    public AccountSnapshot createAccountSnapshot(Account account, LocalDate snapshotDate, BigDecimal balance) {
        AccountSnapshot snapshot = new AccountSnapshot();
        snapshot.setAccount(account);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setBalance(balance);
        snapshot.setAudit(new CreatedAtAudit());
        return accountSnapshotRepository.save(snapshot);
    }

    public RecurringTransaction createRecurringTransaction(User user, Account account, Merchant merchant, BigDecimal amount, Frequency frequency, LocalDate nextDate) {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setUser(user);
        rt.setAccount(account);
        rt.setMerchant(merchant);
        rt.setAmount(amount);
        rt.setFrequency(frequency);
        rt.setNextDate(nextDate);
        rt.setActive(true);
        rt.setAudit(new SoftDeleteAudit());
        return recurringTransactionRepository.save(rt);
    }
}
