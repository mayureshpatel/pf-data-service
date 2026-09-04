package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagDto;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link TagController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("TagController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class TagControllerTest extends BaseControllerTest {

    private static final Long TAG_ID = 1L;
    private static final Long TRANSACTION_ID = 500L;

    @Nested
    @DisplayName("getTags")
    class GetTagsTests {
        @Test
        @DisplayName("GET should return list of tags for authenticated user")
        void getTags_shouldReturnList() throws Exception {
            // Arrange
            TagDto tagDto = new TagDto(TAG_ID, USER_ID, "Travel", "#123456");
            when(tagService.getTags(USER_ID)).thenReturn(List.of(tagDto));

            // Act & Assert
            mockMvc.perform(get("/api/v1/tags"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(TAG_ID))
                    .andExpect(jsonPath("$[0].name").value("Travel"));

            verify(tagService).getTags(USER_ID);
        }

        @Test
        @DisplayName("GET should return empty list when no tags exist")
        void getTags_shouldReturnEmptyList() throws Exception {
            when(tagService.getTags(USER_ID)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/tags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("createTag")
    class CreateTagTests {
        @Test
        @DisplayName("POST should create a new tag and return the generated id")
        void createTag_shouldReturnGeneratedId() throws Exception {
            // Arrange
            TagCreateRequest request = TagCreateRequest.builder()
                    .userId(USER_ID)
                    .name("Travel")
                    .color("#123456")
                    .build();

            when(tagService.createTag(eq(USER_ID), any(TagCreateRequest.class))).thenReturn(42L);

            // Act & Assert
            mockMvc.perform(post("/api/v1/tags")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("42"));

            verify(tagService).createTag(eq(USER_ID), any(TagCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 Bad Request when validation fails")
        void createTag_shouldReturn400OnInvalidInput() throws Exception {
            // Arrange -- missing name
            TagCreateRequest request = TagCreateRequest.builder().userId(USER_ID).build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/tags")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field", org.hamcrest.Matchers.hasItem("name")));
        }
    }

    @Nested
    @DisplayName("updateTag")
    class UpdateTagTests {
        @Test
        @DisplayName("PUT should update an existing tag and return rows affected")
        void updateTag_shouldReturnRowsAffected() throws Exception {
            // Arrange
            TagUpdateRequest request = TagUpdateRequest.builder().id(TAG_ID).name("Updated").color("#FFFFFF").build();
            when(tagService.updateTag(eq(USER_ID), any(TagUpdateRequest.class))).thenReturn(1);

            // Act & Assert
            mockMvc.perform(put("/api/v1/tags")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(tagService).updateTag(eq(USER_ID), any(TagUpdateRequest.class));
        }

        @Test
        @DisplayName("PUT should return 400 Bad Request when validation fails")
        void updateTag_shouldReturn400OnInvalidInput() throws Exception {
            // Arrange -- missing id and name
            TagUpdateRequest request = TagUpdateRequest.builder().build();

            mockMvc.perform(put("/api/v1/tags")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field", org.hamcrest.Matchers.containsInAnyOrder("id", "name")));
        }
    }

    @Nested
    @DisplayName("deleteTag")
    class DeleteTagTests {
        @Test
        @DisplayName("DELETE should remove tag and return 204 No Content")
        void deleteTag_shouldReturnNoContent() throws Exception {
            mockMvc.perform(delete("/api/v1/tags/{id}", TAG_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(tagService).deleteTag(USER_ID, TAG_ID);
        }

        @Test
        @DisplayName("DELETE should return 404 Not Found when tag does not exist")
        void deleteTag_shouldReturn404() throws Exception {
            org.mockito.Mockito.doThrow(new ResourceNotFoundException("Tag not found"))
                    .when(tagService).deleteTag(USER_ID, TAG_ID);

            mockMvc.perform(delete("/api/v1/tags/{id}", TAG_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("assignToTransaction")
    class AssignToTransactionTests {
        @Test
        @DisplayName("POST should assign a tag to a transaction and return 204")
        void assignToTransaction_shouldReturnNoContent() throws Exception {
            mockMvc.perform(post("/api/v1/tags/{tagId}/transactions/{transactionId}", TAG_ID, TRANSACTION_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(tagService).assignToTransaction(USER_ID, TAG_ID, TRANSACTION_ID);
        }

        @Test
        @DisplayName("POST should return 404 Not Found when the tag or transaction does not exist")
        void assignToTransaction_shouldReturn404() throws Exception {
            org.mockito.Mockito.doThrow(new ResourceNotFoundException("Transaction not found"))
                    .when(tagService).assignToTransaction(USER_ID, TAG_ID, TRANSACTION_ID);

            mockMvc.perform(post("/api/v1/tags/{tagId}/transactions/{transactionId}", TAG_ID, TRANSACTION_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("removeFromTransaction")
    class RemoveFromTransactionTests {
        @Test
        @DisplayName("DELETE should remove a tag from a transaction and return 204")
        void removeFromTransaction_shouldReturnNoContent() throws Exception {
            mockMvc.perform(delete("/api/v1/tags/{tagId}/transactions/{transactionId}", TAG_ID, TRANSACTION_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(tagService).removeFromTransaction(USER_ID, TAG_ID, TRANSACTION_ID);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {
        @Test
        @DisplayName("GET should return 500 when service fails unexpectedly")
        void getTags_shouldReturn500() throws Exception {
            when(tagService.getTags(anyLong())).thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/v1/tags"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
