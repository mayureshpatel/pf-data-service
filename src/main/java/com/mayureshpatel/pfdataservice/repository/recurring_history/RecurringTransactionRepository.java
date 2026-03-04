package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
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

    public int insert(RecurringTransactionCreateRequest request, Long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        return jdbcClient.sql(RecurringTransactionQueries.INSERT)
                .param("userId", userId)
                .param("accountId", request.getAccountId())
                .param("merchantId", request.getMerchantId())
                .param("amount", request.getAmount())
                .param("frequency", request.getFrequency())
                .param("lastDate", request.getLastDate())
                .param("nextDate", request.getNextDate())
                .param("active", request.isActive())
                .update(keyHolder);
    }

    public int update(RecurringTransactionUpdateRequest request, Long userId) {
        return jdbcClient.sql(RecurringTransactionQueries.UPDATE)
                .param("accountId", request.getAccountId())
                .param("merchantId", request.getMerchantId())
                .param("amount", request.getAmount())
                .param("frequency", request.getFrequency())
                .param("nextDate", request.getNextDate())
                .param("active", request.isActive())
                .param("id", request.getId())
                .param("userId", userId)
                .update();
    }

    @Override
    public int deleteById(Long id) {
        throw new UnsupportedOperationException("Use delete(Long id, Long userId)");
    }

    public int delete(Long id, Long userId) {
        return jdbcClient.sql(RecurringTransactionQueries.DELETE)
                .param("id", id)
                .param("userId", userId)
                .update();
    }
}
