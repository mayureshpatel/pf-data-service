package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringCharge;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import com.mayureshpatel.pfdataservice.repository.recurring_history.mapper.RecurringTransactionRowMapper;
import com.mayureshpatel.pfdataservice.repository.recurring_history.query.RecurringTransactionQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JDBC-backed persistence for {@link RecurringTransaction} templates. */
@Repository
@RequiredArgsConstructor
public class RecurringTransactionRepository implements JdbcRepository<RecurringTransaction, Long> {

    private static final String MERCHANT_ID = "merchantId";
    /** how far back real charges are matched to tracked items; the same window detection uses. */
    private static final int CHARGE_LOOKBACK_YEARS = 3;

    private final JdbcClient jdbcClient;
    private final RecurringTransactionRowMapper rowMapper;

    @Override
    public List<RecurringTransaction> findAll() {
        return jdbcClient.sql("SELECT * FROM recurring_transactions WHERE deleted_at IS NULL")
                .query(rowMapper)
                .list();
    }

    /**
     * @param userId the owning user's id
     * @return every recurring transaction template the user has, active and inactive alike
     */
    public List<RecurringTransaction> findAllByUserId(Long userId) {
        return jdbcClient.sql(RecurringTransactionQueries.FIND_ALL_BY_USER_ID)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public Optional<RecurringTransaction> findById(Long id) {
        return jdbcClient.sql(RecurringTransactionQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    /**
     * @param request the template to create
     * @param userId  the owning user's id
     * @return the generated template id
     */
    public int insert(RecurringTransactionCreateRequest request, Long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(RecurringTransactionQueries.INSERT)
                .param(SqlParams.USER_ID, userId)
                .param("accountId", request.accountId())
                .param(MERCHANT_ID, request.merchantId())
                .param("amount", request.amount())
                .param("frequency", request.frequency())
                .param("lastDate", request.lastDate())
                .param("nextDate", request.nextDate())
                .param("active", request.active())
                .update(keyHolder);

        return keyHolder.getKey().intValue();
    }

    /**
     * @param request the template's new field values
     * @param userId  the requesting user's id
     * @return the number of rows updated (0 or 1)
     */
    public int update(RecurringTransactionUpdateRequest request, Long userId) {
        return jdbcClient.sql(RecurringTransactionQueries.UPDATE)
                .param("accountId", request.accountId())
                .param(MERCHANT_ID, request.merchantId())
                .param("amount", request.amount())
                .param("frequency", request.frequency())
                .param("nextDate", request.nextDate())
                .param("active", request.active())
                .param("id", request.id())
                .param(SqlParams.USER_ID, userId)
                .update();
    }

    @Override
    public int deleteById(Long id) {
        throw new UnsupportedOperationException("Use delete(Long id, Long userId)");
    }

    /**
     * Soft-deletes a template, scoped to its owner. The real delete entry point --
     * {@link #deleteById(Long)} above always throws, matching this codebase's established pattern
     * for entities where ownership scoping can't be skipped.
     *
     * @param id     the template id
     * @param userId the requesting user's id
     * @return the number of rows updated (0 or 1)
     */
    public int delete(Long id, Long userId) {
        return jdbcClient.sql(RecurringTransactionQueries.DELETE)
                .param("id", id)
                .param(SqlParams.USER_ID, userId)
                .update();
    }

    /**
     * @param accountId the account to check
     * @return the number of non-deleted recurring templates (across all users) on this account
     */
    public long countByAccountId(Long accountId) {
        return jdbcClient.sql(RecurringTransactionQueries.COUNT_BY_ACCOUNT_ID)
                .param("accountId", accountId)
                .query(Long.class)
                .single();
    }

    /**
     * Matches each of the user's tracked items to its real charges (PF-884): the user's non-deleted
     * expense transactions assigned to the item's merchant, or whose folded description equals one
     * linked to that merchant. At most the 60 newest per item, newest first. An item with no
     * matches has no entry.
     *
     * @param userId the owning user's id
     * @return recent matched charges keyed by tracked item id
     */
    public Map<Long, List<RecurringCharge>> findRecentChargesByRecurringId(Long userId) {
        Map<Long, List<RecurringCharge>> byItem = new LinkedHashMap<>();
        jdbcClient.sql(RecurringTransactionQueries.FIND_RECENT_CHARGES_BY_RECURRING_ID)
                .param(SqlParams.USER_ID, userId)
                .param("since", LocalDate.now().minusYears(CHARGE_LOOKBACK_YEARS).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime())
                .query((rs, rowNum) -> {
                    byItem.computeIfAbsent(rs.getLong("recurring_id"), id -> new ArrayList<>())
                            .add(new RecurringCharge(
                                    rs.getObject("date", OffsetDateTime.class).toLocalDate(),
                                    rs.getBigDecimal("amount")));
                    return rowNum;
                })
                .list();
        return byItem;
    }

    /**
     * How many of the user's tracked items (not deleted) still use a merchant; any makes the merchant
     * undeletable.
     *
     * @param merchantId the merchant id
     * @param userId     the owning user's id
     * @return the number of undeleted items that use the merchant
     */
    public long countActiveByMerchantId(Long merchantId, Long userId) {
        return jdbcClient.sql(RecurringTransactionQueries.COUNT_ACTIVE_BY_MERCHANT_ID)
                .param(MERCHANT_ID, merchantId)
                .param(SqlParams.USER_ID, userId)
                .query(Long.class)
                .single();
    }

    /**
     * Permanently removes the user's already-deleted tracked items that reference a merchant, so their
     * NOT NULL merchant reference no longer blocks deleting the merchant.
     *
     * @param merchantId the merchant id
     * @param userId     the owning user's id
     * @return the number of rows removed
     */
    public int purgeDeletedByMerchantId(Long merchantId, Long userId) {
        return jdbcClient.sql(RecurringTransactionQueries.PURGE_DELETED_BY_MERCHANT_ID)
                .param(MERCHANT_ID, merchantId)
                .param(SqlParams.USER_ID, userId)
                .update();
    }
}
