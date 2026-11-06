package com.mayureshpatel.pfdataservice.integration;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-876: a category whose budgets were removed (budgets are soft-deleted via {@code deleted_at})
 * must be deletable, while a category with an active budget is still refused with a specific
 * message. Exercises the real controllers, services and repositories against a real Postgres,
 * because the failure is a foreign key from the soft-deleted {@code budgets} row to
 * {@code categories}, which a mocked unit test can't observe.
 */
@AutoConfigureMockMvc
class CategoryDeleteAfterBudgetRemovalIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("DELETE /categories should succeed once the category's budget was removed through DELETE /budgets (PF-876)")
    @WithCustomMockUser(id = 876L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (876, 'pf876user', 'pf876@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO categories (id, user_id, name, color, type) VALUES (8761, 876, 'PF876 Removed Budget', '#000000', 'EXPENSE') ON CONFLICT DO NOTHING;",
            "INSERT INTO budgets (id, user_id, category_id, amount, month, year) VALUES (8761, 876, 8761, 100.00, 1, 2026) ON CONFLICT DO NOTHING;"
    })
    void shouldDeleteCategoryAfterItsBudgetWasRemoved() throws Exception {
        // arrange -- remove the budget through the same endpoint the ui uses (soft delete)
        mockMvc.perform(delete("/api/v1/budgets/8761").with(csrf()))
                .andExpect(status().isNoContent());

        // act & assert & verify
        mockMvc.perform(delete("/api/v1/categories/8761").with(csrf()))
                .andExpect(status().isOk());

        Integer categories = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE id = 8761", Integer.class);
        Integer budgets = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM budgets WHERE category_id = 8761", Integer.class);
        assertEquals(0, categories);
        assertEquals(0, budgets);
    }

    @Test
    @DisplayName("DELETE /categories should still be refused with a specific message while a budget is active (PF-876)")
    @WithCustomMockUser(id = 877L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (877, 'pf876user2', 'pf876b@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO categories (id, user_id, name, color, type) VALUES (8771, 877, 'PF876 Active Budget', '#000000', 'EXPENSE') ON CONFLICT DO NOTHING;",
            "INSERT INTO budgets (id, user_id, category_id, amount, month, year) VALUES (8771, 877, 8771, 100.00, 1, 2026) ON CONFLICT DO NOTHING;"
    })
    void shouldRefuseCategoryWithActiveBudget() throws Exception {
        // act & assert & verify
        mockMvc.perform(delete("/api/v1/categories/8771").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Cannot delete category with an associated budget. Please delete the budget first."));

        Integer categories = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE id = 8771", Integer.class);
        assertEquals(1, categories);
    }
}
