package com.mayureshpatel.pfdataservice.integration;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.service.MerchantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-893: pins two behaviours of {@code GET /api/v1/transactions/descriptions} against the real
 * security filter chain and a real PostgreSQL: the endpoint is never public (the project has no
 * custom authentication entry point, so spring security answers a missing credential with 403, not
 * 401), and a description link recorded for either spelling of a description -- as typed on the
 * transaction or already folded -- marks the grouped row as linked, while another user's link on
 * the same text is never used. Links are recorded through {@link MerchantService} so the stored
 * key is exactly what production writes.
 */
@AutoConfigureMockMvc
class TransactionDescriptionsIntegrationTest extends BaseIntegrationTest {

    private static final String URL = "/api/v1/transactions/descriptions";
    private static final String PARAM_SEARCH = "search";
    private static final String SEARCH = "pf893";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantService merchantService;

    @Test
    @DisplayName("GET /descriptions should reject a request with no credentials (403, since no entry point makes it 401)")
    void getDescriptions_withoutCredentials_isRejected() throws Exception {
        // arrange -- no authenticated principal and no bearer token

        // act & assert & verify
        mockMvc.perform(get(URL).param(PARAM_SEARCH, SEARCH))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /descriptions should mark the row linked when the link was recorded with the raw typed spelling")
    @WithCustomMockUser(id = 9931L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (9931, 'pf893a', 'pf893a@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9931, 9931, 'PF893 A', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (9931, 9931, 'PF893 Kroger A') ON CONFLICT DO NOTHING;",
            "INSERT INTO transactions (account_id, amount, date, description, type) VALUES (9931, 10.00, NOW(), 'PF893 KROGER  #12 ', 'EXPENSE'), (9931, 10.00, NOW(), 'pf893 kroger #12', 'EXPENSE'), (9931, 10.00, NOW(), 'PF893 Other', 'EXPENSE');"
    })
    void getDescriptions_linkRecordedForRawSpelling_marksGroupedRowLinked() throws Exception {
        // arrange -- the link is recorded with the odd case and spacing exactly as typed
        merchantService.recordDescriptionLink(9931L, 9931L, "PF893 KROGER  #12 ");

        // act & assert & verify
        mockMvc.perform(get(URL).param(PARAM_SEARCH, SEARCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].transactionCount").value(2))
                .andExpect(jsonPath("$.content[0].merchantId").value(9931))
                .andExpect(jsonPath("$.content[0].merchantName").value("PF893 Kroger A"))
                .andExpect(jsonPath("$.content[1].description").value("PF893 Other"))
                .andExpect(jsonPath("$.content[1].merchantId").doesNotExist());
    }

    @Test
    @DisplayName("GET /descriptions should mark the row linked when the link was recorded with the cleaned (folded) spelling")
    @WithCustomMockUser(id = 9932L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (9932, 'pf893b', 'pf893b@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9932, 9932, 'PF893 B', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (9932, 9932, 'PF893 Kroger B') ON CONFLICT DO NOTHING;",
            "INSERT INTO transactions (account_id, amount, date, description, type) VALUES (9932, 10.00, NOW(), 'PF893 KROGER  #12 ', 'EXPENSE'), (9932, 10.00, NOW(), 'Pf893 Kroger #12', 'EXPENSE');"
    })
    void getDescriptions_linkRecordedForFoldedSpelling_marksGroupedRowLinked() throws Exception {
        // arrange -- the link is recorded with the cleaned form the grouping uses
        merchantService.recordDescriptionLink(9932L, 9932L, "pf893 kroger #12");

        // act & assert & verify
        mockMvc.perform(get(URL).param(PARAM_SEARCH, SEARCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transactionCount").value(2))
                .andExpect(jsonPath("$.content[0].merchantId").value(9932))
                .andExpect(jsonPath("$.content[0].merchantName").value("PF893 Kroger B"));
    }

    @Test
    @DisplayName("GET /descriptions should not use another user's link on the same text")
    @WithCustomMockUser(id = 9933L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (9933, 'pf893c', 'pf893c@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (9934, 'pf893d', 'pf893d@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9933, 9933, 'PF893 C', 'CHECKING', 100.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (9934, 9934, 'PF893 Kroger D') ON CONFLICT DO NOTHING;",
            "INSERT INTO transactions (account_id, amount, date, description, type) VALUES (9933, 10.00, NOW(), 'PF893 KROGER  #12 ', 'EXPENSE');"
    })
    void getDescriptions_otherUsersLink_isNotUsed() throws Exception {
        // arrange -- user 9934 links the same text to their own merchant; user 9933 has no link
        merchantService.recordDescriptionLink(9934L, 9934L, "PF893 KROGER  #12 ");

        // act & assert & verify
        mockMvc.perform(get(URL).param(PARAM_SEARCH, SEARCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].merchantId").doesNotExist())
                .andExpect(jsonPath("$.content[0].merchantName").doesNotExist());
    }
}
