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
    @DisplayName("applyTransaction — should initialize null balance to zero")
    void applyTransaction_nullBalance_initializesToZeroThenApplies() {
        // Arrange
        Account account = new Account();
        account.setCurrentBalance(null);
        
        Transaction income = new Transaction();
        income.setAmount(new BigDecimal("50.00"));
        income.setType(TransactionType.INCOME);

        // Act
        account.applyTransaction(income);

        // Assert
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("50.00");
    }
}
