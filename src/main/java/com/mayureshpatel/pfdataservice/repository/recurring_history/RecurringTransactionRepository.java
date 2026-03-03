package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.mapper.RecurringTransactionRowMapper;
import com.mayureshpatel.pfdataservice.repository.recurring_history.query.RecurringTransactionQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecurringTransactionRepository implements JdbcRepository<RecurringTransaction, Long> {

    private final JdbcClient jdbcClient;
    private final RecurringTransactionRowMapper rowMapper;

    @Override
    public List<RecurringTransaction> findAll() {
        return jdbcClient.sql("SELECT * FROM recurring_transactions WHERE deleted_at IS NULL")
                .query(rowMapper)
                .list();
    }

    public List<RecurringTransaction> findAllByUserId(Long userId) {
        return jdbcClient.sql(RecurringTransactionQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public List<RecurringTransaction> findByUserIdAndActiveTrueOrderByNextDate(Long userId) {
        return jdbcClient.sql(RecurringTransactionQueries.FIND_BY_USER_ID_ACTIVE_ORDER_BY_NEXT_DATE)
                .param("userId", userId)
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

    @Override
    public RecurringTransaction save(RecurringTransaction entity) {
        if (entity.getId() == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    @Override
    public int insert(RecurringTransaction recurringTransaction) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(RecurringTransactionQueries.INSERT)
                .param("userId", recurringTransaction.getUser().getId())
                .param("accountId", recurringTransaction.getAccount() != null ? recurringTransaction.getAccount().getId() : null)
                .param("merchantId", recurringTransaction.getMerchant() != null ? recurringTransaction.getMerchant().getId() : null)
                .param("amount", recurringTransaction.getAmount())
                .param("frequency", recurringTransaction.getFrequency().name())
                .param("lastDate", recurringTransaction.getLastDate())
                .param("nextDate", recurringTransaction.getNextDate())
                .param("active", recurringTransaction.isActive())
                .update(keyHolder);

        recurringTransaction.setId(keyHolder.getKeyAs(Long.class));

        return recurringTransaction;
    }

    @Override
    public RecurringTransaction update(RecurringTransaction recurringTransaction) {
        jdbcClient.sql(RecurringTransactionQueries.UPDATE)
                .param("accountId", recurringTransaction.getAccount() != null ? recurringTransaction.getAccount().getId() : null)
                .param("merchantId", recurringTransaction.getMerchant() != null ? recurringTransaction.getMerchant().getId() : null)
                .param("amount", recurringTransaction.getAmount())
                .param("frequency", recurringTransaction.getFrequency().name())
                .param("lastDate", recurringTransaction.getLastDate())
                .param("nextDate", recurringTransaction.getNextDate())
                .param("active", recurringTransaction.isActive())
                .param("id", recurringTransaction.getId())
                .param("userId", recurringTransaction.getUser().getId())
                .update();

        return recurringTransaction;
    }

    @Override
    public void deleteById(Long id) {
        // This is a default delete by ID, we need userId for security in our queries usually, 
        // but for repository level we might just use ID if it's unique enough.
        // However, RecurringTransactionQueries.DELETE uses both.
        // I'll add a simplified version or just use the one with userId if I have it.
        throw new UnsupportedOperationException("Use delete(Long id, Long userId)");
    }

    public void delete(Long id, Long userId) {
        jdbcClient.sql(RecurringTransactionQueries.DELETE)
                .param("id", id)
                .param("userId", userId)
                .update();
    }
}
