package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-920: creating a category rule and applying it to chosen transactions in one step, against a
 * real Postgres -- the overwrite, the previous values for undo, the skipped transfer, the
 * "only the chosen ids" guarantee, a foreign id failing the whole call, and atomic rollback.
 */
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM category_rules WHERE user_id = 920;",
        "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (920, 'pf920u920', 'pf920u920@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
        "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (921, 'pf920u921', 'pf920u921@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
        "INSERT INTO categories (id, user_id, name, type) VALUES (9201, 920, 'PF920 Dining', 'EXPENSE') ON CONFLICT DO NOTHING;",
        "INSERT INTO categories (id, user_id, name, type) VALUES (9202, 920, 'PF920 Fast Food', 'EXPENSE') ON CONFLICT DO NOTHING;",
        "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9201, 920, 'PF920 Mine', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
        "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9202, 921, 'PF920 Theirs', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
        "INSERT INTO transactions (id, account_id, category_id, amount, date, description, type) VALUES (92011, 9201, NULL, 10.00, NOW(), 'PF920 TACO BELL 1', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = NULL, type = 'EXPENSE', deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, category_id, amount, date, description, type) VALUES (92012, 9201, 9201, 10.00, NOW(), 'PF920 TACO BELL 2', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = 9201, type = 'EXPENSE', deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, category_id, amount, date, description, type) VALUES (92013, 9201, NULL, 10.00, NOW(), 'PF920 TACO BELL 3', 'TRANSFER_OUT') ON CONFLICT (id) DO UPDATE SET category_id = NULL, type = 'TRANSFER_OUT', deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, category_id, amount, date, description, type) VALUES (92014, 9201, NULL, 10.00, NOW(), 'PF920 TACO BELL 4', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = NULL, type = 'EXPENSE', deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, category_id, amount, date, description, type) VALUES (92015, 9202, NULL, 10.00, NOW(), 'PF920 TACO BELL 5', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET category_id = NULL, type = 'EXPENSE', deleted_at = NULL;"
})
class CategoryRuleCreateAndApplyIntegrationTest extends BaseIntegrationTest {

    private static final long USER_ID = 920L;
    private static final long DINING = 9201L;
    private static final long FAST_FOOD = 9202L;
    private static final String URL = "/api/v1/category-rules/create-and-apply";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcClient jdbcClient;
    @Autowired
    private CategoryRuleRepository categoryRuleRepository;
    @MockitoSpyBean
    private TransactionRepository transactionRepository;

    private String body(List<Long> ids) throws Exception {
        CategoryRuleCreateRequest rule = CategoryRuleCreateRequest.builder()
                .userId(USER_ID).categoryId(FAST_FOOD).keywords(List.of("pf920 taco bell")).build();
        return objectMapper.writeValueAsString(new CategoryRuleApplyRequest(rule, ids));
    }

    private Long categoryOf(long transactionId) {
        return jdbcClient.sql("select category_id from transactions where id = :id")
                .param("id", transactionId).query(Long.class).optional().orElse(null);
    }

    private int ruleCount() {
        return categoryRuleRepository.findByUserId(USER_ID).stream()
                .filter(r -> r.getKeywords().contains("pf920 taco bell")).toList().size();
    }

    @Test
    @DisplayName("should overwrite the chosen rows, report previous values, skip a transfer and touch nothing else")
    @WithCustomMockUser(id = USER_ID)
    void shouldApplyToChosenRowsOnly() throws Exception {
        // act
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of(92011L, 92012L, 92013L))))
                // assert & verify -- the response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").isNumber())
                .andExpect(jsonPath("$.changes.length()").value(2))
                .andExpect(jsonPath("$.changes[?(@.transactionId==92011)].previousCategoryId").value(
                        org.hamcrest.Matchers.contains((Object) null)))
                .andExpect(jsonPath("$.changes[?(@.transactionId==92012)].previousCategoryId").value(
                        org.hamcrest.Matchers.contains((int) DINING)))
                .andExpect(jsonPath("$.skippedTransactionIds[0]").value(92013));

        // assert & verify -- the database
        assertThat(categoryOf(92011L)).isEqualTo(FAST_FOOD);
        assertThat(categoryOf(92012L)).isEqualTo(FAST_FOOD);
        assertThat(categoryOf(92013L)).isNull();
        assertThat(categoryOf(92014L)).isNull();
        assertThat(ruleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should fail the whole call, changing nothing and saving no rule, when an id is not the user's")
    @WithCustomMockUser(id = USER_ID)
    void shouldRejectAForeignIdWithoutChangingAnything() throws Exception {
        // act
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of(92011L, 92015L))))
                // assert & verify
                .andExpect(status().isForbidden());

        assertThat(categoryOf(92011L)).isNull();
        assertThat(categoryOf(92015L)).isNull();
        assertThat(ruleCount()).isZero();
    }

    @Test
    @DisplayName("should roll the rule back when the update fails after the rule was saved")
    @WithCustomMockUser(id = USER_ID)
    void shouldRollBackTheRuleWhenTheUpdateFails() throws Exception {
        // arrange
        doThrow(new RuntimeException("boom")).when(transactionRepository)
                .overwriteCategory(anyLong(), anyLong(), any());

        // act
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of(92011L))))
                // assert & verify
                .andExpect(status().is5xxServerError());

        assertThat(categoryOf(92011L)).isNull();
        assertThat(ruleCount()).isZero();
    }
}
