package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchGroup;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchRequest;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.service.CategoryBatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-897: the batch category endpoint against a real Postgres -- the real flow (parents, inherited
 * properties, idempotent second call, per-user scope) and atomic rollback when an insert fails
 * part-way through.
 */
@AutoConfigureMockMvc
class CategoryBatchIntegrationTest extends BaseIntegrationTest {

    private static final String EXPENSE = "EXPENSE";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CategoryBatchService batchService;

    @Test
    @DisplayName("should create the batch, skip everything on a second call, and touch only the caller's categories")
    @WithCustomMockUser(id = 910L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (910, 'pf897u910', 'pf897u910@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (911, 'pf897u911', 'pf897u911@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO categories (user_id, name, type) VALUES (911, 'Housing', 'EXPENSE');"
    })
    void shouldCreateThenSkipEverything() throws Exception {
        // arrange
        String body = objectMapper.writeValueAsString(new CategoryBatchRequest(List.of(
                new CategoryBatchGroup("Housing", EXPENSE, "#112233", "home", List.of("Rent", "", "Repairs")),
                new CategoryBatchGroup("Transfers", "TRANSFER", "#445566", "swap", List.of("Between accounts")))));

        // act
        mockMvc.perform(post("/api/v1/categories/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(5))
                .andExpect(jsonPath("$.createdParents").value(2))
                .andExpect(jsonPath("$.createdSubcategories").value(3));
        mockMvc.perform(post("/api/v1/categories/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.skipped.length()").value(5));

        // assert & verify
        List<Category> mine = categoryRepository.findByUserId(910L);
        assertEquals(5, mine.size());
        Category housing = mine.stream().filter(c -> "Housing".equals(c.getName())).findFirst().orElseThrow();
        Category rent = mine.stream().filter(c -> "Rent".equals(c.getName())).findFirst().orElseThrow();
        Category between = mine.stream().filter(c -> "Between accounts".equals(c.getName())).findFirst().orElseThrow();
        assertEquals(housing.getId(), rent.getParentId());
        assertEquals("#112233", rent.getColor());
        assertEquals("home", rent.getIcon());
        assertEquals(EXPENSE, rent.getType());
        assertEquals("TRANSFER", between.getType());
        List<Category> other = categoryRepository.findByUserId(911L);
        assertEquals(1, other.size());
        assertFalse(other.get(0).isSubCategory());
    }

    @Test
    @DisplayName("should persist nothing when an insert fails part-way through the batch")
    @Sql(statements = "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (912, 'pf897u912', 'pf897u912@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;")
    void shouldRollBackEverythingOnFailure() {
        // arrange -- the second group's color exceeds the column width, so its insert fails after the first group was written
        CategoryBatchRequest request = new CategoryBatchRequest(List.of(
                new CategoryBatchGroup("First", EXPENSE, "#112233", null, List.of("One")),
                new CategoryBatchGroup("Second", EXPENSE, "c".repeat(40), null, List.of("Two"))));

        // act
        assertThrows(RuntimeException.class, () -> batchService.createBatch(912L, request));

        // assert & verify
        assertEquals(0, categoryRepository.findByUserId(912L).size());
    }
}
