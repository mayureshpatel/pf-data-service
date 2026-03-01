package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("RecurringTransactionRepository Integration Tests")
class RecurringTransactionRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private TestDataFactory factory;

    private User testUser;
    private Account testAccount;
    private Merchant testMerchant;

    @BeforeEach
    void setUp() {
        testUser = factory.createUser("recurring_" + System.currentTimeMillis());
        testAccount = factory.createAccount(testUser, "Recurring Account");
        testMerchant = factory.createMerchant(testUser, "Netflix");
    }

    @Test
    @DisplayName("save() should insert new recurring transaction and assign ID")
    void save_shouldInsertNewRecurringTransaction() {
        // Arrange
        RecurringTransaction rt = new RecurringTransaction();
        rt.setUser(testUser);
        rt.setAccount(testAccount);
        rt.setMerchant(testMerchant);
        rt.setAmount(new BigDecimal("15.99"));
        rt.setFrequency(Frequency.MONTHLY);
        rt.setNextDate(LocalDate.now().plusMonths(1));
        rt.setActive(true);

        // Act
        RecurringTransaction saved = recurringRepository.save(rt);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAmount()).isEqualByComparingTo("15.99");
    }

    @Test
    @DisplayName("findById() should return recurring transaction when exists")
    void findById_shouldReturnRecurringTransaction() {
        // Arrange
        RecurringTransaction rt = createAndSaveRecurring("Spotify", "9.99");

        // Act
        Optional<RecurringTransaction> found = recurringRepository.findById(rt.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("9.99");
    }

    @Test
    @DisplayName("findAllByUserId() should return all recurring transactions for user")
    void findAllByUserId_shouldReturnUserRecurringTransactions() {
        // Arrange
        createAndSaveRecurring("Internet", "70.00");
        createAndSaveRecurring("Rent", "1500.00");

        // Act
        List<RecurringTransaction> results = recurringRepository.findAllByUserId(testUser.getId());

        // Assert
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("findByUserIdAndActiveTrueOrderByNextDate() should return only active ones")
    void findByUserIdAndActive_shouldReturnActiveOnly() {
        // Arrange
        createAndSaveRecurring("Active 1", "10.00");
        RecurringTransaction inactive = createAndSaveRecurring("Inactive", "20.00");
        inactive.setActive(false);
        recurringRepository.save(inactive);

        // Act
        List<RecurringTransaction> results = recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(testUser.getId());

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("update() should modify existing recurring transaction")
    void update_shouldModifyRecurringTransaction() {
        // Arrange
        RecurringTransaction rt = createAndSaveRecurring("Insurance", "100.00");

        // Act
        rt.setAmount(new BigDecimal("110.00"));
        recurringRepository.save(rt);

        // Assert
        RecurringTransaction updated = recurringRepository.findById(rt.getId()).get();
        assertThat(updated.getAmount()).isEqualByComparingTo("110.00");
    }

    @Test
    @DisplayName("delete() should soft delete by updating deleted_at")
    void delete_shouldSoftDelete() {
        // Arrange
        RecurringTransaction rt = createAndSaveRecurring("Gym", "40.00");

        // Act
        recurringRepository.delete(rt.getId(), testUser.getId());

        // Assert
        assertThat(recurringRepository.findById(rt.getId())).isEmpty();
    }

    private RecurringTransaction createAndSaveRecurring(String merchantName, String amount) {
        Merchant merchant = factory.createMerchant(testUser, merchantName);
        RecurringTransaction rt = new RecurringTransaction();
        rt.setUser(testUser);
        rt.setAccount(testAccount);
        rt.setMerchant(merchant);
        rt.setAmount(new BigDecimal(amount));
        rt.setFrequency(Frequency.MONTHLY);
        rt.setNextDate(LocalDate.now().plusMonths(1));
        rt.setActive(true);
        return recurringRepository.save(rt);
    }
}
