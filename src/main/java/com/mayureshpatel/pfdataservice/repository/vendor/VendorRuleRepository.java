package com.mayureshpatel.pfdataservice.repository.vendor;

import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.domain.vendor.VendorRule;
import com.mayureshpatel.pfdataservice.repository.vendor.mapper.VendorRuleRowMapper;
import com.mayureshpatel.pfdataservice.repository.vendor.query.VendorRuleQueries;
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

    @Override
    public Optional<VendorRule> findById(Long id) {
        return jdbcClient.sql(VendorRuleQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    public List<VendorRule> findByUserOrGlobal(Long userId) {
        return jdbcClient.sql(VendorRuleQueries.FIND_BY_USER_OR_GLOBAL)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public VendorRule insert(VendorRule rule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(VendorRuleQueries.INSERT)
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
        jdbcClient.sql(VendorRuleQueries.UPDATE)
                .param("keyword", rule.getKeyword())
                .param("vendorName", rule.getVendorName())
                .param("priority", rule.getPriority())
                .param("id", rule.getId())
                .update();

        return rule;
    }

    @Override
    public VendorRule save(VendorRule rule) {
        if (rule.getId() == null) {
            return insert(rule);
        } else {
            return update(rule);
        }
    }

    @Override
    public void delete(VendorRule rule) {
        if (rule.getId() != null) {
            deleteById(rule.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql(VendorRuleQueries.DELETE_BY_ID)
                .param("id", id)
                .update();
    }

    @Override
    public long count() {
        return jdbcClient.sql(VendorRuleQueries.COUNT)
                .query(Long.class)
                .single();
    }
}
