package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.AbstractIntegrationTest;
import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/data/test-data.sql")
class TransactionRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void shouldCalculateCategoryTotalsCorrectly() {
        List<CategoryTotal> results = this.transactionRepository.findCategoryTotals(
                1L,
                LocalDate.now().minusDays(1),
                LocalDate.now()
        );


        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCategoryName()).isEqualTo("Groceries");
        assertThat(results.get(0).getTotal()).isEqualByComparingTo("75.50");
        assertThat(results.get(1).getCategoryName()).isEqualTo("Dining Out");
        assertThat(results.get(1).getTotal()).isEqualByComparingTo("60.00");
    }
}