package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.jdbc.JdbcRepository;
import com.mayureshpatel.pfdataservice.jdbc.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.jdbc.mapper.UserRowMapper;
import com.mayureshpatel.pfdataservice.jdbc.util.SqlLoader;
import com.mayureshpatel.pfdataservice.model.User;
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
    private final SqlLoader sqlLoader;

    @Override
    public Optional<User> findById(Long id) {
        String query = this.sqlLoader.load("sql/user/findById.sql");

        return this.jdbcClient.sql(query)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    public Optional<User> findByEmail(String email) {
        String query = this.sqlLoader.load("sql/user/findByEmail.sql");

        return this.jdbcClient.sql(query)
                .param("email", email)
                .query(rowMapper)
                .optional();
    }

    public Optional<User> findByUsername(String username) {
        String query = this.sqlLoader.load("sql/user/findByUsername.sql");

        return this.jdbcClient.sql(query)
                .param("username", username)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<User> findAll() {
        String query = this.sqlLoader.load("sql/user/findAll.sql");

        return this.jdbcClient.sql(query)
                .query(rowMapper)
                .list();
    }


    public boolean existsByEmail(String email) {
        String query = this.sqlLoader.load("sql/user/existsByEmail.sql");

        Integer count = this.jdbcClient.sql(query)
                .param("email", email)
                .query(Integer.class)
                .single();

        return count > 0;
    }

    public boolean existsByUsername(String username) {
        String query = this.sqlLoader.load("sql/user/existsByUsername.sql");

        Integer count = this.jdbcClient.sql(query)
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

    private User insert(User user) {
        String query = this.sqlLoader.load("sql/user/insert.sql");
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.jdbcClient.sql(query)
                .param("username", user.getUsername())
                .param("email", user.getEmail())
                .param("passwordHash",  user.getPasswordHash())
                .update(keyHolder);

        user.setId(keyHolder.getKeyAs(Long.class));
        return user;
    }

    private User update(User user) {
        String query = this.sqlLoader.load("sql/user/update.sql");

        this.jdbcClient.sql(query)
                .param("username", user.getUsername())
                .param("email", user.getEmail())
                .param("passwordHash",  user.getPasswordHash())
                .param("lastUpdatedBy", user.getLastUpdatedBy())
                .param("id", user.getId())
                .update();

        return user;
    }

    @Override
    public void deleteById(Long id) {
        String query = this.sqlLoader.load("sql/user/deleteById.sql");

        this.jdbcClient.sql(query)
                .param("id", id)
                .update();
    }

    @Override
    public boolean existsById(Long id) {
        String query = this.sqlLoader.load("sql/user/existsById.sql");

        Integer count = this.jdbcClient.sql(query)
                .param("id", id)
                .query(Integer.class)
                .single();

        return count > 0;
    }

    @Override
    public long count() {
        String query = this.sqlLoader.load("sql/user/count.sql");

        return this.jdbcClient.sql(query)
                .query(Long.class)
                .single();
    }
}
