package com.mayureshpatel.pfdataservice.repository.user;

import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.user.mapper.UserRowMapper;
import com.mayureshpatel.pfdataservice.repository.user.query.UserQueries;
import com.mayureshpatel.pfdataservice.domain.user.User;
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
    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }

    @Override
    public User insert(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.jdbcClient.sql(UserQueries.INSERT)
                .param("username", user.getUsername())
                .param("email", user.getEmail())
                .param("passwordHash",  user.getPasswordHash())
                .update(keyHolder);

        user.setId(keyHolder.getKeyAs(Long.class));
        return user;
    }

    @Override
    public User update(User user) {
        this.jdbcClient.sql(UserQueries.UPDATE)
                .param("username", user.getUsername())
                .param("email", user.getEmail())
                .param("passwordHash",  user.getPasswordHash())
                .param("lastUpdatedBy", user.getLastUpdatedBy())
                .param("id", user.getId())
                .update();

        return user;
    }

    @Override
    public void delete(User user) {
        if (user.getId() != null) {
            deleteById(user.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        this.jdbcClient.sql(UserQueries.DELETE_BY_ID)
                .param("id", id)
                .update();
    }

    @Override
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
