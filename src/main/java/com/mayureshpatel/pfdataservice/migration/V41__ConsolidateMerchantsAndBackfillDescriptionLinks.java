package com.mayureshpatel.pfdataservice.migration;

import com.mayureshpatel.pfdataservice.service.MerchantNameNormalizer;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One-time consolidation (PF-844): merges every merchant already fragmented by the old
 * regex-based {@link MerchantNameNormalizer} -- the ~342-row problem {@code PF-EPIC-045} found
 * and {@code PF-833} deferred a decision on -- into a single survivor per group, and backfills
 * {@code merchant_description_links} so every raw name that used to identify a merchant (survivor
 * or not) still matches a future import.
 * <p>
 * Same {@code @Component}/{@code @RequiredArgsConstructor}/{@code BaseJavaMigration} pattern as
 * the existing {@link V35__BackfillMerchantCleanNames}, and for the same reason: the grouping key
 * needs real branching logic a SQL-only migration can't express, and this needs to be testable.
 * <p>
 * <b>{@link MerchantNameNormalizer} is injected, not inlined.</b> The "Deliberate Merchants" guide
 * suggested inlining a copy here so the standalone service could be deleted for good once nothing
 * else references it. That premise doesn't hold: {@link V35__BackfillMerchantCleanNames} is an
 * already-shipped historical migration with its own permanent constructor dependency on {@link
 * MerchantNameNormalizer} (Flyway migrations aren't deleted once applied to any real environment,
 * matching every other {@code V*} file in this package staying in place indefinitely) -- so the
 * class can never actually become fully unreferenced regardless of what this migration does.
 * Injecting avoids duplicating ~130 lines of tokenizing/regex logic into a second location that
 * could silently drift from the original.
 * <p>
 * <b>Deliberately excludes {@code user_id IS NULL} ("global") merchant rows.</b> This migration's
 * grouping is inherently per-user, and {@code merchant_description_links.user_id} is
 * {@code NOT NULL} -- a global row has nowhere valid to write a link. Their fate (reassign or
 * delete) is a separate, explicit human decision at the pre-flight check documented on
 * {@code PF-844} before {@code V42} runs, not something this migration silently resolves.
 * <p>
 * Runs once per environment, tracked in {@code flyway_schema_history} like every other migration.
 * Must handle an empty {@code merchants} table cleanly -- Testcontainers replays every migration
 * from scratch on every integration test run.
 * <p>
 * <b>No dedicated integration test as of PF-845, one ticket after this class shipped.</b>
 * {@code V42__finalize_merchants_schema.sql} drops {@code clean_name}/{@code original_name} --
 * since Flyway always replays every migration to the latest version before any test runs, there is
 * no longer a reachable point in the test schema where a fixture could insert rows with those
 * columns to exercise this class's logic directly (the same fate {@code V35}'s own test met from
 * this same migration). The class itself stays in place -- migrations here are never deleted once
 * shipped -- only its dedicated test was removed.
 */
@Component
@RequiredArgsConstructor
public class V41__ConsolidateMerchantsAndBackfillDescriptionLinks extends BaseJavaMigration {

    private final MerchantNameNormalizer nameNormalizer;

    private record MerchantRow(long id, long userId, String originalName, String cleanName) {
    }

    /**
     * Groups every user-owned merchant row by effective name, repoints affected transactions and
     * recurring-transaction templates to a single lowest-id survivor per group, and backfills a
     * description link for every raw name (survivor's own included) that used to identify one.
     *
     * @param context Flyway's migration context, providing the raw JDBC connection to migrate on
     * @throws SQLException if any underlying JDBC call fails
     */
    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        Map<Long, List<MerchantRow>> merchantsByUser = fetchMerchantsByUser(connection);

        try (PreparedStatement setName = connection.prepareStatement(
                     "UPDATE merchants SET name = ? WHERE id = ?");
             PreparedStatement repointTransactions = connection.prepareStatement(
                     "UPDATE transactions SET merchant_id = ? WHERE merchant_id = ?");
             PreparedStatement repointRecurringTransactions = connection.prepareStatement(
                     "UPDATE recurring_transactions SET merchant_id = ? WHERE merchant_id = ?");
             PreparedStatement upsertLink = connection.prepareStatement("""
                     INSERT INTO merchant_description_links
                         (user_id, merchant_id, description, normalized_description, created_at, updated_at)
                     VALUES (?, ?, ?, ?, NOW(), NOW())
                     ON CONFLICT (user_id, normalized_description) DO UPDATE
                         SET merchant_id = excluded.merchant_id,
                             description = excluded.description,
                             updated_at = NOW()
                     """);
             PreparedStatement deleteMerchant = connection.prepareStatement(
                     "DELETE FROM merchants WHERE id = ?")) {

            for (Map.Entry<Long, List<MerchantRow>> userEntry : merchantsByUser.entrySet()) {
                long userId = userEntry.getKey();
                for (List<MerchantRow> group : groupByEffectiveName(userEntry.getValue()).values()) {
                    consolidateGroup(userId, group, setName, repointTransactions,
                            repointRecurringTransactions, upsertLink, deleteMerchant);
                }
            }
        }
    }

    /**
     * Consolidates one same-effective-name group into its lowest-id survivor. A single-row group
     * naturally degrades into "just name and link the survivor" -- the loser loop below simply
     * runs zero times, so no separate single-row branch is needed.
     */
    private void consolidateGroup(long userId, List<MerchantRow> group, PreparedStatement setName,
                                   PreparedStatement repointTransactions, PreparedStatement repointRecurringTransactions,
                                   PreparedStatement upsertLink, PreparedStatement deleteMerchant) throws SQLException {
        MerchantRow survivor = group.stream().min(Comparator.comparingLong(MerchantRow::id)).orElseThrow();

        updateName(setName, survivor.id(), effectiveName(survivor));
        upsertLink(upsertLink, userId, survivor.id(), survivor.originalName());

        for (MerchantRow row : group) {
            if (row.id() == survivor.id()) {
                continue;
            }
            // repoint FKs before deleting the loser -- transactions.merchant_id is ON DELETE SET
            // NULL (V30); deleting first would silently null it out instead of repointing it.
            repoint(repointTransactions, survivor.id(), row.id());
            repoint(repointRecurringTransactions, survivor.id(), row.id());
            upsertLink(upsertLink, userId, survivor.id(), row.originalName());

            deleteMerchant.setLong(1, row.id());
            deleteMerchant.executeUpdate();
        }
    }

    private Map<Long, List<MerchantRow>> fetchMerchantsByUser(Connection connection) throws SQLException {
        Map<Long, List<MerchantRow>> byUser = new LinkedHashMap<>();
        // excludes user_id IS NULL ("global") rows -- see class Javadoc.
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT id, user_id, original_name, clean_name
                FROM merchants
                WHERE user_id IS NOT NULL
                ORDER BY user_id, id
                """);
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                MerchantRow row = new MerchantRow(
                        rows.getLong("id"),
                        rows.getLong("user_id"),
                        rows.getString("original_name"),
                        rows.getString("clean_name"));
                byUser.computeIfAbsent(row.userId(), ignored -> new ArrayList<>()).add(row);
            }
        }
        return byUser;
    }

    /**
     * Groups one user's rows by {@code lower(trim(effectiveName))}. Preserves insertion (id)
     * order within each group via {@link LinkedHashMap} so survivor selection stays deterministic.
     */
    private Map<String, List<MerchantRow>> groupByEffectiveName(List<MerchantRow> userMerchants) {
        Map<String, List<MerchantRow>> groups = new LinkedHashMap<>();
        for (MerchantRow row : userMerchants) {
            String groupKey = effectiveName(row).trim().toLowerCase(Locale.ROOT);
            groups.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(row);
        }
        return groups;
    }

    private String effectiveName(MerchantRow row) {
        String cleanName = row.cleanName();
        if (cleanName != null && !cleanName.isBlank()) {
            return cleanName;
        }
        return nameNormalizer.normalize(row.originalName());
    }

    /**
     * Trim + casefold + whitespace-collapse -- the {@code merchant_description_links} lookup key.
     * Deliberately unrelated to {@link MerchantNameNormalizer}: no number/state stripping, just
     * exact-match-after-light-normalization, per the guide's locked matching-key decision.
     */
    private String lightNormalize(String raw) {
        return raw == null ? "" : raw.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private void updateName(PreparedStatement setName, long merchantId, String name) throws SQLException {
        setName.setString(1, name);
        setName.setLong(2, merchantId);
        setName.executeUpdate();
    }

    private void repoint(PreparedStatement repointStatement, long survivorId, long loserId) throws SQLException {
        repointStatement.setLong(1, survivorId);
        repointStatement.setLong(2, loserId);
        repointStatement.executeUpdate();
    }

    private void upsertLink(PreparedStatement upsertLink, long userId, long merchantId, String description) throws SQLException {
        upsertLink.setLong(1, userId);
        upsertLink.setLong(2, merchantId);
        upsertLink.setString(3, description);
        upsertLink.setString(4, lightNormalize(description));
        upsertLink.executeUpdate();
    }
}
