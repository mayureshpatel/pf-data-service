package com.mayureshpatel.pfdataservice.migration;

import com.mayureshpatel.pfdataservice.service.MerchantNameNormalizer;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * One-time backfill (PF-218): applies {@link MerchantNameNormalizer} to every existing merchant
 * row still sitting with a blank {@code clean_name} from before automatic normalization existed.
 * <p>
 * A Java migration, not a {@code .sql} one, specifically so it can reuse the exact same
 * normalization function as {@code MerchantService} -- a parallel SQL-only implementation would
 * drift from it over time. Spring Boot auto-registers any {@link org.flywaydb.core.api.migration.JavaMigration}
 * bean with Flyway, so this runs exactly once, tracked in {@code flyway_schema_history} like every
 * other migration.
 */
@Component
@RequiredArgsConstructor
public class V35__BackfillMerchantCleanNames extends BaseJavaMigration {

    private final MerchantNameNormalizer nameNormalizer;

    /**
     * Reads every merchant with a blank {@code clean_name}, normalizes its {@code original_name},
     * and writes the result back in one batch.
     *
     * @param context Flyway's migration context, providing the raw JDBC connection to migrate on
     * @throws Exception if the underlying JDBC calls fail
     */
    @Override
    public void migrate(Context context) throws Exception {
        try (PreparedStatement select = context.getConnection().prepareStatement(
                "SELECT id, original_name FROM merchants WHERE clean_name = ''");
             ResultSet rows = select.executeQuery();
             PreparedStatement update = context.getConnection().prepareStatement(
                     "UPDATE merchants SET clean_name = ? WHERE id = ?")) {

            boolean hasPendingUpdates = false;
            while (rows.next()) {
                update.setString(1, nameNormalizer.normalize(rows.getString("original_name")));
                update.setLong(2, rows.getLong("id"));
                update.addBatch();
                hasPendingUpdates = true;
            }

            if (hasPendingUpdates) {
                update.executeBatch();
            }
        }
    }
}
