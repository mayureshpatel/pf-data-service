package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Account domain object tests")
class AccountTest {

    @Test
    @DisplayName("applyTransaction — income should increase balance")
    void applyTransaction_income_increasesBalance() {
        // Arrange
        Account account = new Account();
        account.setCurrentBalance(new BigDecimal("100.00"));
        
        Transaction income = new Transaction();
        income.setAmount(new BigDecimal("50.00"));
        income.setType(TransactionType.INCOME);

        // Act
        account.applyTransaction(income);

        // Assert
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("applyTransaction — expense should decrease balance")
    void applyTransaction_expense_decreasesBalance() {
        // Arrange
        Account account = new Account();
        account.setCurrentBalance(new BigDecimal("100.00"));
        
        Transaction expense = new Transaction();
        expense.setAmount(new BigDecimal("50.00"));
        expense.setType(TransactionType.EXPENSE);

        // Act
        account.applyTransaction(expense);

        // Assert
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("undoTransaction — expense should increase balance back")
    void undoTransaction_expense_increasesBalanceBack() {
        // Arrange
        Account account = new Account();
        account.setCurrentBalance(new BigDecimal("50.00"));
        
        Transaction expense = new Transaction();
        expense.setAmount(new BigDecimal("50.00"));
        expense.setType(TransactionType.EXPENSE);

        // Act
        account.undoTransaction(expense);

        // Assert
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("applyTransaction — adjustment should use signed amount")
    void applyTransaction_adjustment_usesSignedAmount() {
        // Arrange
        Account account = new Account();
        account.setCurrentBalance(new BigDecimal("100.00"));
        
        Transaction adjustment = new Transaction();
        adjustment.setAmount(new BigDecimal("-10.00"));
        adjustment.setType(TransactionType.ADJUSTMENT);

        // Act
        account.applyTransaction(adjustment);

        // Assert
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("toDto — should map all fields correctly")
    void toDto_mapsAllFields() {
        // Arrange
        com.mayureshpatel.pfdataservice.domain.user.User user = new com.mayureshpatel.pfdataservice.domain.user.User();
        user.setId(1L);
        
        Account account = new Account();
        account.setId(10L);
        account.setUser(user);
        account.setName("Checking");
        account.setCurrentBalance(new BigDecimal("123.45"));
        
        AccountType type = new AccountType();
        type.setCode("CH");
        type.setLabel("Checking");
        account.setType(type);
        
        com.mayureshpatel.pfdataservice.domain.currency.Currency currency = new com.mayureshpatel.pfdataservice.domain.currency.Currency();
        currency.setCode("USD");
        currency.setSymbol("$");
        account.setCurrency(currency);

        // Act
        com.mayureshpatel.pfdataservice.dto.account.AccountDto dto = com.mayureshpatel.pfdataservice.mapper.AccountDtoMapper.toDto(account);

        // Assert
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.userId()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Checking");
        assertThat(dto.accountTypeCode()).isEqualTo("CH");
        assertThat(dto.currentBalance()).isEqualByComparingTo("123.45");
        assertThat(dto.currencyCode()).isEqualTo("USD");
        assertThat(dto.currencySymbol()).isEqualTo("$");
    }
}
