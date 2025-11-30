package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TransactionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Test
            void shouldCalculateCategoryTotalsCorrectly() {
        User user = this.userRepository.save(new User(null, "user", "pass", "email@test.com", null, null, null));
        Account account = this.accountRepository.save(new Account(null, "Checking", AccountType.CHECKING, BigDecimal.ZERO, user));
        Category food = this.categoryRepository.save(new Category(null, "Food", "#fff", user));

        this.transactionRepository.save(new Transaction(null, new BigDecimal("10.00"), LocalDate.now(), "Lunch", TransactionType.EXPENSE, account, food));
        this.transactionRepository.save(new Transaction(null, new BigDecimal("20.00"), LocalDate.now(), "Dinner", TransactionType.EXPENSE, account, food));
        this.transactionRepository.save(new Transaction(null, new BigDecimal("100.00"), LocalDate.now(), "Breakfast", TransactionType.EXPENSE, account, food));

        List<CategoryTotal> results = this.transactionRepository.findCategoryTotals(
                user.getId(),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getCategoryName()).isEqualTo("Food");
        assertThat(results.getFirst().getTotalAmount()).isEqualByComparingTo("130.00");
    }
}