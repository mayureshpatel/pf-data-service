package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.mapper.RecurringTransactionRowMapper;
import com.mayureshpatel.pfdataservice.repository.recurring_history.query.RecurringTransactionQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecurringTransactionRepository implements JdbcRepository<RecurringTransaction, Long> {

    private final JdbcClient jdbcClient;
    private final RecurringTransactionRowMapper rowMapper;

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

    public RecurringTransaction insert(RecurringTransaction recurringTransaction) {
        jdbcClient.sql(RecurringTransactionQueries.INSERT)
                .param("userId", recurringTransaction.getUser().getId())
                .param("accountId", recurringTransaction.getAccount() != null ? recurringTransaction.getAccount().getId() : null)
                .param("merchantName", recurringTransaction.getMerchant() != null ? recurringTransaction.getMerchant().getName() : null)
                .param("amount", recurringTransaction.getAmount())
                .param("frequency", recurringTransaction.getFrequency().name())
                .param("lastDate", recurringTransaction.getLastDate())
                .param("nextDate", recurringTransaction.getNextDate())
                .param("active", recurringTransaction.isActive())
                .update();

        return recurringTransaction;
    }

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
                .update();

        return recurringTransaction;
    }

    public void delete(Long id, Long userId) {
        jdbcClient.sql(RecurringTransactionQueries.DELETE)
                .param("id", id)
                .param("userId", userId)
                .update();
    }
}
