package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.tag.mapper.TagRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.TransactionRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.query.TransactionQueries;
import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Repository("jdbcTransactionRepository")
@RequiredArgsConstructor
public class TransactionRepository implements JdbcRepository<Transaction, Long>, SoftDeleteSupport {

    private final JdbcClient jdbcClient;
    private final TransactionRowMapper rowMapper;
    private final TagRowMapper tagRowMapper;

    @Override
    public Optional<Transaction> findById(Long id) {
        Optional<Transaction> transaction = jdbcClient.sql(TransactionQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();

        transaction.ifPresent(this::loadTags);
        return transaction;
    }

    private void loadTags(Transaction transaction) {
        List<Tag> tags = jdbcClient.sql(TransactionQueries.FIND_TAGS_BY_TRANSACTION_ID)
                .param("transactionId", transaction.getId())
                .query(tagRowMapper)
                .list();
        transaction.setTags(new HashSet<>(tags));
    }

    @Override
    public List<Transaction> findAll() {
        List<Transaction> transactions = jdbcClient.sql(TransactionQueries.FIND_ALL)
                .query(rowMapper)
                .list();

        transactions.forEach(this::loadTags);
        return transactions;
    }

    public List<Transaction> findByUserId(Long userId) {
        List<Transaction> transactions = jdbcClient.sql(TransactionQueries.FIND_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();

        transactions.forEach(this::loadTags);
        return transactions;
    }

    @Override
    public Transaction insert(Transaction transaction) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(TransactionQueries.INSERT)
                .param("amount", transaction.getAmount())
                .param("date", transaction.getDate())
                .param("postDate", transaction.getPostDate())
                .param("description", transaction.getDescription())
                .param("originalVendorName", transaction.getOriginalVendorName())
                .param("vendorName", transaction.getVendorName())
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
                .param("date", transaction.getDate())
                .param("postDate", transaction.getPostDate())
                .param("description", transaction.getDescription())
                .param("originalVendorName", transaction.getOriginalVendorName())
                .param("vendorName", transaction.getVendorName())
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
}
