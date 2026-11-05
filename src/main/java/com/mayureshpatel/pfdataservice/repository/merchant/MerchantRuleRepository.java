package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRuleRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.query.MerchantRuleQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JDBC-backed persistence for {@link MerchantRule} (PF-904). A rule's keyword set lives in a
 * separate {@code merchant_rule_keywords} table and is assembled onto the rule in Java rather
 * than joined in SQL, exactly as {@code CategoryRuleRepository} does for category rules.
 */
@Repository
@RequiredArgsConstructor
public class MerchantRuleRepository implements JdbcRepository<MerchantRule, Long> {

    private static final String RULE_ID = "ruleId";

    private final JdbcClient jdbcClient;
    private final MerchantRuleRowMapper rowMapper;

    @Override
    public Optional<MerchantRule> findById(Long id) {
        return this.jdbcClient.sql(MerchantRuleQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional()
                .map(rule -> rule.toBuilder().keywords(findKeywordsByRuleId(id)).build());
    }

    /**
     * @param userId the owning user's id
     * @return every merchant rule the user has, each with its keyword set attached, in priority
     *         order (priority desc, summed keyword length desc, id asc)
     */
    public List<MerchantRule> findByUserId(Long userId) {
        List<MerchantRule> rules = this.jdbcClient.sql(MerchantRuleQueries.FIND_ALL_BY_USER_ID)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .list();

        Map<Long, List<String>> keywordsByRuleId = findKeywordsByUserId(userId);
        return rules.stream()
                .map(rule -> rule.toBuilder()
                        .keywords(keywordsByRuleId.getOrDefault(rule.getId(), List.of()))
                        .build())
                .toList();
    }

    private List<String> findKeywordsByRuleId(Long ruleId) {
        return this.jdbcClient.sql(MerchantRuleQueries.FIND_KEYWORDS_BY_RULE_ID)
                .param(RULE_ID, ruleId)
                .query(String.class)
                .list();
    }

    private Map<Long, List<String>> findKeywordsByUserId(Long userId) {
        return this.jdbcClient.sql(MerchantRuleQueries.FIND_KEYWORDS_BY_USER_ID)
                .param(SqlParams.USER_ID, userId)
                .query((rs, rowNum) -> Map.entry(rs.getLong("rule_id"), rs.getString("keyword")))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    /**
     * Inserts the rule row, then its keyword set as separate {@code merchant_rule_keywords} rows.
     *
     * @param merchantRule the rule to create, with its keywords already populated
     * @return the generated rule id
     */
    public Long insertAndReturnId(MerchantRule merchantRule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql(MerchantRuleQueries.INSERT)
                .param("merchantId", merchantRule.getMerchant().getId())
                .param("priority", merchantRule.getPriority())
                .param("minAmount", merchantRule.getMinAmount())
                .param("maxAmount", merchantRule.getMaxAmount())
                .param("matchType", merchantRule.getMatchType().name())
                .param(SqlParams.USER_ID, merchantRule.getUser().getId())
                .update(keyHolder);

        Long ruleId = keyHolder.getKey().longValue();
        insertKeywords(ruleId, merchantRule.getKeywords());
        return ruleId;
    }

    @Override
    public int update(MerchantRule merchantRule) {
        int rows = this.jdbcClient.sql(MerchantRuleQueries.UPDATE)
                .param("merchantId", merchantRule.getMerchant().getId())
                .param("priority", merchantRule.getPriority())
                .param("minAmount", merchantRule.getMinAmount())
                .param("maxAmount", merchantRule.getMaxAmount())
                .param("matchType", merchantRule.getMatchType().name())
                .param("id", merchantRule.getId())
                .param(SqlParams.USER_ID, merchantRule.getUser().getId())
                .update();

        // only replace keywords if the parent row actually belonged to this user, so a rejected
        // update can never wipe another user's keywords
        if (rows > 0) {
            replaceKeywords(merchantRule.getId(), merchantRule.getKeywords());
        }
        return rows;
    }

    private void insertKeywords(Long ruleId, List<String> keywords) {
        for (String keyword : keywords) {
            this.jdbcClient.sql(MerchantRuleQueries.INSERT_KEYWORD)
                    .param(RULE_ID, ruleId)
                    .param("keyword", keyword)
                    .update();
        }
    }

    private void replaceKeywords(Long ruleId, List<String> keywords) {
        this.jdbcClient.sql(MerchantRuleQueries.DELETE_KEYWORDS_BY_RULE_ID)
                .param(RULE_ID, ruleId)
                .update();
        insertKeywords(ruleId, keywords);
    }

    @Override
    public int deleteById(Long id, Long userId) {
        return this.jdbcClient.sql(MerchantRuleQueries.DELETE)
                .param("id", id)
                .param(SqlParams.USER_ID, userId)
                .update();
    }

    @Override
    public int deleteById(Long id) {
        throw new UnsupportedOperationException("Use deleteById with userId");
    }
}
