package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SqlLoader;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.TagRowMapper;
import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
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
    private final SqlLoader sqlLoader;

    @Override
    public Optional<Tag> findById(Long id) {
        String query = sqlLoader.load("sql/tag/findById.sql");
        return jdbcClient.sql(query)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Tag> findAll() {
        String query = sqlLoader.load("sql/tag/findAll.sql");
        return jdbcClient.sql(query)
                .query(rowMapper)
                .list();
    }

    public List<Tag> findByUserId(Long userId) {
        String query = sqlLoader.load("sql/tag/findByUserId.sql");
        return jdbcClient.sql(query)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public Tag insert(Tag tag) {
        String query = sqlLoader.load("sql/tag/insert.sql");
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(query)
                .param("name", tag.getName())
                .param("color", tag.getColor())
                .param("userId", tag.getUser().getId())
                .update(keyHolder);

        tag.setId(keyHolder.getKeyAs(Long.class));
        return tag;
    }

    @Override
    public Tag update(Tag tag) {
        String query = sqlLoader.load("sql/tag/update.sql");

        jdbcClient.sql(query)
                .param("name", tag.getName())
                .param("color", tag.getColor())
                .param("id", tag.getId())
                .update();

        return tag;
    }

    @Override
    public void deleteById(Long id) {
        String query = sqlLoader.load("sql/tag/deleteById.sql");
        jdbcClient.sql(query)
                .param("id", id)
                .update();
    }

    @Override
    public long count() {
        String query = sqlLoader.load("sql/tag/count.sql");
        return jdbcClient.sql(query)
                .query(Long.class)
                .single();
    }
}
