package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyRule;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleCreateRequest;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.service.MerchantRuleService;
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
 * PF-922: undoing a create-and-apply against a real Postgres -- a clean undo of a category rule, a
 * merchant rule and both together, a row changed since being left alone, another user's rows and
 * rules being left alone, and an already-deleted rule not stopping the restore.
 */
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM category_rules WHERE user_id IN (940, 941);",
        "DELETE FROM merchant_rules WHERE user_id IN (940, 941);",
        "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (940, 'pf922u940', 'pf922u940@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
        "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (941, 'pf922u941', 'pf922u941@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
        "INSERT INTO categories (id, user_id, name, type) VALUES (9401, 940, 'PF922 Dining', 'EXPENSE') ON CONFLICT DO NOTHING;",
        "INSERT INTO categories (id, user_id, name, type) VALUES (9402, 940, 'PF922 Fast Food', 'EXPENSE') ON CONFLICT DO NOTHING;",
        "INSERT INTO merchants (id, user_id, name) VALUES (9401, 940, 'PF922 Old Merchant') ON CONFLICT DO NOTHING;",
        "INSERT INTO merchants (id, user_id, name) VALUES (9402, 940, 'PF922 Taco Bell') ON CONFLICT DO NOTHING;",
        "INSERT INTO merchants (id, user_id, name) VALUES (9403, 941, 'PF922 Theirs') ON CONFLICT DO NOTHING;",
        "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9401, 940, 'PF922 Mine', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
        "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9402, 941, 'PF922 Theirs', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
        "INSERT INTO transactions (id, account_id, category_id, merchant_id, amount, date, description, type) VALUES (94011, 9401, NULL, NULL, 10.00, NOW(), 'PF922 TACO BELL 1', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = NULL, merchant_id = NULL, deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, category_id, merchant_id, amount, date, description, type) VALUES (94012, 9401, 9401, 9401, 10.00, NOW(), 'PF922 TACO BELL 2', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = 9401, merchant_id = 9401, deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, category_id, merchant_id, amount, date, description, type) VALUES (94014, 9402, 9402, 9402, 10.00, NOW(), 'PF922 TACO BELL 4', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = 9402, merchant_id = 9402, deleted_at = NULL;"
})
class RuleApplicationUndoIntegrationTest extends BaseIntegrationTest {

    private static final long USER_ID = 940L;
    private static final long DINING = 9401L;
    private static final long FAST_FOOD = 9402L;
    private static final long OLD_MERCHANT = 9401L;
    private static final long TACO_BELL = 9402L;
    private static final long THEIR_MERCHANT = 9403L;
    private static final String KEYWORD = "pf922 taco bell";
    private static final String RESTORED = "$.restoredCount";
    private static final String CATEGORY_RULE_DELETED = "$.categoryRuleDeleted";
    private static final String MERCHANT_RULE_DELETED = "$.merchantRuleDeleted";

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
    @Autowired
    private MerchantRuleService merchantRuleService;

    private JsonNode applyCategory(List<Long> ids) throws Exception {
        CategoryRuleCreateRequest rule = CategoryRuleCreateRequest.builder()
                .userId(USER_ID).categoryId(FAST_FOOD).keywords(List.of(KEYWORD)).build();
        String json = mockMvc.perform(post("/api/v1/category-rules/create-and-apply").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRuleApplyRequest(rule, ids))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private JsonNode applyMerchant(List<Long> ids) throws Exception {
        MerchantRuleApplyRule rule = new MerchantRuleApplyRule(TACO_BELL, null, List.of(KEYWORD), null, null, null, null);
        String json = mockMvc.perform(post("/api/v1/merchant-rules/create-and-apply").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MerchantRuleApplyRequest(rule, ids))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private org.springframework.test.web.servlet.ResultActions undo(JsonNode category, JsonNode merchant) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        if (category != null) {
            body.set("category", category);
        }
        if (merchant != null) {
            body.set("merchant", merchant);
        }
        return mockMvc.perform(post("/api/v1/rule-applications/undo").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
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
        return categoryRuleRepository.findByUserId(USER_ID).stream()
                .filter(r -> r.getKeywords().contains(KEYWORD)).count();
    }

    private long merchantRules() {
        return merchantRuleRepository.findByUserId(USER_ID).stream()
                .filter(r -> r.getKeywords().contains(KEYWORD)).count();
    }

    @Test
    @DisplayName("should restore the previous categories and delete the category rule")
    @WithCustomMockUser(id = USER_ID)
    void shouldUndoACategoryRule() throws Exception {
        // arrange
        JsonNode applied = applyCategory(List.of(94011L, 94012L));
        assertThat(categoryOf(94011L)).isEqualTo(FAST_FOOD);

        // act
        undo(applied, null)
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath(RESTORED).value(2))
                .andExpect(jsonPath("$.skippedTransactionIds.length()").value(0))
                .andExpect(jsonPath(CATEGORY_RULE_DELETED).value(true))
                .andExpect(jsonPath(MERCHANT_RULE_DELETED).value(false));

        assertThat(categoryOf(94011L)).isNull();
        assertThat(categoryOf(94012L)).isEqualTo(DINING);
        assertThat(categoryRules()).isZero();
    }

    @Test
    @DisplayName("should leave a row that changed since the apply alone, and report it")
    @WithCustomMockUser(id = USER_ID)
    void shouldSkipARowChangedSince() throws Exception {
        // arrange -- after the apply, the owner re-categorizes 94012 by hand
        JsonNode applied = applyCategory(List.of(94011L, 94012L));
        jdbcClient.sql("update transactions set category_id = 9401 where id = 94012").update();

        // act
        undo(applied, null)
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath(RESTORED).value(1))
                .andExpect(jsonPath("$.skippedTransactionIds[0]").value(94012))
                .andExpect(jsonPath(CATEGORY_RULE_DELETED).value(true));

        assertThat(categoryOf(94011L)).isNull();
        assertThat(categoryOf(94012L)).isEqualTo(DINING);
    }

    @Test
    @DisplayName("should still restore the rows when the rule was already deleted, and say so")
    @WithCustomMockUser(id = USER_ID)
    void shouldRestoreWhenTheRuleIsAlreadyGone() throws Exception {
        // arrange
        JsonNode applied = applyCategory(List.of(94011L));
        categoryRuleRepository.deleteById(applied.get("ruleId").asLong(), USER_ID);

        // act
        undo(applied, null)
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath(RESTORED).value(1))
                .andExpect(jsonPath(CATEGORY_RULE_DELETED).value(false));

        assertThat(categoryOf(94011L)).isNull();
    }

    @Test
    @DisplayName("should restore previous merchants and delete the merchant rule")
    @WithCustomMockUser(id = USER_ID)
    void shouldUndoAMerchantRule() throws Exception {
        // arrange
        JsonNode applied = applyMerchant(List.of(94011L, 94012L));
        assertThat(merchantOf(94012L)).isEqualTo(TACO_BELL);

        // act
        undo(null, applied)
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath(RESTORED).value(2))
                .andExpect(jsonPath(MERCHANT_RULE_DELETED).value(true))
                .andExpect(jsonPath(CATEGORY_RULE_DELETED).value(false));

        assertThat(merchantOf(94011L)).isNull();
        assertThat(merchantOf(94012L)).isEqualTo(OLD_MERCHANT);
        assertThat(merchantRules()).isZero();
    }

    @Test
    @DisplayName("should undo a category rule and a merchant rule together, counting each row once")
    @WithCustomMockUser(id = USER_ID)
    void shouldUndoBothKinds() throws Exception {
        // arrange
        JsonNode category = applyCategory(List.of(94011L, 94012L));
        JsonNode merchant = applyMerchant(List.of(94011L, 94012L));

        // act
        undo(category, merchant)
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath(RESTORED).value(2))
                .andExpect(jsonPath(CATEGORY_RULE_DELETED).value(true))
                .andExpect(jsonPath(MERCHANT_RULE_DELETED).value(true));

        assertThat(categoryOf(94011L)).isNull();
        assertThat(merchantOf(94011L)).isNull();
        assertThat(categoryOf(94012L)).isEqualTo(DINING);
        assertThat(merchantOf(94012L)).isEqualTo(OLD_MERCHANT);
    }

    @Test
    @DisplayName("should leave another user's transactions alone")
    @WithCustomMockUser(id = USER_ID)
    void shouldIgnoreAnotherUsersTransactions() throws Exception {
        // arrange -- a hand-built result naming a transaction that belongs to user 941
        ObjectNode result = objectMapper.createObjectNode();
        result.put("ruleId", 1L).put("categoryId", FAST_FOOD);
        result.putArray("changes").addObject().put("transactionId", 94014L).putNull("previousCategoryId");

        // act
        undo(result, null)
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath(RESTORED).value(0))
                .andExpect(jsonPath("$.skippedTransactionIds[0]").value(94014));

        assertThat(categoryOf(94014L)).isEqualTo(FAST_FOOD);
    }

    @Test
    @DisplayName("should not delete another user's rule")
    @WithCustomMockUser(id = USER_ID)
    void shouldNotDeleteAnotherUsersRule() throws Exception {
        // arrange -- user 941 has a merchant rule; user 940 names its id in an undo
        Long theirRule = merchantRuleService.createRule(941L,
                MerchantRuleCreateRequest.builder().merchantId(THEIR_MERCHANT).keywords(List.of(KEYWORD)).build());
        ObjectNode result = objectMapper.createObjectNode();
        result.put("ruleId", theirRule).put("merchantId", THEIR_MERCHANT);
        result.putArray("changes");

        // act
        undo(null, result)
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath(MERCHANT_RULE_DELETED).value(false));

        assertThat(merchantRuleRepository.findById(theirRule)).isPresent();
    }
}
