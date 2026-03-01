package com.mayureshpatel.pfdataservice.util;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.TimestampAudit;
import com.mayureshpatel.pfdataservice.domain.SoftDeleteAudit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class TestFixtures {

    private TestFixtures() {}

    public static User aUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");
        return user;
    }

    public static Account anAccount() {
        return anAccount(aUser());
    }

    public static Account anAccount(User user) {
        Account account = new Account();
        account.setId(1L);
        account.setUser(user);
        account.setName("Test Account");
        account.setCurrentBalance(new BigDecimal("1000.00"));
        
        AccountType type = new AccountType();
        type.setCode("CHECKING");
        type.setLabel("Checking Account");
        account.setType(type);
        
        Currency currency = new Currency();
        currency.setCode("USD");
        currency.setSymbol("$");
        account.setCurrency(currency);
        
        account.setAudit(new TableAudit());
        return account;
    }

    public static AccountType anAccountType() {
        AccountType type = new AccountType();
        type.setCode("SAVINGS");
        type.setLabel("Savings Account");
        return type;
    }

    public static Category aCategory() {
        return aCategory(aUser());
    }

    public static Category aCategory(User user) {
        Category category = new Category();
        category.setId(1L);
        category.setUser(user);
        category.setName("Groceries");
        category.setType(CategoryType.EXPENSE);
        category.setIconography(new Iconography("shopping_cart", "#FF0000"));
        category.setAudit(new TimestampAudit());
        return category;
    }

    public static CategoryRule aCategoryRule(User user, Category category) {
        CategoryRule rule = new CategoryRule();
        rule.setId(1L);
        rule.setUser(user);
        rule.setCategory(category);
        rule.setKeyword("AMZN");
        rule.setPriority(10);
        return rule;
    }

    public static Merchant aMerchant() {
        return aMerchant(aUser());
    }

    public static Merchant aMerchant(User user) {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setUser(user);
        merchant.setCleanName("Amazon");
        merchant.setOriginalName("AMZN MKTP US");
        return merchant;
    }

    public static Transaction aTransaction() {
        return aTransaction(anAccount(), aMerchant(), aCategory());
    }

    public static Transaction aTransaction(Account account, Merchant merchant, Category category) {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setAccount(account);
        tx.setMerchant(merchant);
        tx.setCategory(category);
        tx.setAmount(new BigDecimal("50.00"));
        tx.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        tx.setType(TransactionType.EXPENSE);
        tx.setDescription("Groceries at Amazon");
        tx.setAudit(new SoftDeleteAudit());
        return tx;
    }

    public static Budget aBudget() {
        User user = aUser();
        Category category = aCategory(user);
        Budget budget = new Budget();
        budget.setId(1L);
        budget.setUser(user);
        budget.setCategory(category);
        budget.setAmount(new BigDecimal("500.00"));
        budget.setMonth(1);
        budget.setYear(2025);
        budget.setAudit(new SoftDeleteAudit());
        return budget;
    }

    public static RecurringTransaction aRecurringTransaction() {
        User user = aUser();
        Account account = anAccount(user);
        Merchant merchant = aMerchant(user);
        RecurringTransaction rt = new RecurringTransaction();
        rt.setId(1L);
        rt.setUser(user);
        rt.setAccount(account);
        rt.setMerchant(merchant);
        rt.setAmount(new BigDecimal("15.99"));
        rt.setFrequency(Frequency.MONTHLY);
        rt.setNextDate(LocalDate.now().plusMonths(1));
        rt.setActive(true);
        rt.setAudit(new SoftDeleteAudit());
        return rt;
    }
}
