package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantMergeRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantReviewClusterDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        @DisplayName("GET should return a page of the authenticated user's merchants")
        void getMerchants_shouldReturnPage() throws Exception {
            // arrange
            MerchantDto dto = MerchantDto.builder().id(MERCHANT_ID).userId(USER_ID).originalName("STARBUCKS #1").cleanName("Starbucks").build();
            Page<MerchantDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
            when(merchantService.getAllMerchants(eq(USER_ID), eq(null), any(Pageable.class))).thenReturn(page);

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(MERCHANT_ID))
                    .andExpect(jsonPath("$.content[0].cleanName").value("Starbucks"));

            verify(merchantService).getAllMerchants(eq(USER_ID), eq(null), any(Pageable.class));
        }

        @Test
        @DisplayName("PF-320: GET should pass a search query param through to the service")
        void getMerchants_shouldPassThroughSearch() throws Exception {
            // arrange
            Page<MerchantDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(merchantService.getAllMerchants(eq(USER_ID), anyString(), any(Pageable.class))).thenReturn(page);

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants").param("search", "starbucks"))
                    .andExpect(status().isOk());

            verify(merchantService).getAllMerchants(eq(USER_ID), eq("starbucks"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getDistinctCleanNames")
    class GetDistinctCleanNamesTests {

        @Test
        @DisplayName("GET /clean-names should return a page of the authenticated user's distinct clean names")
        void getDistinctCleanNames_shouldReturnPage() throws Exception {
            // arrange
            Page<String> page = new PageImpl<>(List.of("Kroger", "Starbucks"), PageRequest.of(0, 20), 2);
            when(merchantService.getDistinctCleanNames(eq(USER_ID), eq(null), any(Pageable.class))).thenReturn(page);

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants/clean-names"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0]").value("Kroger"));

            verify(merchantService).getDistinctCleanNames(eq(USER_ID), eq(null), any(Pageable.class));
        }

        @Test
        @DisplayName("GET /clean-names should pass a search query param through to the service")
        void getDistinctCleanNames_shouldPassThroughSearch() throws Exception {
            // arrange
            Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(merchantService.getDistinctCleanNames(eq(USER_ID), anyString(), any(Pageable.class))).thenReturn(page);

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants/clean-names").param("search", "kro"))
                    .andExpect(status().isOk());

            verify(merchantService).getDistinctCleanNames(eq(USER_ID), eq("kro"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getMerchantsByCleanName")
    class GetMerchantsByCleanNameTests {

        @Test
        @DisplayName("GET /by-clean-name should return every merchant sharing the given exact clean name")
        void getMerchantsByCleanName_shouldReturnGroup() throws Exception {
            // arrange
            MerchantDto a = MerchantDto.builder().id(1L).userId(USER_ID).originalName("KROGER #431 ROSWELL").cleanName("Kroger").build();
            MerchantDto b = MerchantDto.builder().id(2L).userId(USER_ID).originalName("KROGER #696 WARNER ROBINS").cleanName("Kroger").build();
            when(merchantService.getMerchantsByCleanName(USER_ID, "Kroger")).thenReturn(List.of(a, b));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants/by-clean-name").param("cleanName", "Kroger"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[1].id").value(2L));

            verify(merchantService).getMerchantsByCleanName(USER_ID, "Kroger");
        }
    }

    @Nested
    @DisplayName("getMerchantsNeedingReview")
    class GetMerchantsNeedingReviewTests {

        @Test
        @DisplayName("GET /needs-review should return the authenticated user's review clusters")
        void getMerchantsNeedingReview_shouldReturnClusters() throws Exception {
            // arrange
            MerchantDto a = MerchantDto.builder().id(1L).userId(USER_ID).originalName("KROGER #431 ROSWELL GA").cleanName("").build();
            MerchantReviewClusterDto cluster = MerchantReviewClusterDto.builder()
                    .suggestedCleanName("Kroger")
                    .merchants(List.of(a))
                    .build();
            when(merchantService.getMerchantsNeedingReview(USER_ID)).thenReturn(List.of(cluster));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants/needs-review"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].suggestedCleanName").value("Kroger"))
                    .andExpect(jsonPath("$[0].merchants", hasSize(1)));

            verify(merchantService).getMerchantsNeedingReview(USER_ID);
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
    @DisplayName("updateMerchantsBulk")
    class UpdateMerchantsBulkTests {

        @Test
        @DisplayName("PATCH /bulk should update every merchant in the request and return the total rows-affected count")
        void updateMerchantsBulk_shouldUpdateAll() throws Exception {
            // arrange
            List<MerchantUpdateRequest> requests = List.of(
                    MerchantUpdateRequest.builder().id(1L).cleanName("Kroger").build(),
                    MerchantUpdateRequest.builder().id(2L).cleanName("Kroger").build());
            when(merchantService.updateMerchantsBulk(eq(USER_ID), any())).thenReturn(2);

            // act & assert & verify
            mockMvc.perform(patch("/api/v1/merchants/bulk")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("2"));

            verify(merchantService).updateMerchantsBulk(eq(USER_ID), any());
        }

        @Test
        @DisplayName("PATCH /bulk should return 400 Bad Request when more than 1000 items are sent")
        void updateMerchantsBulk_shouldReturn400WhenOverLimit() throws Exception {
            // arrange
            List<MerchantUpdateRequest> requests = IntStream.rangeClosed(1, 1001)
                    .mapToObj(i -> MerchantUpdateRequest.builder().id((long) i).cleanName("Name " + i).build())
                    .toList();

            // act & assert & verify
            mockMvc.perform(patch("/api/v1/merchants/bulk")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isBadRequest());

            verify(merchantService, never()).updateMerchantsBulk(anyLong(), any());
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
