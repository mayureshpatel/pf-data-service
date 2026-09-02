package com.mayureshpatel.pfdataservice.integration;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-212's fix (disabling springdoc via {@code application-prod.yml}) only takes effect for the
 * {@code prod} profile. This confirms the other half of that ticket's acceptance criteria wasn't
 * broken in the process -- local development convenience is preserved outside {@code prod}.
 */
@AutoConfigureMockMvc
class SwaggerNonProdProfileIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /v3/api-docs should remain reachable outside the prod profile (PF-212)")
    void apiDocsShouldRemainReachableOutsideProd() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
