package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.dto.transaction.recurring.DismissedPatternDto;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JDBC-backed persistence for the recurring patterns a user dismissed (PF-884). Every query is
 * scoped to the owning user in SQL itself.
 */
@Repository
@RequiredArgsConstructor
public class DismissedPatternRepository {

    private static final String PATTERN_KEY = "patternKey";

    // language=SQL
    private static final String FIND_BY_USER_ID = """
            select id, pattern_key, name, created_at
            from dismissed_recurring_patterns
            where user_id = :userId
            order by created_at desc, id desc
            """;

    // language=SQL
    private static final String UPSERT = """
            insert into dismissed_recurring_patterns (user_id, pattern_key, name)
            values (:userId, :patternKey, :name)
            on conflict (user_id, pattern_key) do update set name = excluded.name
            returning id
            """;

    // language=SQL
    private static final String DELETE_BY_ID_AND_USER_ID = """
            delete from dismissed_recurring_patterns
            where id = :id
              and user_id = :userId
            """;

    private final JdbcClient jdbcClient;

    /**
     * @param userId the owning user's id
     * @return the user's dismissals, newest first
     */
    public List<DismissedPatternDto> findByUserId(Long userId) {
        return jdbcClient.sql(FIND_BY_USER_ID)
                .param(SqlParams.USER_ID, userId)
                .query((rs, rowNum) -> new DismissedPatternDto(
                        rs.getLong("id"),
                        rs.getString("pattern_key"),
                        rs.getString("name"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class)))
                .list();
    }

    /**
     * @param userId the owning user's id
     * @return the folded keys of every pattern the user dismissed
     */
    public Set<String> findKeysByUserId(Long userId) {
        return new HashSet<>(findByUserId(userId).stream().map(DismissedPatternDto::patternKey).toList());
    }

    /**
     * Records a dismissal; dismissing the same key again keeps one row (and refreshes its name).
     *
     * @param userId     the owning user's id
     * @param patternKey the folded pattern key
     * @param name       the detected name as shown to the user
     * @return the dismissal id
     */
    public Long insert(Long userId, String patternKey, String name) {
        return jdbcClient.sql(UPSERT)
                .param(SqlParams.USER_ID, userId)
                .param(PATTERN_KEY, patternKey)
                .param("name", name)
                .query(Long.class)
                .single();
    }

    /**
     * @param id     the dismissal id
     * @param userId the owning user's id
     * @return the number of rows deleted (0 when the dismissal is not the user's)
     */
    public int deleteByIdAndUserId(Long id, Long userId) {
        return jdbcClient.sql(DELETE_BY_ID_AND_USER_ID)
                .param("id", id)
                .param(SqlParams.USER_ID, userId)
                .update();
    }
}
