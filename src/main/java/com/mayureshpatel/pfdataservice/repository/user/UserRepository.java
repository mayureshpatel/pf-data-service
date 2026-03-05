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

@Repository
@RequiredArgsConstructor
public class UserRepository implements JdbcRepository<User, Long>, SoftDeleteSupport {

    private final JdbcClient jdbcClient;
    private final UserRowMapper rowMapper;

    @Override
    public Optional<User> findById(Long id) {
        return this.jdbcClient.sql(UserQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    public Optional<User> findByEmail(String email) {
        return this.jdbcClient.sql(UserQueries.FIND_BY_EMAIL)
                .param("email", email)
                .query(rowMapper)
                .optional();
    }

    public Optional<User> findByUsername(String username) {
        return this.jdbcClient.sql(UserQueries.FIND_BY_USERNAME)
                .param("username", username)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<User> findAll() {
        return this.jdbcClient.sql(UserQueries.FIND_ALL)
                .query(rowMapper)
                .list();
    }


    public boolean existsByEmail(String email) {
        Integer count = this.jdbcClient.sql(UserQueries.EXISTS_BY_EMAIL)
                .param("email", email)
                .query(Integer.class)
                .single();

        return count > 0;
    }

    public boolean existsByUsername(String username) {
        Integer count = this.jdbcClient.sql(UserQueries.EXISTS_BY_USERNAME)
                .param("username", username)
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
                .param("username", user.getUsername())
                .param("email", user.getEmail())
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
                .param("username", user.getUsername())
                .param("email", user.getEmail())
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
