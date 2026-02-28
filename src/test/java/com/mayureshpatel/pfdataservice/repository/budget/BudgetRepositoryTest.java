package com.mayureshpatel.pfdataservice.repository.budget;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("BudgetRepository Integration Tests")
class BudgetRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hash");
        return userRepository.save(user);
    }

    private Category createCategory(User user, String name) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setType(CategoryType.EXPENSE);
        category.setIconography(new com.mayureshpatel.pfdataservice.domain.Iconography("icon", "color"));
        return categoryRepository.save(category);
    }

    private Account createAccount(User user, String name) {
        try {
            accountTypeRepository.save(new AccountType("CHECKING", "Checking", null, true, 1, true, null));
        } catch (Exception e) { }
        
        com.mayureshpatel.pfdataservice.domain.currency.Currency currency = new com.mayureshpatel.pfdataservice.domain.currency.Currency();
        currency.setCode("USD");
        currency.setSymbol("$");
        currency.setName("US Dollar");
        try {
            currencyRepository.save(currency);
        } catch (Exception e) { }

        Account account = new Account();
        account.setUser(user);
        account.setName(name);
        AccountType t = new AccountType();
        t.setCode("CHECKING");
        account.setType(t);
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setCurrency(currency);
        account.setAudit(new com.mayureshpatel.pfdataservice.domain.TableAudit());
        return accountRepository.save(account);
    }

    private Merchant createMerchant(User user, String name) {
        Merchant merchant = new Merchant();
        merchant.setUser(user);
        merchant.setCleanName(name);
        merchant.setOriginalName(name);
        return merchantRepository.save(merchant);
    }

    @Test
    @DisplayName("findBudgetStatusByUserIdAndMonthAndYear() should aggregate budget and spending")
    void findBudgetStatus_shouldReturnStatus() {
        // Arrange
        User user = createUser("budgetuser");
        Category food = createCategory(user, "Food");
        Category rent = createCategory(user, "Rent"); // Unbudgeted
        Account account = createAccount(user, "Budget Account");
        Merchant merchant = createMerchant(user, "Shop");

        // 1. Create Budget for Food
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(food);
        budget.setAmount(new BigDecimal("500.00"));
        budget.setMonth(5);
        budget.setYear(2025);
        budgetRepository.save(budget);

        // 2. Spending in Food (Budgeted)
        Transaction t1 = new Transaction();
        t1.setAccount(account);
        t1.setCategory(food);
        t1.setMerchant(merchant);
        t1.setAmount(new BigDecimal("100.00"));
        t1.setTransactionDate(OffsetDateTime.parse("2025-05-10T10:00:00Z"));
        t1.setType(TransactionType.EXPENSE);
        transactionRepository.save(t1);

        // 3. Spending in Rent (Unbudgeted)
        Transaction t2 = new Transaction();
        t2.setAccount(account);
        t2.setCategory(rent);
        t2.setMerchant(merchant);
        t2.setAmount(new BigDecimal("1000.00"));
        t2.setTransactionDate(OffsetDateTime.parse("2025-05-15T10:00:00Z"));
        t2.setType(TransactionType.EXPENSE);
        transactionRepository.save(t2);

        // Act
        List<BudgetStatusDto> statuses = budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(user.getId(), 5, 2025);

        // Assert
        assertThat(statuses).hasSize(2);

        // Check Food (Budgeted)
        BudgetStatusDto foodStatus = statuses.stream()
                .filter(s -> s.category().name().equals("Food"))
                .findFirst().orElseThrow();
        assertThat(foodStatus.budgetedAmount()).isEqualByComparingTo("500.00");
        assertThat(foodStatus.spentAmount()).isEqualByComparingTo("100.00");
        assertThat(foodStatus.remainingAmount()).isEqualByComparingTo("400.00");

        // Check Rent (Unbudgeted)
        BudgetStatusDto rentStatus = statuses.stream()
                .filter(s -> s.category().name().equals("Rent"))
                .findFirst().orElseThrow();
        assertThat(rentStatus.budgetedAmount()).isEqualByComparingTo("0.00");
        assertThat(rentStatus.spentAmount()).isEqualByComparingTo("1000.00");
    }
}
