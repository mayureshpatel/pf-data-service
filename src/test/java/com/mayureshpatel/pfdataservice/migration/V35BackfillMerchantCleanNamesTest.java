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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises {@link V35__BackfillMerchantCleanNames} directly against a real Testcontainers
 * Postgres, rather than relying on Flyway's own "has this version already run" bookkeeping --
 * by the time any test's Spring context boots, every migration up to and including V35 has
 * already applied once against an empty {@code merchants} table, so there's nothing left for
 * Flyway itself to backfill. This inserts synthetic pre-normalization-era rows (blank
 * {@code clean_name}, exactly what existed before PF-218) and invokes the migration's own
 * {@code migrate()} method directly to verify its actual backfill behavior end-to-end.
 * <p>
 * Uses {@link DataSourceUtils#getConnection} rather than {@code dataSource.getConnection()}
 * directly -- {@code @JdbcTest} keeps {@code BaseRepositoryTest}'s {@code @BeforeEach} baseline
 * reseed (a {@code TRUNCATE ... CASCADE}) open in the same test-managed transaction for the
 * whole test method. A raw {@code dataSource.getConnection()} call bypasses that transaction
 * synchronization and grabs a genuinely separate physical connection from the pool, which then
 * deadlocks against the still-open {@code TRUNCATE}'s lock -- waiting on a lock that only this
 * same, now-blocked test method finishing would release. {@code DataSourceUtils} joins the
 * ambient transaction instead, avoiding the self-deadlock entirely.
 */
@DisplayName("V35__BackfillMerchantCleanNames Integration Test")
class V35BackfillMerchantCleanNamesTest extends BaseRepositoryTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("should normalize every existing blank-clean_name merchant row when run")
    void shouldBackfillBlankCleanNames() throws Exception {
        // arrange
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            long wholefdsId = insertLegacyBlankMerchant(connection, "WHOLEFDS #12345");
            long chevronId = insertLegacyBlankMerchant(connection, "CHEVRON 00123 4567");

            V35__BackfillMerchantCleanNames migration =
                    new V35__BackfillMerchantCleanNames(new MerchantNameNormalizer());
            Context context = contextFor(connection);

            // act
            migration.migrate(context);

            // assert & verify
            assertEquals("Wholefds", readCleanName(connection, wholefdsId));
            assertEquals("Chevron", readCleanName(connection, chevronId));
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Test
    @DisplayName("should leave already-normalized merchants untouched")
    void shouldLeaveAlreadyNormalizedMerchantsUntouched() throws Exception {
        // arrange
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            long id = insertMerchant(connection, "STARBUCKS #1", "Starbucks");

            V35__BackfillMerchantCleanNames migration =
                    new V35__BackfillMerchantCleanNames(new MerchantNameNormalizer());
            Context context = contextFor(connection);

            // act
            migration.migrate(context);

            // assert & verify
            assertEquals("Starbucks", readCleanName(connection, id));
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Test
    @DisplayName("should not throw when there are no blank clean names to backfill")
    void shouldNoOpWhenNothingToBackfill() throws Exception {
        // arrange -- deliberately doesn't assert the table is empty first: whether it is depends
        // on test execution order relative to the other tests in this class (each of which inserts
        // its own rows), and that ordering isn't this test's concern. What matters here is that the
        // empty-batch path (see V35__BackfillMerchantCleanNames's hasPendingUpdates guard) doesn't
        // throw when there happen to be zero blank-clean_name rows, regardless of what else exists.
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            V35__BackfillMerchantCleanNames migration =
                    new V35__BackfillMerchantCleanNames(new MerchantNameNormalizer());
            Context context = contextFor(connection);

            // act & assert & verify
            assertDoesNotThrow(() -> migration.migrate(context));
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

    private long insertLegacyBlankMerchant(Connection connection, String originalName) throws Exception {
        return insertMerchant(connection, originalName, "");
    }

    private long insertMerchant(Connection connection, String originalName, String cleanName) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO merchants (user_id, original_name, clean_name) VALUES (1, ?, ?) RETURNING id")) {
            insert.setString(1, originalName);
            insert.setString(2, cleanName);
            try (ResultSet keys = insert.executeQuery()) {
                keys.next();
                return keys.getLong("id");
            }
        }
    }

    private String readCleanName(Connection connection, long id) throws Exception {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT clean_name FROM merchants WHERE id = ?")) {
            select.setLong(1, id);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getString("clean_name");
            }
        }
    }
}
