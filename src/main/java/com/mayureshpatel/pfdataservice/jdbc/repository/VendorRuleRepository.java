package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.jdbc.JdbcRepository;
import com.mayureshpatel.pfdataservice.jdbc.mapper.VendorRuleRowMapper;
import com.mayureshpatel.pfdataservice.jdbc.util.SqlLoader;
import com.mayureshpatel.pfdataservice.model.VendorRule;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("jdbcVendorRuleRepository")
@RequiredArgsConstructor
public class VendorRuleRepository implements JdbcRepository<VendorRule, Long> {

    private final JdbcClient jdbcClient;
    private final VendorRuleRowMapper rowMapper;
    private final SqlLoader sqlLoader;

    @Override
    public Optional<VendorRule> findById(Long id) {
        String query = sqlLoader.load("sql/vendor-rule/findById.sql");
        return jdbcClient.sql(query)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    public List<VendorRule> findByUserOrGlobal(Long userId) {
        String query = sqlLoader.load("sql/vendor-rule/findByUserOrGlobal.sql");
        return jdbcClient.sql(query)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public VendorRule insert(VendorRule rule) {
        String query = sqlLoader.load("sql/vendor-rule/insert.sql");
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(query)
                .param("keyword", rule.getKeyword())
                .param("vendorName", rule.getVendorName())
                .param("priority", rule.getPriority())
                .param("userId", rule.getUser() != null ? rule.getUser().getId() : null)
                .update(keyHolder);

        rule.setId(keyHolder.getKeyAs(Long.class));
        return rule;
    }

    @Override
    public VendorRule update(VendorRule rule) {
        String query = sqlLoader.load("sql/vendor-rule/update.sql");

        jdbcClient.sql(query)
                .param("keyword", rule.getKeyword())
                .param("vendorName", rule.getVendorName())
                .param("priority", rule.getPriority())
                .param("id", rule.getId())
                .update();

        return rule;
    }

    @Override
    public void deleteById(Long id) {
        String query = sqlLoader.load("sql/vendor-rule/deleteById.sql");
        jdbcClient.sql(query)
                .param("id", id)
                .update();
    }

    @Override
    public long count() {
        String query = sqlLoader.load("sql/vendor-rule/count.sql");
        return jdbcClient.sql(query)
                .query(Long.class)
                .single();
    }
}
