package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-912: guards the generated OpenAPI spec against ambiguous operation ids.
 * <p>
 * When two controllers declare a method with the same name, springdoc silently numbers the later
 * one ({@code getRules_1}), so adding a method in one controller renames another controller's
 * operations in pf-ui's generated types. This test fails when any operation id repeats or carries
 * that numeric suffix; the fix is an explicit {@code @Operation(operationId = ...)} on the newer
 * method.
 */
@AutoConfigureMockMvc
class OpenApiOperationIdIntegrationTest extends BaseIntegrationTest {

    private static final Pattern NUMBERED_SUFFIX = Pattern.compile(".*_\\d+$");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Reads every operation id from the live spec and asserts none repeats or is springdoc-numbered.
     *
     * @throws Exception if the spec cannot be fetched or parsed
     */
    @Test
    @DisplayName("PF-912: every OpenAPI operationId is unique and has no springdoc _<n> suffix")
    void operationIdsAreUniqueAndUnnumbered() throws Exception {
        // arrange
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode paths = objectMapper.readTree(body).path("paths");

        // act
        List<String> ids = new ArrayList<>();
        paths.forEach(pathItem -> pathItem.forEach(operation -> {
            if (operation.hasNonNull("operationId")) {
                ids.add(operation.get("operationId").asText());
            }
        }));
        Set<String> seen = new HashSet<>();
        List<String> duplicates = ids.stream().filter(id -> !seen.add(id)).sorted().toList();
        List<String> numbered = ids.stream().filter(id -> NUMBERED_SUFFIX.matcher(id).matches()).sorted().toList();

        // assert & verify
        assertThat(ids).isNotEmpty();
        assertThat(duplicates).as("duplicated operation ids").isEmpty();
        assertThat(numbered).as("springdoc-numbered operation ids").isEmpty();
    }
}
