package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.TagRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.TransactionRowMapper;
import com.mayureshpatel.pfdataservice.repository.SqlLoader;
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
    private final SqlLoader sqlLoader;

    @Override
    public Optional<Transaction> findById(Long id) {
        String query = sqlLoader.load("sql/transaction/findById.sql");
        Optional<Transaction> transaction = jdbcClient.sql(query)
                .param("id", id)
                .query(rowMapper)
                .optional();

        transaction.ifPresent(this::loadTags);
        return transaction;
    }

    private void loadTags(Transaction transaction) {
        String query = sqlLoader.load("sql/transaction/findTagsByTransactionId.sql");
        List<Tag> tags = jdbcClient.sql(query)
                .param("transactionId", transaction.getId())
                .query(tagRowMapper)
                .list();
        transaction.setTags(new HashSet<>(tags));
    }

    @Override
    public List<Transaction> findAll() {
        String query = sqlLoader.load("sql/transaction/findAll.sql");
        List<Transaction> transactions = jdbcClient.sql(query)
                .query(rowMapper)
                .list();

        transactions.forEach(this::loadTags);
        return transactions;
    }

    public List<Transaction> findByUserId(Long userId) {
        String query = sqlLoader.load("sql/transaction/findByUserId.sql");
        List<Transaction> transactions = jdbcClient.sql(query)
                .param("userId", userId)
                .query(rowMapper)
                .list();

        transactions.forEach(this::loadTags);
        return transactions;
    }

    @Override
    public Transaction insert(Transaction transaction) {
        String query = sqlLoader.load("sql/transaction/insert.sql");
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(query)
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
        String query = sqlLoader.load("sql/transaction/update.sql");

        jdbcClient.sql(query)
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
    public void deleteById(Long id) {
        String query = sqlLoader.load("sql/transaction/deleteById.sql");
        jdbcClient.sql(query)
                .param("id", id)
                .update();
    }

    @Override
    public long count() {
        String query = sqlLoader.load("sql/transaction/count.sql");
        return jdbcClient.sql(query)
                .query(Long.class)
                .single();
    }
}
