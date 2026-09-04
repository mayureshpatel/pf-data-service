package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantMergeRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link MerchantService#mergeMerchants} end-to-end against real Postgres -- both
 * reassignment queries (transactions and recurring transactions) together, plus the merged-away
 * merchant's actual deletion, in one real transaction. A Mockito-based test can't stand in for
 * this: the ticket's whole point is proving the real {@code recurring_transactions.merchant_id
 * NOT NULL} constraint is satisfied by explicit reassignment happening before the delete, not
 * assumed from reading the code.
 */
@Import({MerchantService.class, MerchantRepository.class, MerchantNameNormalizer.class,
        TransactionRepository.class, RecurringTransactionRepository.class})
@DisplayName("MerchantService.mergeMerchants Integration Test")
class MerchantMergeIntegrationTest extends BaseRepositoryTest {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private DataSource dataSource;

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final Long ACCOUNT_1 = 1L; // owned by USER_1, per test-data-baseline.sql

    @Test
    @DisplayName("PF-222: should reassign both transactions and recurring transactions to the "
            + "surviving merchant, then delete the merged-away one -- all in one operation")
    void shouldReassignAcrossBothTransactionsAndRecurringTransactions() throws Exception {
        // arrange
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            long survivingId = insertMerchant(connection, "STARBUCKS #1", "Starbucks");
            long mergedAwayId = insertMerchant(connection, "STARBUCKS #2", "Starbucks Coffee");
            long transactionId = insertTransaction(connection, mergedAwayId);
            long recurringId = insertRecurringTransaction(connection, mergedAwayId);

            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(survivingId)
                    .mergedAwayMerchantId(mergedAwayId)
                    .build();

            // act
            merchantService.mergeMerchants(USER_1, request);

            // assert & verify
            assertEquals(survivingId, readTransactionMerchantId(connection, transactionId));
            assertEquals(survivingId, readRecurringTransactionMerchantId(connection, recurringId));
            assertTrue(merchantRepository.findById(mergedAwayId).isEmpty());
            assertTrue(merchantRepository.findById(survivingId).isPresent());
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Test
    @DisplayName("PF-222: should throw and change nothing when the surviving merchant isn't owned "
            + "by the requesting user")
    void shouldThrowWhenSurvivingMerchantNotOwned() throws Exception {
        // arrange
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            long notOwnedId = insertMerchant(connection, "USER2 SHOP", "User2 Shop", USER_2);
            long mergedAwayId = insertMerchant(connection, "STARBUCKS #2", "Starbucks Coffee");
            long transactionId = insertTransaction(connection, mergedAwayId);

            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(notOwnedId)
                    .mergedAwayMerchantId(mergedAwayId)
                    .build();

            // act & assert & verify
            assertThrows(RuntimeException.class, () -> merchantService.mergeMerchants(USER_1, request));
            // nothing reassigned, nothing deleted
            assertEquals(mergedAwayId, readTransactionMerchantId(connection, transactionId));
            assertTrue(merchantRepository.findById(mergedAwayId).isPresent());
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Test
    @DisplayName("PF-222: should throw when merging a merchant into itself")
    void shouldThrowForSelfMerge() throws Exception {
        // arrange
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            long merchantId = insertMerchant(connection, "STARBUCKS #1", "Starbucks");
            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(merchantId)
                    .mergedAwayMerchantId(merchantId)
                    .build();

            // act & assert & verify
            assertThrows(IllegalArgumentException.class, () -> merchantService.mergeMerchants(USER_1, request));
            assertTrue(merchantRepository.findById(merchantId).isPresent());
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private long insertMerchant(Connection connection, String originalName, String cleanName) throws Exception {
        return insertMerchant(connection, originalName, cleanName, USER_1);
    }

    private long insertMerchant(Connection connection, String originalName, String cleanName, Long userId) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO merchants (user_id, original_name, clean_name) VALUES (?, ?, ?) RETURNING id")) {
            insert.setLong(1, userId);
            insert.setString(2, originalName);
            insert.setString(3, cleanName);
            try (ResultSet keys = insert.executeQuery()) {
                keys.next();
                return keys.getLong("id");
            }
        }
    }

    private long insertTransaction(Connection connection, long merchantId) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO transactions (account_id, merchant_id, amount, date, description, type) "
                        + "VALUES (?, ?, 5.00, CURRENT_TIMESTAMP, 'Test Coffee', 'EXPENSE') RETURNING id")) {
            insert.setLong(1, ACCOUNT_1);
            insert.setLong(2, merchantId);
            try (ResultSet keys = insert.executeQuery()) {
                keys.next();
                return keys.getLong("id");
            }
        }
    }

    private long insertRecurringTransaction(Connection connection, long merchantId) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO recurring_transactions (user_id, account_id, merchant_id, amount, frequency, next_date, active) "
                        + "VALUES (?, ?, ?, 5.00, 'MONTHLY', CURRENT_DATE, true) RETURNING id")) {
            insert.setLong(1, USER_1);
            insert.setLong(2, ACCOUNT_1);
            insert.setLong(3, merchantId);
            try (ResultSet keys = insert.executeQuery()) {
                keys.next();
                return keys.getLong("id");
            }
        }
    }

    private long readTransactionMerchantId(Connection connection, long transactionId) throws Exception {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT merchant_id FROM transactions WHERE id = ?")) {
            select.setLong(1, transactionId);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getLong("merchant_id");
            }
        }
    }

    private long readRecurringTransactionMerchantId(Connection connection, long recurringId) throws Exception {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT merchant_id FROM recurring_transactions WHERE id = ?")) {
            select.setLong(1, recurringId);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getLong("merchant_id");
            }
        }
    }
}
