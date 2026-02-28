package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.RecurringTransactionDto;
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

    public RecurringTransactionDto save(RecurringTransactionDto entity) {
        if (entity.id() == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    public RecurringTransactionDto insert(RecurringTransactionDto recurringTransaction) {
        jdbcClient.sql(RecurringTransactionQueries.INSERT)
                .param("userId", recurringTransaction.userId())
                .param("accountId", recurringTransaction.account() != null ? recurringTransaction.account().id() : null)
                .param("merchantName", recurringTransaction.merchant() != null ? recurringTransaction.merchant().originalName() : null)
                .param("amount", recurringTransaction.amount())
                .param("frequency", recurringTransaction.frequency().name())
                .param("lastDate", recurringTransaction.lastDate())
                .param("nextDate", recurringTransaction.nextDate())
                .param("active", recurringTransaction.active())
                .update();

        return recurringTransaction;
    }

    public RecurringTransactionDto update(RecurringTransactionDto recurringTransaction) {
        jdbcClient.sql(RecurringTransactionQueries.UPDATE)
                .param("accountId", recurringTransaction.account() != null ? recurringTransaction.account().id() : null)
                .param("merchantId", recurringTransaction.merchant() != null ? recurringTransaction.merchant().id() : null)
                .param("amount", recurringTransaction.amount())
                .param("frequency", recurringTransaction.frequency().name())
                .param("lastDate", recurringTransaction.lastDate())
                .param("nextDate", recurringTransaction.nextDate())
                .param("active", recurringTransaction.active())
                .param("id", recurringTransaction.id())
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
