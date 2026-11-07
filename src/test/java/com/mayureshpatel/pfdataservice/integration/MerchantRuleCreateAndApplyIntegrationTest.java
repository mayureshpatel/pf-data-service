package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleCreateRequest;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
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
 * PF-921: creating a merchant rule and applying it to chosen transactions in one step, against a
 * real Postgres -- the overwrite, the previous values for undo, the "only the chosen ids"
 * guarantee, a foreign transaction or merchant failing the whole call, and atomic rollback.
 */
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM merchant_rules WHERE user_id = 930;",
        "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (930, 'pf921u930', 'pf921u930@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
        "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (931, 'pf921u931', 'pf921u931@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
        "INSERT INTO merchants (id, user_id, name) VALUES (9301, 930, 'PF921 Old Merchant') ON CONFLICT DO NOTHING;",
        "INSERT INTO merchants (id, user_id, name) VALUES (9302, 930, 'PF921 Taco Bell') ON CONFLICT DO NOTHING;",
        "INSERT INTO merchants (id, user_id, name) VALUES (9303, 931, 'PF921 Theirs') ON CONFLICT DO NOTHING;",
        "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9301, 930, 'PF921 Mine', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
        "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9302, 931, 'PF921 Theirs', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
        "INSERT INTO transactions (id, account_id, merchant_id, amount, date, description, type) VALUES (93011, 9301, NULL, 10.00, NOW(), 'PF921 TACO BELL 1', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET merchant_id = NULL, deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, merchant_id, amount, date, description, type) VALUES (93012, 9301, 9301, 10.00, NOW(), 'PF921 TACO BELL 2', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET merchant_id = 9301, deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, merchant_id, amount, date, description, type) VALUES (93013, 9301, NULL, 10.00, NOW(), 'PF921 TACO BELL 3', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET merchant_id = NULL, deleted_at = NULL;",
        "INSERT INTO transactions (id, account_id, merchant_id, amount, date, description, type) VALUES (93014, 9302, NULL, 10.00, NOW(), 'PF921 TACO BELL 4', 'EXPENSE') ON CONFLICT (id) DO UPDATE SET merchant_id = NULL, deleted_at = NULL;"
})
class MerchantRuleCreateAndApplyIntegrationTest extends BaseIntegrationTest {

    private static final long USER_ID = 930L;
    private static final long OLD_MERCHANT = 9301L;
    private static final long TACO_BELL = 9302L;
    private static final long THEIR_MERCHANT = 9303L;
    private static final String URL = "/api/v1/merchant-rules/create-and-apply";
    private static final String KEYWORD = "pf921 taco bell";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcClient jdbcClient;
    @Autowired
    private MerchantRuleRepository merchantRuleRepository;
    @MockitoSpyBean
    private TransactionRepository transactionRepository;

    private String body(long merchantId, List<Long> ids) throws Exception {
        MerchantRuleCreateRequest rule = MerchantRuleCreateRequest.builder()
                .merchantId(merchantId).keywords(List.of(KEYWORD)).build();
        return objectMapper.writeValueAsString(new MerchantRuleApplyRequest(rule, ids));
    }

    private Long merchantOf(long transactionId) {
        return jdbcClient.sql("select merchant_id from transactions where id = :id")
                .param("id", transactionId).query(Long.class).optional().orElse(null);
    }

    private int ruleCount() {
        return merchantRuleRepository.findByUserId(USER_ID).stream()
                .filter(r -> r.getKeywords().contains(KEYWORD)).toList().size();
    }

    @Test
    @DisplayName("should overwrite the chosen rows, report previous values and touch nothing else")
    @WithCustomMockUser(id = USER_ID)
    void shouldApplyToChosenRowsOnly() throws Exception {
        // act
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(TACO_BELL, List.of(93011L, 93012L))))
                // assert & verify -- the response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").isNumber())
                .andExpect(jsonPath("$.changes.length()").value(2))
                .andExpect(jsonPath("$.changes[?(@.transactionId==93011)].previousMerchantId").value(
                        org.hamcrest.Matchers.contains((Object) null)))
                .andExpect(jsonPath("$.changes[?(@.transactionId==93012)].previousMerchantId").value(
                        org.hamcrest.Matchers.contains((int) OLD_MERCHANT)));

        // assert & verify -- the database
        assertThat(merchantOf(93011L)).isEqualTo(TACO_BELL);
        assertThat(merchantOf(93012L)).isEqualTo(TACO_BELL);
        assertThat(merchantOf(93013L)).isNull();
        assertThat(ruleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should fail the whole call, changing nothing and saving no rule, when a transaction is not the user's")
    @WithCustomMockUser(id = USER_ID)
    void shouldRejectAForeignTransactionWithoutChangingAnything() throws Exception {
        // act
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(TACO_BELL, List.of(93011L, 93014L))))
                // assert & verify
                .andExpect(status().isForbidden());

        assertThat(merchantOf(93011L)).isNull();
        assertThat(merchantOf(93014L)).isNull();
        assertThat(ruleCount()).isZero();
    }

    @Test
    @DisplayName("should not accept another user's merchant, changing nothing and saving no rule")
    @WithCustomMockUser(id = USER_ID)
    void shouldRejectAForeignMerchantWithoutChangingAnything() throws Exception {
        // act
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(THEIR_MERCHANT, List.of(93011L))))
                // assert & verify
                .andExpect(status().is4xxClientError());

        assertThat(merchantOf(93011L)).isNull();
        assertThat(ruleCount()).isZero();
    }

    @Test
    @DisplayName("should roll the rule back when the update fails after the rule was saved")
    @WithCustomMockUser(id = USER_ID)
    void shouldRollBackTheRuleWhenTheUpdateFails() throws Exception {
        // arrange
        doThrow(new RuntimeException("boom")).when(transactionRepository)
                .overwriteMerchant(anyLong(), anyLong(), any());

        // act
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(TACO_BELL, List.of(93011L))))
                // assert & verify
                .andExpect(status().is5xxServerError());

        assertThat(merchantOf(93011L)).isNull();
        assertThat(ruleCount()).isZero();
    }
}
