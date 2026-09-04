package com.mayureshpatel.pfdataservice.repository.tag;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.tag.mapper.TagRowMapper;
import com.mayureshpatel.pfdataservice.repository.tag.query.TagQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("jdbcTagRepository")
@RequiredArgsConstructor
public class TagRepository implements JdbcRepository<Tag, Long> {

    private final JdbcClient jdbcClient;
    private final TagRowMapper rowMapper;

    @Override
    public Optional<Tag> findById(Long id) {
        return jdbcClient.sql(TagQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    @Override
    public Optional<Tag> findById(Long id, Long userId) {
        return jdbcClient.sql(TagQueries.FIND_BY_ID_AND_USER_ID)
                .param("id", id)
                .param("userId", userId)
                .query(rowMapper)
                .optional();
    }

    public List<Tag> findAllByUserId(Long userId) {
        return jdbcClient.sql(TagQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    /**
     * PF-307: renamed from {@code insert} -- {@code JdbcRepository<Tag, Long>.insert(T)} is
     * {@code int}-returning, so a {@code Long}-returning override of the same name doesn't
     * compile (return types aren't covariant between {@code int} and {@code Long}). Previously
     * returned the row-count from {@code update(keyHolder)} while never reading the actual
     * generated key back out of the holder, so every real caller of the discarded id would have
     * gotten the wrong value (always {@code 1} on a successful single-row insert, not the real
     * tag id) -- caught here before this method got a real caller, not after.
     */
    public Long insertAndReturnId(Tag tag) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(TagQueries.INSERT)
                .param("name", tag.getName())
                .param("color", tag.getColor())
                .param("userId", tag.getUserId())
                .update(keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * PF-307: now ownership-scoped ({@code tag.getUserId()} flows into the query's
     * {@code user_id} predicate) -- previously matched by id alone.
     */
    @Override
    public int update(Tag tag) {
        return jdbcClient.sql(TagQueries.UPDATE)
                .param("name", tag.getName())
                .param("color", tag.getColor())
                .param("id", tag.getId())
                .param("userId", tag.getUserId())
                .update();
    }

    @Override
    public int deleteById(Long id, Long userId) {
        return jdbcClient.sql(TagQueries.DELETE)
                .param("id", id)
                .param("userId", userId)
                .update();
    }

    @Override
    public int deleteById(Long id) {
        throw new UnsupportedOperationException("Use deleteById with userId");
    }

    @Override
    public long count() {
        return jdbcClient.sql(TagQueries.COUNT)
                .query(Long.class)
                .single();
    }

    /**
     * PF-307: the transaction_tags join-table operations below live here rather than on
     * TransactionRepository -- they're tag-domain functionality (TagController is what exposes
     * them), and the queries they use were previously dead code sitting unused on
     * TransactionQueries.
     */
    public List<Tag> findByTransactionId(Long transactionId) {
        return jdbcClient.sql(TagQueries.FIND_BY_TRANSACTION_ID)
                .param("transactionId", transactionId)
                .query(rowMapper)
                .list();
    }

    public int insertTransactionTag(Long transactionId, Long tagId) {
        return jdbcClient.sql(TagQueries.INSERT_TRANSACTION_TAG)
                .param("transactionId", transactionId)
                .param("tagId", tagId)
                .update();
    }

    public int deleteTransactionTag(Long transactionId, Long tagId) {
        return jdbcClient.sql(TagQueries.DELETE_TRANSACTION_TAG)
                .param("transactionId", transactionId)
                .param("tagId", tagId)
                .update();
    }
}
