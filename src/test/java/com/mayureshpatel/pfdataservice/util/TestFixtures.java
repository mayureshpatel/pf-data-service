package com.mayureshpatel.pfdataservice.util;

import com.mayureshpatel.pfdataservice.domain.CreatedAtAudit;
import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.SoftDeleteAudit;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.TimestampAudit;
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
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Provides pre-built domain objects for unit tests (no database interaction).
 * Use {@link TestDataFactory} for integration tests that need persisted entities.
 */
public final class TestFixtures {

    public static final Long USER_ID = 1L;
    public static final Long ACCOUNT_ID = 10L;
    public static final Long TRANSACTION_ID = 100L;
    public static final Long CATEGORY_ID = 50L;
    public static final Long MERCHANT_ID = 30L;
    public static final Long BUDGET_ID = 60L;

    private TestFixtures() {}

    // ---- User ----

    public static User aUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setAudit(new TableAudit());
        return user;
    }

    public static User aUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setAudit(new TableAudit());
        return user;
    }

    // ---- Account ----

    public static Account anAccount() {
        return anAccount(aUser());
    }

    public static Account anAccount(User user) {
        Account account = new Account();
        account.setId(ACCOUNT_ID);
        account.setUser(user);
        account.setName("Test Checking");
        account.setType(anAccountType());
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setCurrency(aCurrency());
        account.setVersion(1L);
        account.setBankName(null);
        account.setAudit(new TableAudit());
        return account;
    }

    public static Account anAccount(Long id, User user, BigDecimal balance) {
        Account account = anAccount(user);
        account.setId(id);
        account.setCurrentBalance(balance);
        return account;
    }

    // ---- AccountType ----

    public static AccountType anAccountType() {
        return new AccountType("CHECKING", "Checking", null, true, 1, true, new TimestampAudit());
    }

    // ---- Currency ----

    public static Currency aCurrency() {
        return new Currency("USD", "US Dollar", "$", true, new CreatedAtAudit());
    }

    // ---- Category ----

    public static Category aCategory() {
        return aCategory(aUser());
    }

    public static Category aCategory(User user) {
        return Category.builder()
                .id(CATEGORY_ID)
                .user(user)
                .name("Groceries")
                .type(CategoryType.EXPENSE)
                .parent(null)
                .iconography(new Iconography("cart", "#00FF00"))
                .audit(new TimestampAudit())
                .build();
    }

    public static Category aCategory(Long id, User user, String name, CategoryType type) {
        return Category.builder()
                .id(id)
                .user(user)
                .name(name)
                .type(type)
                .parent(null)
                .iconography(new Iconography("icon", "#000000"))
                .audit(new TimestampAudit())
                .build();
    }

    // ---- Merchant ----

    public static Merchant aMerchant() {
        return aMerchant(aUser());
    }

    public static Merchant aMerchant(User user) {
        Merchant merchant = new Merchant();
        merchant.setId(MERCHANT_ID);
        merchant.setUser(user);
        merchant.setOriginalName("KROGER #431");
        merchant.setCleanName("Kroger");
        merchant.setAudit(new TimestampAudit());
        return merchant;
    }

    // ---- Transaction ----

    public static Transaction aTransaction() {
        return aTransaction(anAccount(), aCategory(), aMerchant());
    }

    public static Transaction aTransaction(Account account) {
        return aTransaction(account, aCategory(account.getUser()), aMerchant(account.getUser()));
    }

    public static Transaction aTransaction(Account account, Category category, Merchant merchant) {
        Transaction tx = new Transaction();
        tx.setId(TRANSACTION_ID);
        tx.setAccount(account);
        tx.setCategory(category);
        tx.setAmount(new BigDecimal("50.00"));
        tx.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        tx.setPostDate(null);
        tx.setDescription("Test Transaction");
        tx.setMerchant(merchant);
        tx.setType(TransactionType.EXPENSE);
        tx.setAudit(new SoftDeleteAudit());
        return tx;
    }

    public static Transaction aTransaction(Long id, Account account, BigDecimal amount, TransactionType type) {
        Transaction tx = aTransaction(account);
        tx.setId(id);
        tx.setAmount(amount);
        tx.setType(type);
        return tx;
    }

    // ---- Budget ----

    public static Budget aBudget() {
        return aBudget(aUser(), aCategory());
    }

    public static Budget aBudget(User user, Category category) {
        return Budget.builder()
                .id(BUDGET_ID)
                .user(user)
                .category(category)
                .amount(new BigDecimal("500.00"))
                .month(1)
                .year(2026)
                .audit(new SoftDeleteAudit())
                .build();
    }

    // ---- RecurringTransaction ----

    public static RecurringTransaction aRecurringTransaction() {
        return aRecurringTransaction(aUser(), anAccount(), aMerchant());
    }

    public static RecurringTransaction aRecurringTransaction(User user, Account account, Merchant merchant) {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setId(70L);
        rt.setUser(user);
        rt.setAccount(account);
        rt.setMerchant(merchant);
        rt.setAmount(new BigDecimal("15.99"));
        rt.setFrequency(Frequency.MONTHLY);
        rt.setLastDate(LocalDate.of(2026, 1, 15));
        rt.setNextDate(LocalDate.of(2026, 2, 15));
        rt.setActive(true);
        rt.setAudit(new SoftDeleteAudit());
        return rt;
    }

    // ---- CategoryRule ----

    public static CategoryRule aCategoryRule(User user, Category category) {
        CategoryRule rule = new CategoryRule();
        rule.setId(80L);
        rule.setUser(user);
        rule.setKeyword("KROGER");
        rule.setPriority(1);
        rule.setCategory(category);
        rule.setAudit(new TimestampAudit());
        return rule;
    }

    // ---- AccountSnapshot ----

    public static AccountSnapshot anAccountSnapshot(Account account) {
        AccountSnapshot snapshot = new AccountSnapshot();
        snapshot.setId(90L);
        snapshot.setAccount(account);
        snapshot.setSnapshotDate(LocalDate.of(2026, 1, 31));
        snapshot.setBalance(account.getCurrentBalance());
        snapshot.setAudit(new CreatedAtAudit());
        return snapshot;
    }

    // ---- FileImportHistory ----

    public static FileImportHistory aFileImportHistory(Account account) {
        FileImportHistory history = new FileImportHistory();
        history.setId(95L);
        history.setAccount(account);
        history.setFileName("transactions.csv");
        history.setFileHash("abc123def456");
        history.setTransactionCount(25);
        history.setAudit(new CreatedAtAudit());
        return history;
    }
}
