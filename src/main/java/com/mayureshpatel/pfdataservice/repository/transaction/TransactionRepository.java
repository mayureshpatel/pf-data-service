package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.TransactionRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.query.TransactionQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository("jdbcTransactionRepository")
@RequiredArgsConstructor
public class TransactionRepository implements JdbcRepository<Transaction, Long>, SoftDeleteSupport {

    private final JdbcClient jdbcClient;
    private final TransactionRowMapper rowMapper;

    @Override
    public Optional<Transaction> findById(Long id) {
        return jdbcClient.sql(TransactionQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Transaction> findAll() {
        return jdbcClient.sql(TransactionQueries.FIND_ALL)
                .query(rowMapper)
                .list();
    }

    public List<Transaction> findByUserId(Long userId) {
        return jdbcClient.sql(TransactionQueries.FIND_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public boolean existsByAccountIdAndDateAndAmountAndDescriptionAndType(
            Long accountId,
            OffsetDateTime transactionDate,
            BigDecimal amount,
            String description,
            TransactionType type
    ) {
        return jdbcClient.sql(TransactionQueries.FIND_BY_ACCOUNT_ID_AND_DATE_AND_AMOUNT_AND_DESCRIPTION_AND_TYPE)
                .param("accountId", accountId)
                .param("transactionDate", transactionDate)
                .param("amount", amount)
                .param("description", description)
                .param("type", type)
                .query(rowMapper)
                .optional().isPresent();
    }

    @Override
    public Transaction insert(Transaction transaction) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(TransactionQueries.INSERT)
                .param("amount", transaction.getAmount())
                .param("date", transaction.getTransactionDate())
                .param("postDate", transaction.getPostDate())
                .param("description", transaction.getDescription())
                .param("merchantId", transaction.getMerchant().getId())
                .param("type", transaction.getType().name())
                .param("accountId", transaction.getAccount().getId())
                .param("categoryId", transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .update(keyHolder);

        transaction.setId(keyHolder.getKeyAs(Long.class));

        return transaction;
    }

    @Override
    public Transaction update(Transaction transaction) {
        jdbcClient.sql(TransactionQueries.UPDATE)
                .param("amount", transaction.getAmount())
                .param("date", transaction.getTransactionDate())
                .param("postDate", transaction.getPostDate())
                .param("description", transaction.getDescription())
                .param("merchantId", transaction.getMerchant().getId())
                .param("type", transaction.getType().name())
                .param("accountId", transaction.getAccount().getId())
                .param("categoryId", transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .param("id", transaction.getId())
                .update();

        return transaction;
    }

    @Override
    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            return insert(transaction);
        } else {
            return update(transaction);
        }
    }

    public List<Transaction> saveAll(List<Transaction> transactions) {
        return transactions.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Transaction transaction) {
        if (transaction.getId() != null) {
            deleteById(transaction.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql(TransactionQueries.DELETE_BY_ID)
                .param("id", id)
                .update();
    }

    @Override
    public long count() {
        return jdbcClient.sql(TransactionQueries.COUNT)
                .query(Long.class)
                .single();
    }

    public long countByUserId(Long accountId) {
        return jdbcClient.sql(TransactionQueries.COUNT_BY_ACCOUNT_ID)
                .param("accountId", accountId)
                .query(Long.class)
                .single();
    }
}
