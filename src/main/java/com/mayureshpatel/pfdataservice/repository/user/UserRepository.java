package com.mayureshpatel.pfdataservice.repository.user;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.user.mapper.UserRowMapper;
import com.mayureshpatel.pfdataservice.repository.user.query.UserQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** JDBC-backed persistence for {@link User}. */
@Repository
@RequiredArgsConstructor
public class UserRepository implements JdbcRepository<User, Long>, SoftDeleteSupport {

    private static final String PARAM_EMAIL = "email";
    private static final String PARAM_USERNAME = "username";

    private final JdbcClient jdbcClient;
    private final UserRowMapper rowMapper;

    @Override
    public Optional<User> findById(Long id) {
        return this.jdbcClient.sql(UserQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    /**
     * @param email the email to look up
     * @return the matching non-deleted user, if one exists
     */
    public Optional<User> findByEmail(String email) {
        return this.jdbcClient.sql(UserQueries.FIND_BY_EMAIL)
                .param(PARAM_EMAIL, email)
                .query(rowMapper)
                .optional();
    }

    /**
     * @param username the username to look up
     * @return the matching non-deleted user, if one exists
     */
    public Optional<User> findByUsername(String username) {
        return this.jdbcClient.sql(UserQueries.FIND_BY_USERNAME)
                .param(PARAM_USERNAME, username)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<User> findAll() {
        return this.jdbcClient.sql(UserQueries.FIND_ALL)
                .query(rowMapper)
                .list();
    }


    /**
     * @param email the email to check
     * @return whether a non-deleted user with this email exists
     */
    public boolean existsByEmail(String email) {
        Integer count = this.jdbcClient.sql(UserQueries.EXISTS_BY_EMAIL)
                .param(PARAM_EMAIL, email)
                .query(Integer.class)
                .single();

        return count > 0;
    }

    /**
     * @param username the username to check
     * @return whether a non-deleted user with this username exists
     */
    public boolean existsByUsername(String username) {
        Integer count = this.jdbcClient.sql(UserQueries.EXISTS_BY_USERNAME)
                .param(PARAM_USERNAME, username)
                .query(Integer.class)
                .single();

        return count > 0;
    }

    @Override
    public int insert(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String lastUpdatedBy = (user.getAudit() != null && user.getAudit().getUpdatedBy() != null)
                ? user.getAudit().getUpdatedBy().getUsername()
                : "system";

        this.jdbcClient.sql(UserQueries.INSERT)
                .param(PARAM_USERNAME, user.getUsername())
                .param(PARAM_EMAIL, user.getEmail())
                .param("passwordHash", user.getPasswordHash())
                .param("role", user.getRole() != null ? user.getRole() : "USER")
                .param("lastUpdatedBy", lastUpdatedBy)
                .update(keyHolder);

        return Optional.ofNullable(keyHolder.getKey())
                .map(Number::intValue)
                .orElse(0);
    }

    @Override
    public int update(User user) {
        String lastUpdatedBy = (user.getAudit() != null && user.getAudit().getUpdatedBy() != null)
                ? user.getAudit().getUpdatedBy().getUsername()
                : "system";

        return this.jdbcClient.sql(UserQueries.UPDATE)
                .param(PARAM_USERNAME, user.getUsername())
                .param(PARAM_EMAIL, user.getEmail())
                .param("passwordHash", user.getPasswordHash())
                .param("role", user.getRole() != null ? user.getRole() : "USER")
                .param("lastUpdatedBy", lastUpdatedBy)
                .param("id", user.getId())
                .update();
    }

    @Override
    public int delete(User user) {
        if (user.getId() != null) {
            return deleteById(user.getId());
        }

        return 0;
    }

    @Override
    public int deleteById(Long id) {
        return this.jdbcClient.sql(UserQueries.DELETE_BY_ID)
                .param("id", id)
                .update();
    }

    /**
     * @param id the user id to check
     * @return whether a non-deleted user with this id exists
     */
    public boolean existsById(Long id) {
        Integer count = this.jdbcClient.sql(UserQueries.EXISTS_BY_ID)
                .param("id", id)
                .query(Integer.class)
                .single();

        return count > 0;
    }

    @Override
    public long count() {
        return this.jdbcClient.sql(UserQueries.COUNT)
                .query(Long.class)
                .single();
    }
}
