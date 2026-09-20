package com.mayureshpatel.pfdataservice.migration;

import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import com.mayureshpatel.pfdataservice.service.MerchantNameNormalizer;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link V41__ConsolidateMerchantsAndBackfillDescriptionLinks} directly against a real
 * Testcontainers Postgres, the same way {@code V35BackfillMerchantCleanNamesTest} exercises
 * {@link V35__BackfillMerchantCleanNames} -- by the time any test's Spring context boots, V41 has
 * already applied once against an empty {@code merchants} table (proof by itself that it handles
 * zero rows cleanly: if it threw there, no test in this suite would ever start), so this invokes
 * {@code migrate()} directly again with synthetic fixture rows to verify its actual behavior.
 * <p>
 * Uses {@link DataSourceUtils#getConnection} rather than {@code dataSource.getConnection()}
 * directly, for the exact self-deadlock reason documented on {@code V35BackfillMerchantCleanNamesTest}
 * -- a raw {@code dataSource.getConnection()} call here would bypass {@code @JdbcTest}'s ambient
 * test transaction and deadlock against {@code BaseRepositoryTest}'s still-open baseline reseed.
 */
@DisplayName("V41__ConsolidateMerchantsAndBackfillDescriptionLinks Integration Test")
class V41ConsolidateMerchantsAndBackfillDescriptionLinksTest extends BaseRepositoryTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("should name and link the baseline's un-duplicated merchant, leaving global (user_id IS NULL) merchants untouched")
    void shouldHandleBaselineWithNoDuplicateGroups() throws Exception {
        // arrange -- baseline seeds merchants 1-3 as global (user_id NULL: Whole Foods/Amazon/Shell)
        // and merchant 4 as the only user-owned row (user 1, "LOCAL CAFE" / "My Favorite Cafe"),
        // with no duplicate group for either.
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            V41__ConsolidateMerchantsAndBackfillDescriptionLinks migration =
                    new V41__ConsolidateMerchantsAndBackfillDescriptionLinks(new MerchantNameNormalizer());
            Context context = contextFor(connection);

            // act
            migration.migrate(context);

            // assert & verify
            assertEquals("My Favorite Cafe", readMerchantName(connection, 4));
            assertTrue(descriptionLinkExists(connection, 1, 4, "LOCAL CAFE"));
            assertNull(readMerchantName(connection, 1));
            assertNull(readMerchantName(connection, 2));
            assertNull(readMerchantName(connection, 3));
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Test
    @DisplayName("should consolidate a real duplicate group: lowest-id survivor, both FK repoints, both original names linked")
    void shouldConsolidateDuplicateGroup() throws Exception {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            // arrange -- two rows sharing clean_name "Starbucks" for user 1, plus a transaction and
            // a recurring transaction pointing at the one that should lose.
            long survivorId = insertMerchant(connection, 1, "STARBUCKS #1", "Starbucks");
            long loserId = insertMerchant(connection, 1, "STARBUCKS #2", "Starbucks");
            long transactionId = insertTransaction(connection, 1, loserId, "Coffee");
            long recurringId = insertRecurringTransaction(connection, 1, 1, loserId);

            V41__ConsolidateMerchantsAndBackfillDescriptionLinks migration =
                    new V41__ConsolidateMerchantsAndBackfillDescriptionLinks(new MerchantNameNormalizer());
            Context context = contextFor(connection);

            // act
            migration.migrate(context);

            // assert & verify
            assertEquals("Starbucks", readMerchantName(connection, survivorId));
            assertFalse(merchantExists(connection, loserId));
            assertEquals(survivorId, readTransactionMerchantId(connection, transactionId));
            assertEquals(survivorId, readRecurringTransactionMerchantId(connection, recurringId));
            assertTrue(descriptionLinkExists(connection, 1, survivorId, "STARBUCKS #1"));
            assertTrue(descriptionLinkExists(connection, 1, survivorId, "STARBUCKS #2"));
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private Context contextFor(Connection connection) {
        return new Context() {
            @Override
            public Configuration getConfiguration() {
                return null; // not used by this migration's implementation
            }

            @Override
            public Connection getConnection() {
                return connection;
            }
        };
    }

    private long insertMerchant(Connection connection, long userId, String originalName, String cleanName) throws Exception {
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

    private long insertTransaction(Connection connection, long accountId, long merchantId, String description) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO transactions (account_id, merchant_id, amount, date, description, type)
                VALUES (?, ?, ?, NOW(), ?, 'EXPENSE') RETURNING id
                """)) {
            insert.setLong(1, accountId);
            insert.setLong(2, merchantId);
            insert.setBigDecimal(3, new BigDecimal("4.50"));
            insert.setString(4, description);
            try (ResultSet keys = insert.executeQuery()) {
                keys.next();
                return keys.getLong("id");
            }
        }
    }

    private long insertRecurringTransaction(Connection connection, long userId, long accountId, long merchantId) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO recurring_transactions (user_id, account_id, merchant_id, amount, frequency, next_date, active)
                VALUES (?, ?, ?, ?, 'MONTHLY', CURRENT_DATE, true) RETURNING id
                """)) {
            insert.setLong(1, userId);
            insert.setLong(2, accountId);
            insert.setLong(3, merchantId);
            insert.setBigDecimal(4, new BigDecimal("9.99"));
            try (ResultSet keys = insert.executeQuery()) {
                keys.next();
                return keys.getLong("id");
            }
        }
    }

    private String readMerchantName(Connection connection, long id) throws Exception {
        try (PreparedStatement select = connection.prepareStatement("SELECT name FROM merchants WHERE id = ?")) {
            select.setLong(1, id);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getString("name");
            }
        }
    }

    private boolean merchantExists(Connection connection, long id) throws Exception {
        try (PreparedStatement select = connection.prepareStatement("SELECT 1 FROM merchants WHERE id = ?")) {
            select.setLong(1, id);
            try (ResultSet rs = select.executeQuery()) {
                return rs.next();
            }
        }
    }

    private long readTransactionMerchantId(Connection connection, long id) throws Exception {
        try (PreparedStatement select = connection.prepareStatement("SELECT merchant_id FROM transactions WHERE id = ?")) {
            select.setLong(1, id);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getLong("merchant_id");
            }
        }
    }

    private long readRecurringTransactionMerchantId(Connection connection, long id) throws Exception {
        try (PreparedStatement select = connection.prepareStatement("SELECT merchant_id FROM recurring_transactions WHERE id = ?")) {
            select.setLong(1, id);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getLong("merchant_id");
            }
        }
    }

    private boolean descriptionLinkExists(Connection connection, long userId, long merchantId, String description) throws Exception {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT 1 FROM merchant_description_links WHERE user_id = ? AND merchant_id = ? AND description = ?")) {
            select.setLong(1, userId);
            select.setLong(2, merchantId);
            select.setString(3, description);
            try (ResultSet rs = select.executeQuery()) {
                return rs.next();
            }
        }
    }
}
