package com.mayureshpatel.pfdataservice.repository.tag;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.tag.mapper.TagRowMapper;
import com.mayureshpatel.pfdataservice.repository.tag.query.TagQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
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

    public List<Tag> findAllByUserId(Long userId) {
        return jdbcClient.sql(TagQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public Tag insert(Tag tag) {
        jdbcClient.sql(TagQueries.INSERT)
                .param("name", tag.getName())
                .param("color", tag.getIconography().getColor())
                .param("userId", tag.getUser().getId());

        return tag;
    }

    @Override
    public Tag update(Tag tag) {
        jdbcClient.sql(TagQueries.UPDATE)
                .param("name", tag.getName())
                .param("color", tag.getIconography().getColor())
                .param("id", tag.getId())
                .update();

        return tag;
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql(TagQueries.DELETE)
                .param("id", id)
                .update();
    }

    @Override
    public long count() {
        return jdbcClient.sql(TagQueries.COUNT)
                .query(Long.class)
                .single();
    }
}
