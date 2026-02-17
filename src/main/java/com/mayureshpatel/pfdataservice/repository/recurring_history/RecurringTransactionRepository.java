package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.mapper.RecurringTransactionRowMapper;
import com.mayureshpatel.pfdataservice.repository.recurring_history.query.RecurringTransactionQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    public RecurringTransaction insert(RecurringTransaction recurringTransaction) {
        jdbcClient.sql(RecurringTransactionQueries.INSERT)
                .param("userId", recurringTransaction.getUser().getId())
                .param("accountId", recurringTransaction.getAccount().getId())
                .param("merchantName", recurringTransaction.getVendor().getName())
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
                .param("accountId", recurringTransaction.getAccount().getId())
                .param("merchantName", recurringTransaction.getVendor().getName())
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
