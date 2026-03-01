package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("AccountSnapshotRepository Integration Tests")
class AccountSnapshotRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AccountSnapshotRepository snapshotRepository;

    @Autowired
    private TestDataFactory factory;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = factory.createUser("snapshot_" + System.currentTimeMillis());
        testAccount = factory.createAccount(testUser, "Snapshot Account");
    }

    @Test
    @DisplayName("save() should insert new snapshot")
    void save_shouldInsertSnapshot() {
        // Arrange
        AccountSnapshot snapshot = new AccountSnapshot();
        snapshot.setAccountId(testAccount.getId());
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setBalance(new BigDecimal("1234.56"));

        // Act
        AccountSnapshot saved = snapshotRepository.save(snapshot);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBalance()).isEqualByComparingTo("1234.56");
    }

    @Test
    @DisplayName("findByAccountIdAndSnapshotDate() should return snapshot")
    void findByAccountAndDate() {
        // Arrange
        AccountSnapshot snapshot = new AccountSnapshot();
        snapshot.setAccountId(testAccount.getId());
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setBalance(new BigDecimal("500.00"));
        snapshotRepository.save(snapshot);

        // Act
        Optional<AccountSnapshot> found = snapshotRepository.findByAccountIdAndSnapshotDate(testAccount.getId(), LocalDate.now());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("update() should change balance")
    void update_shouldChangeBalance() {
        // Arrange
        AccountSnapshot snapshot = new AccountSnapshot();
        snapshot.setAccountId(testAccount.getId());
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setBalance(new BigDecimal("100.00"));
        AccountSnapshot saved = snapshotRepository.save(snapshot);

        // Act
        saved.setBalance(new BigDecimal("200.00"));
        snapshotRepository.save(saved);

        // Assert
        AccountSnapshot updated = snapshotRepository.findById(saved.getId()).get();
        assertThat(updated.getBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("delete() should remove snapshot")
    void delete_shouldRemoveSnapshot() {
        // Arrange
        AccountSnapshot snapshot = new AccountSnapshot();
        snapshot.setAccountId(testAccount.getId());
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setBalance(new BigDecimal("100.00"));
        AccountSnapshot saved = snapshotRepository.save(snapshot);

        // Act
        snapshotRepository.delete(saved);

        // Assert
        assertThat(snapshotRepository.findById(saved.getId())).isEmpty();
    }
}
