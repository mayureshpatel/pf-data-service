package com.mayureshpatel.pfdataservice.integration;

import com.mayureshpatel.pfdataservice.config.TestContainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-212: with the {@code prod} profile active, Swagger UI and the raw OpenAPI spec must not be
 * reachable at all -- disabled via springdoc's own {@code enabled=false} properties in
 * {@code application-prod.yml}, not merely access-controlled. Boots the real app context with
 * {@code prod} actually active (not just asserting the YAML file's contents) since that's the one
 * thing a config-file read alone can't prove -- whether the property genuinely takes effect.
 */
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("prod")
@Import(TestContainersConfig.class)
// prod profile also activates cors.allowed-origins: ${CORS_ALLOWED_ORIGINS} (unrelated to this
// test) -- supplied here only so the context can boot at all, not because this test cares about it
@TestPropertySource(properties = "CORS_ALLOWED_ORIGINS=http://localhost:4200")
class SwaggerProdProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /v3/api-docs should not be reachable when the prod profile is active (PF-212)")
    void apiDocsShouldNotBeReachableInProd() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html should not be reachable when the prod profile is active (PF-212)")
    void swaggerUiShouldNotBeReachableInProd() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound());
    }
}
