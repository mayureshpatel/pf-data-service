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

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.dto.transaction.CategoryTransactionsDto;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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
    private CurrencyRepository currencyRepository;

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
        merchant.setCleanName(name);
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
        tx.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
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
        t1.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t1.setType(TransactionType.EXPENSE);
        transactionRepository.save(t1);

        Transaction t2 = new Transaction();
        t2.setAccount(account);
        t2.setCategory(cat1);
        t2.setMerchant(merchant);
        t2.setAmount(new BigDecimal("50.00"));
        t2.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t2.setType(TransactionType.EXPENSE);
        transactionRepository.save(t2);

        Transaction t3 = new Transaction();
        t3.setAccount(account);
        t3.setCategory(cat2);
        t3.setMerchant(merchant);
        t3.setAmount(new BigDecimal("20.00"));
        t3.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t3.setType(TransactionType.EXPENSE);
        transactionRepository.save(t3);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime start = now.minusDays(2);
        OffsetDateTime end = now.plusDays(2);

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
        t1.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t1.setType(TransactionType.INCOME);
        transactionRepository.save(t1);

        // Expense (-200)
        Transaction t2 = new Transaction();
        t2.setAccount(account);
        t2.setMerchant(merchant);
        t2.setAmount(new BigDecimal("200.00")); 
        t2.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t2.setType(TransactionType.EXPENSE);
        transactionRepository.save(t2);

        // Act
        BigDecimal flow = transactionRepository.getNetFlowAfterDate(account.getId(), LocalDate.now().minusDays(1));

        // Assert
        // Net flow = Income - Expense
        // 1000 - 200 = 800
        assertThat(flow).isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("findById() should return transaction when exists")
    void findById_shouldReturnTransaction() {
        // Arrange
        User user = createUser("finduser");
        Account account = createAccount(user, "Find Account");
        Merchant merchant = createMerchant(user, "FindMerchant");

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setMerchant(merchant);
        tx.setAmount(new BigDecimal("25.00"));
        tx.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        tx.setDescription("Find me");
        tx.setType(TransactionType.EXPENSE);
        Transaction saved = transactionRepository.save(tx);

        // Act
        Optional<Transaction> found = transactionRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Find me");
        assertThat(found.get().getAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("findById() should return empty when not exists")
    void findById_shouldReturnEmpty() {
        assertThat(transactionRepository.findById(99999L)).isEmpty();
    }

    @Test
    @DisplayName("getCountByCategory() should return category transaction counts")
    void getCountByCategory_shouldReturnCounts() {
        // Arrange
        User user = createUser("countuser");
        Account account = createAccount(user, "Count Account");
        Category food = createCategory(user, "Food Count");
        Category transport = createCategory(user, "Transport Count");
        Merchant merchant = createMerchant(user, "CountMerchant");

        // 2 food transactions
        for (int i = 0; i < 2; i++) {
            Transaction tx = new Transaction();
            tx.setAccount(account);
            tx.setCategory(food);
            tx.setMerchant(merchant);
            tx.setAmount(new BigDecimal("10.00"));
            tx.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
            tx.setType(TransactionType.EXPENSE);
            transactionRepository.save(tx);
        }

        // 1 transport transaction
        Transaction tx3 = new Transaction();
        tx3.setAccount(account);
        tx3.setCategory(transport);
        tx3.setMerchant(merchant);
        tx3.setAmount(new BigDecimal("30.00"));
        tx3.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        tx3.setType(TransactionType.EXPENSE);
        transactionRepository.save(tx3);

        // Act
        List<CategoryTransactionsDto> counts = transactionRepository.getCountByCategory(user.getId());

        // Assert
        assertThat(counts).hasSizeGreaterThanOrEqualTo(2);
        CategoryTransactionsDto foodCount = counts.stream()
                .filter(c -> c.category().name().equals("Food Count"))
                .findFirst().orElseThrow();
        assertThat(foodCount.transactionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("getSumByDateRange() should sum amounts by type within range")
    void getSumByDateRange_shouldSumByType() {
        // Arrange
        User user = createUser("sumuser");
        Account account = createAccount(user, "Sum Account");
        Merchant merchant = createMerchant(user, "SumMerchant");

        Transaction t1 = new Transaction();
        t1.setAccount(account);
        t1.setMerchant(merchant);
        t1.setAmount(new BigDecimal("500.00"));
        t1.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t1.setType(TransactionType.INCOME);
        transactionRepository.save(t1);

        Transaction t2 = new Transaction();
        t2.setAccount(account);
        t2.setMerchant(merchant);
        t2.setAmount(new BigDecimal("300.00"));
        t2.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t2.setType(TransactionType.INCOME);
        transactionRepository.save(t2);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime start = now.minusDays(2);
        OffsetDateTime end = now.plusDays(2);

        // Act
        BigDecimal sum = transactionRepository.getSumByDateRange(user.getId(), start, end, TransactionType.INCOME);

        // Assert
        assertThat(sum).isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("data isolation - user cannot see other user's transactions")
    void dataIsolation_userCannotSeeOtherTransactions() {
        // Arrange
        User user1 = createUser("txiso1");
        User user2 = createUser("txiso2");
        Account acc1 = createAccount(user1, "Iso1 Account");
        Account acc2 = createAccount(user2, "Iso2 Account");
        Merchant m1 = createMerchant(user1, "IsoMerchant1");
        Merchant m2 = createMerchant(user2, "IsoMerchant2");

        Transaction tx1 = new Transaction();
        tx1.setAccount(acc1);
        tx1.setMerchant(m1);
        tx1.setAmount(new BigDecimal("100.00"));
        tx1.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        tx1.setType(TransactionType.EXPENSE);
        transactionRepository.save(tx1);

        Transaction tx2 = new Transaction();
        tx2.setAccount(acc2);
        tx2.setMerchant(m2);
        tx2.setAmount(new BigDecimal("200.00"));
        tx2.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        tx2.setType(TransactionType.EXPENSE);
        transactionRepository.save(tx2);

        // Act
        List<Transaction> user1Txs = transactionRepository.findByUserId(user1.getId());
        List<Transaction> user2Txs = transactionRepository.findByUserId(user2.getId());

        // Assert
        assertThat(user1Txs).hasSize(1);
        assertThat(user1Txs.get(0).getAmount()).isEqualByComparingTo("100.00");
        assertThat(user2Txs).hasSize(1);
        assertThat(user2Txs.get(0).getAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("getUncategorizedExpenseTotals() should return sum of expenses without category")
    void getUncategorizedExpenseTotals_shouldReturnSum() {
        // Arrange
        User user = createUser("uncatuser");
        Account account = createAccount(user, "Uncat Account");
        Merchant merchant = createMerchant(user, "Uncat Merchant");

        // Uncategorized expense
        Transaction t1 = new Transaction();
        t1.setAccount(account);
        t1.setMerchant(merchant);
        t1.setAmount(new BigDecimal("50.00"));
        t1.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t1.setType(TransactionType.EXPENSE);
        t1.setCategory(null);
        transactionRepository.save(t1);

        // Categorized expense
        Category cat = createCategory(user, "Categorized");
        Transaction t2 = new Transaction();
        t2.setAccount(account);
        t2.setMerchant(merchant);
        t2.setCategory(cat);
        t2.setAmount(new BigDecimal("30.00"));
        t2.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t2.setType(TransactionType.EXPENSE);
        transactionRepository.save(t2);

        // Act
        BigDecimal uncatTotal = transactionRepository.getUncategorizedExpenseTotals(user.getId());

        // Assert
        assertThat(uncatTotal).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("deleteById() should perform soft delete")
    void deleteById_shouldSoftDelete() {
        // Arrange
        User user = createUser("softdeluser");
        Account account = createAccount(user, "SoftDel Account");
        Merchant merchant = createMerchant(user, "SoftDel Merchant");

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setMerchant(merchant);
        tx.setAmount(new BigDecimal("10.00"));
        tx.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        tx.setType(TransactionType.EXPENSE);
        Transaction saved = transactionRepository.save(tx);

        // Act
        transactionRepository.deleteById(saved.getId());

        // Assert
        assertThat(transactionRepository.findById(saved.getId())).isEmpty();
        assertThat(transactionRepository.findByUserId(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("findAll() with Pageable and filter should return results")
    void findAll_withPageable_shouldReturnResults() {
        // Arrange
        User user = createUser("pageuser");
        Account account = createAccount(user, "Page Account");
        Merchant merchant = createMerchant(user, "Page Merchant");

        for (int i = 0; i < 5; i++) {
            Transaction tx = new Transaction();
            tx.setAccount(account);
            tx.setMerchant(merchant);
            tx.setAmount(new BigDecimal("10.00").multiply(new BigDecimal(i + 1)));
            tx.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(i));
            tx.setType(TransactionType.EXPENSE);
            transactionRepository.save(tx);
        }

        com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.TransactionFilter filter =
                new com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.TransactionFilter(
                        account.getId(), null, null, null, null, null, null, null, null
                );
        com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.FilterResult filterResult =
                com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.withFilter(user.getId(), filter);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 3);

        // Act
        org.springframework.data.domain.Page<Transaction> page = transactionRepository.findAll(filterResult, pageable);

        // Assert
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("existsByAccountIdAndDateAndAmountAndDescriptionAndType() should return true when exists")
    void exists_shouldReturnTrue() {
        // Arrange
        User user = createUser("existsuser");
        Account account = createAccount(user, "Exists Account");
        Merchant merchant = createMerchant(user, "Exists Merchant");
        OffsetDateTime date = OffsetDateTime.now(ZoneOffset.UTC);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setMerchant(merchant);
        tx.setAmount(new BigDecimal("123.45"));
        tx.setTransactionDate(date);
        tx.setDescription("Unique Tx");
        tx.setType(TransactionType.EXPENSE);
        transactionRepository.save(tx);

        // Act
        boolean exists = transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                account.getId(), date, new BigDecimal("123.45"), "Unique Tx", TransactionType.EXPENSE
        );

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("saveAll() and deleteAll() should handle multiple transactions")
    void saveAllAnddeleteAll_shouldWork() {
        // Arrange
        User user = createUser("bulkuser");
        Account account = createAccount(user, "Bulk Account");
        Merchant merchant = createMerchant(user, "Bulk Merchant");

        Transaction t1 = new Transaction();
        t1.setAccount(account);
        t1.setMerchant(merchant);
        t1.setAmount(new BigDecimal("1.00"));
        t1.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t1.setType(TransactionType.EXPENSE);

        Transaction t2 = new Transaction();
        t2.setAccount(account);
        t2.setMerchant(merchant);
        t2.setAmount(new BigDecimal("2.00"));
        t2.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        t2.setType(TransactionType.EXPENSE);

        // Act - Save
        List<Transaction> saved = transactionRepository.saveAll(List.of(t1, t2));
        assertThat(saved).hasSize(2);
        assertThat(transactionRepository.findByUserId(user.getId())).hasSize(2);

        // Act - Delete
        transactionRepository.deleteAll(saved);
        assertThat(transactionRepository.findByUserId(user.getId())).isEmpty();
    }

    private Transaction createTestTransaction(BigDecimal amount, TransactionType type) {
        User testUser = createUser("testuser_" + System.currentTimeMillis());
        Account account = createAccount(testUser, "Test Account");
        Merchant merchant = createMerchant(testUser, "Test Merchant");
        
        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setMerchant(merchant);
        tx.setAmount(amount);
        tx.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        tx.setType(type);
        return tx;
    }

    @Test
    @DisplayName("findAll should return all transactions")
    void findAll_returnsAllTransactions() {
        Transaction t1 = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        Transaction t2 = createTestTransaction(new BigDecimal("20.00"), TransactionType.INCOME);
        transactionRepository.saveAll(List.of(t1, t2));

        List<Transaction> result = transactionRepository.findAll();

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("existsByAccountIdAndDateAndAmountAndDescriptionAndType should find matching transaction")
    void findByExactMatch_findsTransaction() {
        Transaction t = createTestTransaction(new BigDecimal("15.55"), TransactionType.EXPENSE);
        t.setDescription("Test Match");
        Transaction saved = transactionRepository.save(t);

        boolean exists = transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                saved.getAccount().getId(),
                saved.getTransactionDate(),
                saved.getAmount(),
                saved.getDescription(),
                saved.getType()
        );

        assertThat(exists).isTrue();

        boolean notExists = transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                saved.getAccount().getId(),
                saved.getTransactionDate(),
                new BigDecimal("999.99"),
                saved.getDescription(),
                saved.getType()
        );

        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("count methods should return correct totals")
    void countMethods_returnCorrectTotals() {
        Transaction t = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        Category c = createCategory(t.getAccount().getUser(), "Test count category");
        t.setCategory(c);
        Transaction saved = transactionRepository.save(t);

        long accTotal = transactionRepository.countByAccountId(saved.getAccount().getId());
        assertThat(accTotal).isEqualTo(1);

        long catTotal = transactionRepository.countByCategoryId(saved.getCategory().getId());
        assertThat(catTotal).isEqualTo(1);
    }

    @Test
    @DisplayName("getCategoriesWithTransactions should return categories")
    void getCategoriesWithTransactions_returnsCategories() {
        Transaction t = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        Category parent = createCategory(t.getAccount().getUser(), "Parent Cat");
        
        Category child = new Category();
        child.setUser(t.getAccount().getUser());
        child.setName("Child Cat");
        child.setType(CategoryType.EXPENSE);
        child.setIconography(new com.mayureshpatel.pfdataservice.domain.Iconography("icon", "color"));
        child.setParent(parent);
        categoryRepository.save(child);
        
        t.setCategory(child);
        transactionRepository.save(t);

        List<Category> result = transactionRepository.getCategoriesWithTransactions(t.getAccount().getUser().getId());

        assertThat(result).isNotEmpty();
        assertThat(result.stream().map(Category::getName)).contains("Child Cat");
    }

    @Test
    @DisplayName("getMerchantsWithTransactions should return merchants")
    void getMerchantsWithTransactions_returnsMerchants() {
        Transaction t = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        t.getMerchant().setCleanName("Test Merchant");
        transactionRepository.save(t);

        List<Merchant> result = transactionRepository.getMerchantsWithTransactions(t.getAccount().getUser().getId());

        assertThat(result).isNotEmpty();
        assertThat(result).extracting(Merchant::getCleanName).contains("Test Merchant");
    }

    @Test
    @DisplayName("findMonthlySums should return data")
    void findMonthlySums_returnsData() {
        Transaction t = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        transactionRepository.save(t);

        List<Object[]> result = transactionRepository.findMonthlySums(t.getAccount().getUser().getId(), LocalDate.now().minusMonths(1));

        assertThat(result).isNotEmpty();
        Object[] row = result.get(0);
        assertThat(row).hasSize(4);
    }

    @Test
    @DisplayName("findRecentNonTransferTransactions should return data")
    void findRecentNonTransferTransactions_returnsData() {
        Transaction t = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        transactionRepository.save(t);

        List<Transaction> result = transactionRepository.findRecentNonTransferTransactions(t.getAccount().getUser().getId(), LocalDate.now().minusDays(1));

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("findAllById should return data or empty list correctly")
    void findAllById_returnsData() {
        Transaction t = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        Transaction saved = transactionRepository.save(t);

        List<Transaction> result = transactionRepository.findAllById(List.of(saved.getId()));
        assertThat(result).hasSize(1);

        assertThat(transactionRepository.findAllById(null)).isEmpty();
        assertThat(transactionRepository.findAllById(List.of())).isEmpty();
    }

    @Test
    @DisplayName("findAllByIdWithAccountAndUser should return data correctly")
    void findAllByIdWithAccountAndUser_returnsData() {
        Transaction t = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        Transaction saved = transactionRepository.save(t);

        List<Transaction> result = transactionRepository.findAllByIdWithAccountAndUser(List.of(saved.getId()));
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("countByIdInAndAccount_User_Id should count correctly")
    void countByIdInAndAccount_User_Id_countsCorrectly() {
        Transaction t = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        Transaction saved = transactionRepository.save(t);

        long count = transactionRepository.countByIdInAndAccount_User_Id(List.of(saved.getId()), t.getAccount().getUser().getId());
        assertThat(count).isEqualTo(1);

        assertThat(transactionRepository.countByIdInAndAccount_User_Id(null, t.getAccount().getUser().getId())).isEqualTo(0);
    }

    @Test
    @DisplayName("findExpensesSince should return data")
    void findExpensesSince_returnsData() {
        Transaction t = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        transactionRepository.save(t);

        List<Transaction> result = transactionRepository.findExpensesSince(t.getAccount().getUser().getId(), LocalDate.now().minusDays(1));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("search should handle sorting")
    void search_handlesSorting() {
        Transaction t1 = createTestTransaction(new BigDecimal("10.00"), TransactionType.EXPENSE);
        t1.setDescription("A");
        Transaction t2 = createTestTransaction(new BigDecimal("20.00"), TransactionType.EXPENSE);
        t2.setDescription("B");
        transactionRepository.saveAll(List.of(t1, t2));

        com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.TransactionFilter filter = 
            new com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.TransactionFilter(
                t1.getAccount().getId(), null, null, null, null, null, null, null, null);
        com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.FilterResult filterResult =
            com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.withFilter(t1.getAccount().getUser().getId(), filter);

        org.springframework.data.domain.Page<Transaction> descSort = transactionRepository.findAll(filterResult, org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "description")));
        
        assertThat(descSort.getContent()).isNotEmpty();
    }
}
