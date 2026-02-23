package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("TransactionRepository Integration Tests")
class TransactionRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository currencyRepository;

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hash");
        return userRepository.save(user);
    }

    private Account createAccount(User user, String name) {
        AccountType type = new AccountType();
        type.setCode("CHECKING"); 
        try {
            accountTypeRepository.save(new AccountType("CHECKING", "Checking", null, true, 1, true, null));
        } catch (Exception e) {
            // Ignore if exists
        }
        
        com.mayureshpatel.pfdataservice.domain.currency.Currency currency = new com.mayureshpatel.pfdataservice.domain.currency.Currency();
        currency.setCode("USD");
        currency.setSymbol("$");
        currency.setName("US Dollar");
        try {
            currencyRepository.save(currency);
        } catch (Exception e) {
            // Ignore
        }

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

    private Category createCategory(User user, String name) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setType(CategoryType.EXPENSE);
        category.setIconography(new com.mayureshpatel.pfdataservice.domain.Iconography("icon", "color"));
        return categoryRepository.save(category);
    }
    
    private Merchant createMerchant(User user, String name) {
        Merchant merchant = new Merchant();
        merchant.setUser(user);
        merchant.setName(name);
        merchant.setOriginalName(name);
        return merchantRepository.save(merchant);
    }

    @Test
    @DisplayName("save() should persist transaction")
    void save_shouldPersistTransaction() {
        // Arrange
        User user = createUser("txuser");
        Account account = createAccount(user, "Tx Account");
        Merchant merchant = createMerchant(user, "Amazon");

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setMerchant(merchant);
        tx.setAmount(new BigDecimal("50.00"));
        tx.setTransactionDate(OffsetDateTime.now());
        tx.setDescription("Groceries");
        tx.setType(TransactionType.EXPENSE);

        // Act
        Transaction saved = transactionRepository.save(tx);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("findCategoryTotals() should aggregate correctly")
    void findCategoryTotals_shouldAggregate() {
        // Arrange
        User user = createUser("catuser");
        Account account = createAccount(user, "Cat Account");
        Category cat1 = createCategory(user, "Food");
        Category cat2 = createCategory(user, "Transport");
        Merchant merchant = createMerchant(user, "Generic");

        Transaction t1 = new Transaction();
        t1.setAccount(account);
        t1.setCategory(cat1);
        t1.setMerchant(merchant);
        t1.setAmount(new BigDecimal("100.00"));
        t1.setTransactionDate(OffsetDateTime.now());
        t1.setType(TransactionType.EXPENSE);
        transactionRepository.save(t1);

        Transaction t2 = new Transaction();
        t2.setAccount(account);
        t2.setCategory(cat1);
        t2.setMerchant(merchant);
        t2.setAmount(new BigDecimal("50.00"));
        t2.setTransactionDate(OffsetDateTime.now());
        t2.setType(TransactionType.EXPENSE);
        transactionRepository.save(t2);

        Transaction t3 = new Transaction();
        t3.setAccount(account);
        t3.setCategory(cat2);
        t3.setMerchant(merchant);
        t3.setAmount(new BigDecimal("20.00"));
        t3.setTransactionDate(OffsetDateTime.now());
        t3.setType(TransactionType.EXPENSE);
        transactionRepository.save(t3);

        OffsetDateTime start = LocalDate.now().minusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = LocalDate.now().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        // Act
        List<CategoryBreakdownDto> result = transactionRepository.findCategoryTotals(user.getId(), start, end);

        // Assert
        assertThat(result).hasSize(2);
        CategoryBreakdownDto food = result.stream().filter(c -> c.category().name().equals("Food")).findFirst().orElseThrow();
        assertThat(food.total()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("getNetFlowAfterDate() should calculate net flow")
    void getNetFlowAfterDate_shouldCalculateNetFlow() {
        // Arrange
        User user = createUser("flowuser");
        Account account = createAccount(user, "Flow Account");
        Merchant merchant = createMerchant(user, "Flow Merchant");

        // Income (+1000)
        Transaction t1 = new Transaction();
        t1.setAccount(account);
        t1.setMerchant(merchant);
        t1.setAmount(new BigDecimal("1000.00"));
        t1.setTransactionDate(OffsetDateTime.now());
        t1.setType(TransactionType.INCOME);
        transactionRepository.save(t1);

        // Expense (-200)
        Transaction t2 = new Transaction();
        t2.setAccount(account);
        t2.setMerchant(merchant);
        t2.setAmount(new BigDecimal("200.00")); 
        t2.setTransactionDate(OffsetDateTime.now());
        t2.setType(TransactionType.EXPENSE);
        transactionRepository.save(t2);

        // Act
        BigDecimal flow = transactionRepository.getNetFlowAfterDate(account.getId(), LocalDate.now().minusDays(1));

        // Assert
        // Net flow = Income - Expense
        // 1000 - 200 = 800
        assertThat(flow).isEqualByComparingTo("800.00");
    }
}
