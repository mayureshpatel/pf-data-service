package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantMergeRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MerchantController Unit Tests")
@WithCustomMockUser()
class MerchantControllerTest extends BaseControllerTest {

    private static final Long MERCHANT_ID = 101L;

    @Nested
    @DisplayName("getMerchants")
    class GetMerchantsTests {

        @Test
        @DisplayName("GET should return the authenticated user's merchants")
        void getMerchants_shouldReturnList() throws Exception {
            // arrange
            MerchantDto dto = MerchantDto.builder().id(MERCHANT_ID).userId(USER_ID).originalName("STARBUCKS #1").cleanName("Starbucks").build();
            when(merchantService.getAllMerchants(USER_ID)).thenReturn(List.of(dto));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(MERCHANT_ID))
                    .andExpect(jsonPath("$[0].cleanName").value("Starbucks"));

            verify(merchantService).getAllMerchants(USER_ID);
        }
    }

    @Nested
    @DisplayName("updateMerchant")
    class UpdateMerchantTests {

        @Test
        @DisplayName("PUT should update the merchant and return the rows-affected count")
        void updateMerchant_shouldUpdate() throws Exception {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(MERCHANT_ID).cleanName("Corrected Name").build();
            when(merchantService.updateMerchant(eq(USER_ID), any(MerchantUpdateRequest.class))).thenReturn(1);

            // act & assert & verify
            mockMvc.perform(put("/api/v1/merchants")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(merchantService).updateMerchant(eq(USER_ID), any(MerchantUpdateRequest.class));
        }

        @Test
        @DisplayName("PUT should return 400 Bad Request when cleanName is blank")
        void updateMerchant_shouldReturn400WhenCleanNameBlank() throws Exception {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(MERCHANT_ID).cleanName("").build();

            // act & assert & verify
            mockMvc.perform(put("/api/v1/merchants")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("cleanName"));
        }

        @Test
        @DisplayName("PUT should return 404 Not Found when the merchant doesn't exist")
        void updateMerchant_shouldReturn404WhenNotFound() throws Exception {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(MERCHANT_ID).cleanName("Corrected Name").build();
            when(merchantService.updateMerchant(eq(USER_ID), any(MerchantUpdateRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Merchant not found."));

            // act & assert & verify
            mockMvc.perform(put("/api/v1/merchants")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Merchant not found."));
        }

        @Test
        @DisplayName("PF-220: PUT should return 403 Forbidden when the authenticated user does not own the "
                + "merchant -- exercises the real @PreAuthorize + SecurityService.isMerchantOwner path, not a "
                + "service-level exception standing in for it")
        void updateMerchant_shouldReturn403WhenNotOwner() throws Exception {
            // arrange -- overrides BaseControllerTest's default (permissive) stub for this one test
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(MERCHANT_ID).cleanName("Someone Else's Merchant").build();

            // act & assert & verify
            mockMvc.perform(put("/api/v1/merchants")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            // the request never reached the service -- @PreAuthorize denied it first
            verify(merchantService, never()).updateMerchant(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("mergeMerchants")
    class MergeMerchantsTests {

        private static final Long SURVIVING_ID = 101L;
        private static final Long MERGED_AWAY_ID = 102L;

        @Test
        @DisplayName("POST /merge should merge the two merchants and return 204")
        void mergeMerchants_shouldMerge() throws Exception {
            // arrange
            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(SURVIVING_ID)
                    .mergedAwayMerchantId(MERGED_AWAY_ID)
                    .build();

            // act & assert & verify
            mockMvc.perform(post("/api/v1/merchants/merge")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(merchantService).mergeMerchants(eq(USER_ID), any(MerchantMergeRequest.class));
        }

        @Test
        @DisplayName("POST /merge should return 400 Bad Request when either id is missing")
        void mergeMerchants_shouldReturn400WhenIdMissing() throws Exception {
            // arrange
            MerchantMergeRequest request = MerchantMergeRequest.builder().survivingMerchantId(SURVIVING_ID).build();

            // act & assert & verify
            mockMvc.perform(post("/api/v1/merchants/merge")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("mergedAwayMerchantId"));
        }

        @Test
        @DisplayName("POST /merge should return 400 Bad Request when merging a merchant into itself")
        void mergeMerchants_shouldReturn400ForSelfMerge() throws Exception {
            // arrange
            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(SURVIVING_ID)
                    .mergedAwayMerchantId(SURVIVING_ID)
                    .build();
            doThrow(new IllegalArgumentException("Cannot merge a merchant into itself."))
                    .when(merchantService).mergeMerchants(eq(USER_ID), any(MerchantMergeRequest.class));

            // act & assert & verify
            mockMvc.perform(post("/api/v1/merchants/merge")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Cannot merge a merchant into itself."));
        }

        @Test
        @DisplayName("PF-222: POST /merge should return 403 Forbidden when the user doesn't own the surviving merchant")
        void mergeMerchants_shouldReturn403WhenSurvivingNotOwned() throws Exception {
            // arrange -- overrides BaseControllerTest's default (permissive) stub for this one test
            when(securityService.isMerchantOwner(eq(SURVIVING_ID), any())).thenReturn(false);
            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(SURVIVING_ID)
                    .mergedAwayMerchantId(MERGED_AWAY_ID)
                    .build();

            // act & assert & verify
            mockMvc.perform(post("/api/v1/merchants/merge")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(merchantService, never()).mergeMerchants(anyLong(), any());
        }

        @Test
        @DisplayName("PF-222: POST /merge should return 403 Forbidden when the user doesn't own the "
                + "merged-away merchant, even though they DO own the surviving one -- both must be owned")
        void mergeMerchants_shouldReturn403WhenMergedAwayNotOwned() throws Exception {
            // arrange
            when(securityService.isMerchantOwner(eq(SURVIVING_ID), any())).thenReturn(true);
            when(securityService.isMerchantOwner(eq(MERGED_AWAY_ID), any())).thenReturn(false);
            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(SURVIVING_ID)
                    .mergedAwayMerchantId(MERGED_AWAY_ID)
                    .build();

            // act & assert & verify
            mockMvc.perform(post("/api/v1/merchants/merge")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(merchantService, never()).mergeMerchants(anyLong(), any());
        }
    }
}
