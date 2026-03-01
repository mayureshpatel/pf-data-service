package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.mapper.TransactionDtoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transaction domain object tests")
class TransactionTest {

    @Test
    @DisplayName("getNetChange — should return positive for INCOME")
    void getNetChange_income_returnsPositive() {
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("100.00"));
        tx.setType(TransactionType.INCOME);

        assertThat(tx.getNetChange()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("getNetChange — should return negative for EXPENSE")
    void getNetChange_expense_returnsNegative() {
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("50.00"));
        tx.setType(TransactionType.EXPENSE);

        assertThat(tx.getNetChange()).isEqualByComparingTo("-50.00");
    }

    @Test
    @DisplayName("toDto — should map all fields correctly")
    void toDto_mapsAllFields() {
        // Arrange
        Account account = new Account();
        account.setId(5L);
        account.setName("Account");

        Merchant merchant = new Merchant();
        merchant.setId(8L);
        merchant.setCleanName("Merchant");

        Category category = new Category();
        category.setId(12L);
        category.setName("Category");

        Transaction tx = new Transaction();
        tx.setId(10L);
        tx.setAccount(account);
        tx.setMerchant(merchant);
        tx.setCategory(category);
        tx.setAmount(new BigDecimal("75.00"));
        tx.setTransactionDate(OffsetDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC));
        tx.setType(TransactionType.EXPENSE);
        tx.setDescription("Description");

        // Act
        TransactionDto dto = TransactionDtoMapper.toDto(tx);

        // Assert
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.account().id()).isEqualTo(5L);
        assertThat(dto.account().name()).isEqualTo("Account");
        assertThat(dto.merchant().id()).isEqualTo(8L);
        assertThat(dto.merchant().cleanName()).isEqualTo("Merchant");
        assertThat(dto.category().id()).isEqualTo(12L);
        assertThat(dto.category().name()).isEqualTo("Category");
        assertThat(dto.amount()).isEqualByComparingTo("75.00");
        assertThat(dto.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(dto.description()).isEqualTo("Description");
    }
}
