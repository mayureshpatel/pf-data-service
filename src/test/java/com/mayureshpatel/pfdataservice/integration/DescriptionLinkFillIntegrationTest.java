package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-980: linking descriptions to a merchant through the real endpoints, security and PostgreSQL
 * fills that merchant in on the user's past transactions that have none, never overwrites a set
 * merchant or touches another user's rows, reports a moved link, and the stateless undo reverses
 * exactly what the link did while leaving a transaction edited since alone.
 */
@AutoConfigureMockMvc
class DescriptionLinkFillIntegrationTest extends BaseIntegrationTest {

    private static final String LINKS_URL = "/api/v1/merchants/{id}/description-links";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcClient jdbcClient;

    private Long merchantOf(String description, long accountId) {
        return jdbcClient.sql("select merchant_id from transactions where description = :d and account_id = :a limit 1")
                .param("d", description).param("a", accountId)
                .query(Long.class).optional().orElse(null);
    }

    private Long linkOf(long userId, String normalized) {
        return jdbcClient.sql("select merchant_id from merchant_description_links where user_id = :u and normalized_description = :n")
                .param("u", userId).param("n", normalized)
                .query(Long.class).optional().orElse(null);
    }

    @Test
    @DisplayName("bulk link should fill blanks only, report a moved link, and undo should skip a row edited since")
    @WithCustomMockUser(id = 9980L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (9980, 'pf980a', 'pf980a@example.com', 'hash', 'system', NOW()), (9981, 'pf980b', 'pf980b@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9980, 9980, 'PF980 A', 'CHECKING', 100.00, 'STANDARD'), (9981, 9981, 'PF980 B', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (9980, 9980, 'PF980 Kroger'), (9981, 9980, 'PF980 Old'), (9982, 9980, 'PF980 Set'), (9983, 9981, 'PF980 Theirs') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchant_description_links (user_id, merchant_id, description, normalized_description, created_at, updated_at) VALUES (9980, 9981, 'PF980 KROGER #2', 'pf980 kroger #2', NOW(), NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO transactions (account_id, merchant_id, amount, date, description, type) VALUES (9980, null, 10.00, NOW(), 'PF980 KROGER #1', 'EXPENSE'), (9980, null, 10.00, NOW(), 'PF980 KROGER #2', 'EXPENSE'), (9980, 9982, 10.00, NOW(), 'PF980 KROGER #1', 'EXPENSE'), (9981, null, 10.00, NOW(), 'PF980 KROGER #1', 'EXPENSE');"
    })
    void bulkLink_fillsBlanksOnly_andUndoSkipsEditedRow() throws Exception {
        // arrange
        String body = "{\"descriptions\":[\"PF980 KROGER #1\",\"pf980  kroger #2\"]}";

        // act
        String response = mockMvc.perform(post(LINKS_URL + "/bulk", 9980L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedCount").value(2))
                .andExpect(jsonPath("$.movedCount").value(1))
                .andExpect(jsonPath("$.transactionsFilled").value(2))
                .andReturn().getResponse().getContentAsString();

        // assert & verify -- blanks filled, set merchant kept, other user's row untouched
        assertThat(merchantOf("PF980 KROGER #2", 9980L)).isEqualTo(9980L);
        assertThat(jdbcClient.sql("select merchant_id from transactions where account_id = 9980 and description = 'PF980 KROGER #1' order by merchant_id")
                .query(Long.class).list()).containsExactly(9980L, 9982L);
        assertThat(merchantOf("PF980 KROGER #1", 9981L)).isNull();
        assertThat(linkOf(9980L, "pf980 kroger #2")).isEqualTo(9980L);

        // arrange -- the user re-assigns one filled row by hand before undoing
        jdbcClient.sql("update transactions set merchant_id = 9982 where account_id = 9980 and description = 'PF980 KROGER #2'").update();
        JsonNode result = objectMapper.readTree(response);

        // act
        mockMvc.perform(post(LINKS_URL + "/undo", 9980L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(result.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linksRestored").value(2))
                .andExpect(jsonPath("$.linksSkipped").value(0))
                .andExpect(jsonPath("$.transactionsRestored").value(1))
                .andExpect(jsonPath("$.skippedTransactionIds.length()").value(1));

        // assert & verify -- new link gone, moved link back, edited row kept
        assertThat(linkOf(9980L, "pf980 kroger #1")).isNull();
        assertThat(linkOf(9980L, "pf980 kroger #2")).isEqualTo(9981L);
        assertThat(jdbcClient.sql("select merchant_id from transactions where account_id = 9980 and description = 'PF980 KROGER #1' order by merchant_id nulls first")
                .query(Long.class).list()).containsExactly(null, 9982L);
        assertThat(merchantOf("PF980 KROGER #2", 9980L)).isEqualTo(9982L);
    }

    @Test
    @DisplayName("single link should fill blanks too, and undo for another user's merchant is forbidden")
    @WithCustomMockUser(id = 9984L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (9984, 'pf980c', 'pf980c@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9984, 9984, 'PF980 C', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (9984, 9984, 'PF980 Fuel') ON CONFLICT DO NOTHING;",
            "INSERT INTO transactions (account_id, amount, date, description, type) VALUES (9984, 10.00, NOW(), 'PF980 FUEL #9', 'EXPENSE');"
    })
    void singleLink_fillsBlank_andForeignUndoIsForbidden() throws Exception {
        // act & assert & verify
        mockMvc.perform(post(LINKS_URL, 9984L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"PF980 FUEL #9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionsFilled").value(1));
        assertThat(merchantOf("PF980 FUEL #9", 9984L)).isEqualTo(9984L);

        mockMvc.perform(post(LINKS_URL + "/undo", 9980L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantId\":9980,\"linkedCount\":0,\"movedCount\":0,\"transactionsFilled\":0,\"linkChanges\":[],\"filledTransactionIds\":[]}"))
                .andExpect(status().isForbidden());
    }
}
