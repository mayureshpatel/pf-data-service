package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchGroup;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchResultDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchSkippedDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Unit tests for {@code POST /api/v1/categories/batch} on {@link CategoryController}: the 200 result body, 400 validation and the unauthenticated 403. */
@DisplayName("CategoryController batch endpoint Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class CategoryBatchControllerTest extends BaseControllerTest {

    private static final String FOOD = "Food";
    private static final String URL = "/api/v1/categories/batch";

    private String body(CategoryBatchRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private CategoryBatchGroup group(String name, List<String> subs) {
        return new CategoryBatchGroup(name, "EXPENSE", "#112233", "home", subs);
    }

    @Test
    @DisplayName("should return 200 with the batch result")
    void returnsResult() throws Exception {
        // arrange
        CategoryBatchResultDto result = new CategoryBatchResultDto(3, 1, 2,
                List.of(new CategoryBatchSkippedDto("Food", null, "Already exists")));
        when(categoryBatchService.createBatch(eq(USER_ID), any(CategoryBatchRequest.class))).thenReturn(result);
        CategoryBatchRequest request = new CategoryBatchRequest(List.of(group("Housing", List.of("Rent", "Repairs"))));

        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.createdCount").value(3))
                .andExpect(jsonPath("$.createdParents").value(1))
                .andExpect(jsonPath("$.createdSubcategories").value(2))
                .andExpect(jsonPath("$.skipped[0].name").value("Food"))
                .andExpect(jsonPath("$.skipped[0].parentName").doesNotExist())
                .andExpect(jsonPath("$.skipped[0].reason").value("Already exists"));
        verify(categoryBatchService).createBatch(eq(USER_ID), any(CategoryBatchRequest.class));
    }

    @Test
    @DisplayName("should return 400 for empty groups")
    void rejectsEmptyGroups() throws Exception {
        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(new CategoryBatchRequest(List.of()))))
                .andExpect(status().isBadRequest());
        verify(categoryBatchService, never()).createBatch(any(), any());
    }

    @Test
    @DisplayName("should return 400 for a blank parent name")
    void rejectsBlankParent() throws Exception {
        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(new CategoryBatchRequest(List.of(group(" ", List.of("Rent")))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 naming an over-long subcategory")
    void rejectsOverLongName() throws Exception {
        // arrange
        String longName = "y".repeat(51);

        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(new CategoryBatchRequest(List.of(group(FOOD, List.of(longName)))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].message").value(org.hamcrest.Matchers.containsString(longName)));
    }

    @Test
    @DisplayName("should return 400 for more than 300 names in total")
    void rejectsTooManyNames() throws Exception {
        // arrange
        List<CategoryBatchGroup> groups = new ArrayList<>();
        IntStream.range(0, 4).forEach(g -> groups.add(group("G" + g,
                IntStream.range(0, 99).mapToObj(i -> "S" + g + "-" + i).toList())));

        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(new CategoryBatchRequest(groups))))
                .andExpect(status().isBadRequest());
        verify(categoryBatchService, never()).createBatch(any(), any());
    }

    @Test
    @DisplayName("should return 400 for a color longer than the single create allows")
    void rejectsLongColor() throws Exception {
        // arrange
        CategoryBatchGroup bad = new CategoryBatchGroup(FOOD, "EXPENSE", "c".repeat(21), null, List.of());

        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(new CategoryBatchRequest(List.of(bad)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("should return 403 when unauthenticated")
    void rejectsAnonymous() throws Exception {
        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(new CategoryBatchRequest(List.of(group(FOOD, List.of()))))))
                .andExpect(status().isForbidden());
        verify(categoryBatchService, never()).createBatch(any(), any());
    }
}
