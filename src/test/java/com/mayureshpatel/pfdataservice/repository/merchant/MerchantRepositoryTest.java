package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("MerchantRepository Integration Tests")
class MerchantRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private TestDataFactory factory;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = factory.createUser("merchant_" + System.currentTimeMillis());
    }

    @Test
    @DisplayName("save() should insert new merchant and assign ID")
    void save_shouldInsertMerchant() {
        Merchant merchant = factory.createMerchant(testUser, "Amazon");

        assertThat(merchant.getId()).isNotNull();
    }

    @Test
    @DisplayName("findById() should return merchant when exists")
    void findById_shouldReturnMerchant() {
        Merchant saved = factory.createMerchant(testUser, "Target");

        Optional<Merchant> found = merchantRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCleanName()).isEqualTo("Target");
    }

    @Test
    @DisplayName("findById() should return empty when not exists")
    void findById_shouldReturnEmpty() {
        assertThat(merchantRepository.findById(99999L)).isEmpty();
    }

    @Test
    @DisplayName("findAllByUserId() should return all merchants for user")
    void findAllByUserId_shouldReturnUserMerchants() {
        factory.createMerchant(testUser, "Amazon");
        factory.createMerchant(testUser, "Walmart");

        List<Merchant> merchants = merchantRepository.findAllByUserId(testUser.getId());

        assertThat(merchants).hasSize(2);
        assertThat(merchants).extracting(Merchant::getCleanName).containsExactlyInAnyOrder("Amazon", "Walmart");
    }

    @Test
    @DisplayName("findAllByCleanName() should return merchants matching exact name")
    void findAllByCleanName_shouldMatchExact() {
        factory.createMerchant(testUser, "Kroger");
        factory.createMerchant(testUser, "Kroger Plus");

        List<Merchant> merchants = merchantRepository.findAllByCleanName("Kroger");

        assertThat(merchants).hasSize(1);
        assertThat(merchants.get(0).getCleanName()).isEqualTo("Kroger");
    }

    @Test
    @DisplayName("update() should modify existing merchant")
    void update_shouldModifyMerchant() {
        Merchant merchant = factory.createMerchant(testUser, "Old Name");

        merchant.setCleanName("New Name");
        merchantRepository.update(merchant);

        Optional<Merchant> found = merchantRepository.findById(merchant.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCleanName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("findMerchantTotals() should aggregate spending by merchant")
    void findMerchantTotals_shouldAggregate() {
        // Arrange
        Account account = factory.createAccount(testUser, "Totals Account");
        Merchant m1 = factory.createMerchant(testUser, "Grocery Store");
        Merchant m2 = factory.createMerchant(testUser, "Gas Station");
        Category category = factory.createCategory(testUser, "General", CategoryType.EXPENSE);

        factory.createTransaction(account, m1, category, new BigDecimal("50.00"), OffsetDateTime.now(ZoneOffset.UTC), TransactionType.EXPENSE);
        factory.createTransaction(account, m1, category, new BigDecimal("30.00"), OffsetDateTime.now(ZoneOffset.UTC), TransactionType.EXPENSE);
        factory.createTransaction(account, m2, category, new BigDecimal("40.00"), OffsetDateTime.now(ZoneOffset.UTC), TransactionType.EXPENSE);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime start = now.minusDays(2);
        OffsetDateTime end = now.plusDays(2);

        // Act
        List<MerchantBreakdownDto> totals = merchantRepository.findMerchantTotals(testUser.getId(), start, end);

        // Assert
        assertThat(totals).hasSizeGreaterThanOrEqualTo(2);
        MerchantBreakdownDto groceryTotal = totals.stream()
                .filter(t -> t.merchant().cleanName().equals("Grocery Store"))
                .findFirst().orElseThrow();
        assertThat(groceryTotal.total()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("data isolation - user cannot see other user's merchants")
    void dataIsolation_shouldIsolateByUser() {
        factory.createMerchant(testUser, "User1 Merchant");

        User otherUser = factory.createUser("otherm_" + System.currentTimeMillis());
        factory.createMerchant(otherUser, "User2 Merchant");

        List<Merchant> user1 = merchantRepository.findAllByUserId(testUser.getId());
        List<Merchant> user2 = merchantRepository.findAllByUserId(otherUser.getId());

        assertThat(user1).extracting(Merchant::getCleanName).containsExactly("User1 Merchant");
        assertThat(user2).extracting(Merchant::getCleanName).containsExactly("User2 Merchant");
    }
}
