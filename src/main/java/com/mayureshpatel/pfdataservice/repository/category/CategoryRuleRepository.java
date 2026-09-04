package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRuleRowMapper;
import com.mayureshpatel.pfdataservice.repository.category.query.CategoryRuleQueries;
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

@Repository
@RequiredArgsConstructor
public class CategoryRuleRepository implements JdbcRepository<CategoryRule, Long> {

    private final JdbcClient jdbcClient;
    private final CategoryRuleRowMapper rowMapper;

    @Override
    public Optional<CategoryRule> findById(Long id) {
        return this.jdbcClient.sql(CategoryRuleQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional()
                .map(rule -> rule.toBuilder().keywords(findKeywordsByRuleId(id)).build());
    }

    public List<CategoryRule> findByUserId(Long userId) {
        List<CategoryRule> rules = this.jdbcClient.sql(CategoryRuleQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();

        Map<Long, List<String>> keywordsByRuleId = findKeywordsByUserId(userId);
        return rules.stream()
                .map(rule -> rule.toBuilder()
                        .keywords(keywordsByRuleId.getOrDefault(rule.getId(), List.of()))
                        .build())
                .toList();
    }

    /**
     * PF-315: a rule's keywords live in category_rule_keywords, fetched separately from the main
     * rule row and attached in Java rather than joined in SQL, since a join would turn a
     * one-row-per-rule result into one-row-per-keyword, which {@link CategoryRuleRowMapper} isn't
     * shaped for.
     */
    private List<String> findKeywordsByRuleId(Long ruleId) {
        return this.jdbcClient.sql(CategoryRuleQueries.FIND_KEYWORDS_BY_RULE_ID)
                .param("ruleId", ruleId)
                .query(String.class)
                .list();
    }

    private Map<Long, List<String>> findKeywordsByUserId(Long userId) {
        return this.jdbcClient.sql(CategoryRuleQueries.FIND_KEYWORDS_BY_USER_ID)
                .param("userId", userId)
                .query((rs, rowNum) -> Map.entry(rs.getLong("rule_id"), rs.getString("keyword")))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    public Long insertAndReturnId(CategoryRule categoryRule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql(CategoryRuleQueries.INSERT)
                .param("categoryId", categoryRule.getCategory().getId())
                .param("priority", categoryRule.getPriority())
                .param("minAmount", categoryRule.getMinAmount())
                .param("maxAmount", categoryRule.getMaxAmount())
                .param("matchType", categoryRule.getMatchType().name())
                .param("userId", categoryRule.getUser().getId())
                .update(keyHolder);

        Long ruleId = keyHolder.getKey().longValue();
        insertKeywords(ruleId, categoryRule.getKeywords());
        return ruleId;
    }

    @Override
    public int update(CategoryRule categoryRule) {
        int rows = this.jdbcClient.sql(CategoryRuleQueries.UPDATE)
                .param("categoryId", categoryRule.getCategory().getId())
                .param("priority", categoryRule.getPriority())
                .param("minAmount", categoryRule.getMinAmount())
                .param("maxAmount", categoryRule.getMaxAmount())
                .param("matchType", categoryRule.getMatchType().name())
                .param("id", categoryRule.getId())
                .param("userId", categoryRule.getUser().getId())
                .update();

        // only replace keywords if the parent row actually belonged to this user -- otherwise an
        // attacker who fails the ownership-scoped parent update could still wipe another user's
        // rule's keywords out from under them
        if (rows > 0) {
            replaceKeywords(categoryRule.getId(), categoryRule.getKeywords());
        }
        return rows;
    }

    private void insertKeywords(Long ruleId, List<String> keywords) {
        for (String keyword : keywords) {
            this.jdbcClient.sql(CategoryRuleQueries.INSERT_KEYWORD)
                    .param("ruleId", ruleId)
                    .param("keyword", keyword)
                    .update();
        }
    }

    private void replaceKeywords(Long ruleId, List<String> keywords) {
        this.jdbcClient.sql(CategoryRuleQueries.DELETE_KEYWORDS_BY_RULE_ID)
                .param("ruleId", ruleId)
                .update();
        insertKeywords(ruleId, keywords);
    }

    @Override
    public int deleteById(Long id, Long userId) {
        return this.jdbcClient.sql(CategoryRuleQueries.DELETE)
                .param("id", id)
                .param("userId", userId)
                .update();
    }

    @Override
    public int deleteById(Long id) {
        throw new UnsupportedOperationException("Use deleteById with userId");
    }

    public long countByCategoryId(Long categoryId) {
        return this.jdbcClient.sql(CategoryRuleQueries.COUNT_BY_CATEGORY_ID)
                .param("categoryId", categoryId)
                .query(Long.class)
                .single();
    }
}
