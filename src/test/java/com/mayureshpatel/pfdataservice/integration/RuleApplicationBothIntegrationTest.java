package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyRule;
import com.mayureshpatel.pfdataservice.dto.rule.CreateBothAndApplyRequest;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-934: creating a category rule and a merchant rule together against a real Postgres -- both
 * applied to the chosen rows with previous values reported, a new merchant created in the same
 * step, and all-or-nothing across the two kinds when the second part fails.
 */
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM category_rules WHERE user_id = 950;",
        "DELETE FROM merchant_rules WHERE user_id = 950;",
        "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (950, 'pf934u950', 'pf934u950@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
        "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (951, 'pf934u951', 'pf934u951@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
        "INSERT INTO categories (id, user_id, name, type) VALUES (9501, 950, 'PF934 Dining', 'EXPENSE') ON CONFLICT DO NOTHING;",
        "INSERT INTO categories (id, user_id, name, type) VALUES (9502, 950, 'PF934 Fast Food', 'EXPENSE') ON CONFLICT DO NOTHING;",
        "INSERT INTO merchants (id, user_id, name) VALUES (9501, 950, 'PF934 Old Merchant') ON CONFLICT DO NOTHING;",
        "INSERT INTO merchants (id, user_id, name) VALUES (9502, 950, 'PF934 Taco Bell') ON CONFLICT DO NOTHING;",
        "INSERT INTO merchants (id, user_id, name) VALUES (9503, 951, 'PF934 Theirs') ON CONFLICT DO NOTHING;",
        "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9501, 950, 'PF934 Mine', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
        "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9502, 951, 'PF934 Theirs', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
        "INSERT INTO transactions (id, account_id, category_id, merchant_id, amount, date, description, type) VALUES (95011, 9501, NULL, NULL, 10.00, NOW(), 'PF934 TACO BELL 1', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = NULL, merchant_id = NULL, deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, category_id, merchant_id, amount, date, description, type) VALUES (95012, 9501, 9501, 9501, 10.00, NOW(), 'PF934 TACO BELL 2', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = 9501, merchant_id = 9501, deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, category_id, merchant_id, amount, date, description, type) VALUES (95014, 9502, NULL, NULL, 10.00, NOW(), 'PF934 TACO BELL 4', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = NULL, merchant_id = NULL, deleted_at = NULL;"
})
class RuleApplicationBothIntegrationTest extends BaseIntegrationTest {

    private static final long USER_ID = 950L;
    private static final long FAST_FOOD = 9502L;
    private static final long TACO_BELL = 9502L;
    private static final String KEYWORD = "pf934 taco bell";
    private static final String URL = "/api/v1/rule-applications/create-both-and-apply";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcClient jdbcClient;
    @Autowired
    private CategoryRuleRepository categoryRuleRepository;
    @Autowired
    private MerchantRuleRepository merchantRuleRepository;

    private String body(MerchantRuleApplyRule merchantRule, List<Long> categoryIds, List<Long> merchantIds) throws Exception {
        CategoryRuleCreateRequest category = CategoryRuleCreateRequest.builder()
                .userId(USER_ID).categoryId(FAST_FOOD).keywords(List.of(KEYWORD)).build();
        return objectMapper.writeValueAsString(new CreateBothAndApplyRequest(
                new CategoryRuleApplyRequest(category, categoryIds),
                new MerchantRuleApplyRequest(merchantRule, merchantIds)));
    }

    private MerchantRuleApplyRule existingMerchant() {
        return new MerchantRuleApplyRule(TACO_BELL, null, List.of(KEYWORD), null, null, null, null);
    }

    private Long categoryOf(long id) {
        return jdbcClient.sql("select category_id from transactions where id = :id")
                .param("id", id).query(Long.class).optional().orElse(null);
    }

    private Long merchantOf(long id) {
        return jdbcClient.sql("select merchant_id from transactions where id = :id")
                .param("id", id).query(Long.class).optional().orElse(null);
    }

    private long categoryRules() {
        return categoryRuleRepository.findByUserId(USER_ID).stream().filter(r -> r.getKeywords().contains(KEYWORD)).count();
    }

    private long merchantRules() {
        return merchantRuleRepository.findByUserId(USER_ID).stream().filter(r -> r.getKeywords().contains(KEYWORD)).count();
    }

    @Test
    @DisplayName("should create both rules and apply each to its own chosen rows, reporting previous values")
    @WithCustomMockUser(id = USER_ID)
    void shouldCreateAndApplyBothKinds() throws Exception {
        // act -- the category rule takes both rows, the merchant rule only the first
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(existingMerchant(), List.of(95011L, 95012L), List.of(95011L))))
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.ruleId").isNumber())
                .andExpect(jsonPath("$.category.changes.length()").value(2))
                .andExpect(jsonPath("$.merchant.ruleId").isNumber())
                .andExpect(jsonPath("$.merchant.changes.length()").value(1));

        assertThat(categoryOf(95011L)).isEqualTo(FAST_FOOD);
        assertThat(categoryOf(95012L)).isEqualTo(FAST_FOOD);
        assertThat(merchantOf(95011L)).isEqualTo(TACO_BELL);
        assertThat(merchantOf(95012L)).isEqualTo(9501L);
        assertThat(categoryRules()).isEqualTo(1);
        assertThat(merchantRules()).isEqualTo(1);
    }

    @Test
    @DisplayName("should save nothing, not even the category part, when the merchant part fails")
    @WithCustomMockUser(id = USER_ID)
    void shouldSaveNothingWhenTheSecondKindFails() throws Exception {
        // act -- the merchant part names a transaction that is not the user's
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(existingMerchant(), List.of(95011L), List.of(95014L))))
                // assert & verify
                .andExpect(status().isForbidden());

        assertThat(categoryOf(95011L)).isNull();
        assertThat(categoryRules()).isZero();
        assertThat(merchantRules()).isZero();
    }

    @Test
    @DisplayName("should create a new merchant in the same step, and keep nothing of it when the category part fails first")
    @WithCustomMockUser(id = USER_ID)
    void shouldCreateTheMerchantInTheSameAllOrNothingStep() throws Exception {
        // arrange -- the merchant is new; the category part names a foreign transaction and fails
        MerchantRuleApplyRule named = new MerchantRuleApplyRule(null, "PF934 Brand New", List.of(KEYWORD), null, null, null, null);

        // act
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(named, List.of(95014L), List.of(95011L))))
                // assert & verify
                .andExpect(status().isForbidden());
        assertThat(jdbcClient.sql("select count(*) from merchants where user_id = 950 and name = 'PF934 Brand New'")
                .query(Integer.class).single()).isZero();

        // act -- now the same new merchant succeeds
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(named, List.of(95011L), List.of(95011L))))
                .andExpect(status().isOk());
        assertThat(jdbcClient.sql("select count(*) from merchants where user_id = 950 and name = 'PF934 Brand New'")
                .query(Integer.class).single()).isEqualTo(1);
    }
}
