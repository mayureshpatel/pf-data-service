package com.mayureshpatel.pfdataservice.integration;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edit and delete of a stored transaction on a MANUAL-balance account must never touch that
 * account's hand-entered balance. Runs the real service and repositories against a real Postgres:
 * the bug lived in the enriched transaction query not selecting {@code balance_mode}, so the
 * transaction's nested account silently read as DERIVED -- invisible to any hand-built mock.
 */
class ManualBalanceTransactionIntegrationTest extends BaseIntegrationTest {

    private static final long USER_ID = 710L;
    private static final long MANUAL_ID = 710L;
    private static final long DERIVED_ID = 711L;
    private static final long TX_ID = 710L;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM transactions WHERE account_id IN (?, ?)", MANUAL_ID, DERIVED_ID);
        jdbc.update("DELETE FROM accounts WHERE id IN (?, ?)", MANUAL_ID, DERIVED_ID);
        jdbc.update("INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) "
                + "VALUES (?, 'manual710', 'manual710@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING", USER_ID);
        jdbc.update("INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name, balance_mode) "
                + "VALUES (?, ?, 'Roth IRA', 'INVESTMENT', 12500.00, 'STANDARD', 'MANUAL')", MANUAL_ID, USER_ID);
        jdbc.update("INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) "
                + "VALUES (?, ?, 'Checking', 'CHECKING', 1000.00, 'STANDARD')", DERIVED_ID, USER_ID);
        jdbc.update("INSERT INTO transactions (id, account_id, amount, date, description, type) "
                + "VALUES (?, ?, 500.00, NOW(), 'Contribution', 'CONTRIBUTION')", TX_ID, MANUAL_ID);
    }

    private BigDecimal balanceOf(long accountId) {
        return jdbc.queryForObject("SELECT current_balance FROM accounts WHERE id = ?", BigDecimal.class, accountId);
    }

    private TransactionUpdateRequest.TransactionUpdateRequestBuilder update(long accountId, String amount, String type) {
        return TransactionUpdateRequest.builder().id(TX_ID).accountId(accountId).amount(new BigDecimal(amount))
                .transactionDate(OffsetDateTime.now()).description("Contribution").type(type);
    }

    @Test
    @DisplayName("deleting a transaction leaves a MANUAL account's balance unchanged")
    void delete_shouldNotChangeManualBalance() {
        // act
        transactionService.deleteTransaction(USER_ID, TX_ID);

        // assert & verify
        assertEquals(0, new BigDecimal("12500.00").compareTo(balanceOf(MANUAL_ID)));
    }

    @Test
    @DisplayName("editing a transaction's amount leaves a MANUAL account's balance unchanged")
    void update_shouldNotChangeManualBalance() {
        // act
        transactionService.updateTransaction(USER_ID, update(MANUAL_ID, "600.00", "CONTRIBUTION").build());

        // assert & verify
        assertEquals(0, new BigDecimal("12500.00").compareTo(balanceOf(MANUAL_ID)));
    }

    @Test
    @DisplayName("a bulk update leaves a MANUAL account's balance unchanged")
    void bulkUpdate_shouldNotChangeManualBalance() {
        // act
        transactionService.updateTransactionsBulk(USER_ID, List.of(update(MANUAL_ID, "600.00", "CONTRIBUTION").build()));

        // assert & verify
        assertEquals(0, new BigDecimal("12500.00").compareTo(balanceOf(MANUAL_ID)));
    }

    @Test
    @DisplayName("moving a transaction from a MANUAL to a DERIVED account skips the undo on the MANUAL side only")
    void move_manualToDerived_shouldOnlyApplyToDerivedSide() {
        // act -- the row becomes a 500 EXPENSE on the derived checking account
        transactionService.updateTransaction(USER_ID, update(DERIVED_ID, "500.00", "EXPENSE").build());

        // assert & verify
        assertEquals(0, new BigDecimal("12500.00").compareTo(balanceOf(MANUAL_ID)));
        assertEquals(0, new BigDecimal("500.00").compareTo(balanceOf(DERIVED_ID)));
        assertTrue(jdbc.queryForObject("SELECT account_id FROM transactions WHERE id = ?", Long.class, TX_ID) == DERIVED_ID);
    }

    @Test
    @DisplayName("a CONTRIBUTION equal in amount to a checking EXPENSE on the same date is not suggested as a transfer")
    void contribution_shouldNotBeSuggestedAsTransfer() {
        // arrange -- a 500 EXPENSE on checking, same date as the 500 CONTRIBUTION seeded on the Roth
        jdbc.update("INSERT INTO transactions (id, account_id, amount, date, description, type) "
                + "SELECT 711, ?, 500.00, date, 'Roth funding', 'EXPENSE' FROM transactions WHERE id = ?", DERIVED_ID, TX_ID);

        // act
        var suggestions = transactionService.findPotentialTransfers(USER_ID);

        // assert & verify
        assertTrue(suggestions.isEmpty(), "a contribution must never be paired with an expense: " + suggestions);
    }
}
