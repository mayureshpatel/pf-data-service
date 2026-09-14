package com.mayureshpatel.pfdataservice.integration;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-316: exports the live OpenAPI spec to {@code target/openapi.json} at build time, so pf-ui's
 * {@code npm run generate:api} has a stable source to generate a TypeScript client from.
 * <p>
 * Deliberately built on {@link BaseIntegrationTest}'s Testcontainers-backed Postgres rather than a
 * {@code spring-boot-maven-plugin} start/stop of the real application: the default
 * {@code application.yml} datasource points at the live Supabase database, and this project has
 * already had one real incident from a build-time tool touching that database directly (the
 * Flyway-Maven-plugin/Supabase schema_history corruption). Every other integration test in this
 * suite already safely avoids that via {@code @Import(TestContainersConfig.class)}'s
 * {@code @ServiceConnection}-backed container; this test reuses the exact same isolation rather
 * than introducing a second, less-proven mechanism.
 */
@AutoConfigureMockMvc
class OpenApiSpecExportIntegrationTest extends BaseIntegrationTest {

    private static final Path OUTPUT_PATH = Path.of("target", "openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("PF-316: exports the live OpenAPI spec to target/openapi.json for TS client generation")
    void exportsOpenApiSpecToFile() throws Exception {
        // act
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        // assert & verify
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"openapi\"");

        Files.createDirectories(OUTPUT_PATH.getParent());
        Files.writeString(OUTPUT_PATH, body, StandardCharsets.UTF_8);
    }
}
