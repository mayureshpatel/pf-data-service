package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkChangeDto;
import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkUndoResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkBulkCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantSuggestionRequest;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies {@code MerchantController}'s post-PF-845 CRUD and description-link endpoints, one {@code @Nested} class per endpoint below, including the real {@code @PreAuthorize} + {@code SecurityService#isMerchantOwner} enforcement path (not a service-level exception standing in for it) for every write. */
@DisplayName("MerchantController Unit Tests")
@WithCustomMockUser()
class MerchantControllerTest extends BaseControllerTest {

    private static final Long MERCHANT_ID = 101L;
    private static final Long LINK_ID = 201L;
    private static final String BASE_URL = "/api/v1/merchants";
    private static final String DESCRIPTION_LINKS_URL = "/api/v1/merchants/{id}/description-links";
    private static final String DESCRIPTION_STARBUCKS = "STARBUCKS #1";

    private static DescriptionLinkResultDto linkResult() {
        return new DescriptionLinkResultDto(MERCHANT_ID, 1, 1, 2,
                List.of(new DescriptionLinkChangeDto("starbucks #1", 55L)), List.of(9L, 10L));
    }

    /** {@code GET /api/v1/merchants} returns a {@link Page} of the caller's merchants, and -- a PF-320 case -- passes an optional {@code search} query param straight through to the service. */
    @Nested
    @DisplayName("getMerchants")
    class GetMerchantsTests {

        @Test
        @DisplayName("GET should return a page of the authenticated user's merchants")
        void getMerchants_shouldReturnPage() throws Exception {
            // arrange
            MerchantDto dto = MerchantDto.builder().id(MERCHANT_ID).userId(USER_ID).name("Starbucks").linkCount(3).build();
            Page<MerchantDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
            when(merchantService.getAllMerchants(eq(USER_ID), eq(null), any(Pageable.class))).thenReturn(page);

            // act & assert & verify
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(MERCHANT_ID))
                    .andExpect(jsonPath("$.content[0].name").value("Starbucks"))
                    .andExpect(jsonPath("$.content[0].linkCount").value(3));

            verify(merchantService).getAllMerchants(eq(USER_ID), eq(null), any(Pageable.class));
        }

        @Test
        @DisplayName("PF-320: GET should pass a search query param through to the service")
        void getMerchants_shouldPassThroughSearch() throws Exception {
            // arrange
            Page<MerchantDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(merchantService.getAllMerchants(eq(USER_ID), anyString(), any(Pageable.class))).thenReturn(page);

            // act & assert & verify
            mockMvc.perform(get(BASE_URL).param("search", "starbucks"))
                    .andExpect(status().isOk());

            verify(merchantService).getAllMerchants(eq(USER_ID), eq("starbucks"), any(Pageable.class));
        }
    }

    /** {@code POST /api/v1/merchants} creates the merchant and returns its new id, and returns a 400 with field-level {@code validationErrors} for a blank {@code name}. */
    /** {@code POST /api/v1/merchants/suggestions} (PF-982) passes the names and limit (default 5) to the service and validates the body. */
    @Nested
    @DisplayName("suggestMerchants")
    class SuggestMerchantsTests {

        private static final String URL = BASE_URL + "/suggestions";

        @Test
        @DisplayName("POST should return suggestions per name, defaulting the limit to 5")
        void suggestMerchants_shouldReturnSuggestions() throws Exception {
            // arrange
            MerchantDto kroger = new MerchantDto(MERCHANT_ID, USER_ID, "Kroger", null, null, null, null, 0);
            when(merchantService.suggestMerchants(USER_ID, List.of("KROGER, INC"), 5))
                    .thenReturn(List.of(new MerchantSuggestionDto("KROGER, INC", List.of(kroger))));

            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new MerchantSuggestionRequest(List.of("KROGER, INC"), null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("KROGER, INC"))
                    .andExpect(jsonPath("$[0].merchants[0].id").value(MERCHANT_ID));
            verify(merchantService).suggestMerchants(USER_ID, List.of("KROGER, INC"), 5);
        }

        @Test
        @DisplayName("POST should return 400 for a limit over 20 or more than 100 names")
        void suggestMerchants_shouldValidate() throws Exception {
            // arrange
            List<String> tooMany = IntStream.range(0, 101).mapToObj(i -> "N" + i).toList();

            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new MerchantSuggestionRequest(List.of("A"), 21))))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new MerchantSuggestionRequest(tooMany, 5))))
                    .andExpect(status().isBadRequest());
            verify(merchantService, never()).suggestMerchants(anyLong(), anyList(), anyInt());
        }

        @Test
        @DisplayName("POST should return 400 for a null name or a name over 255 characters")
        void suggestMerchants_shouldValidateEachName() throws Exception {
            // arrange
            String nullName = "{\"names\":[\"KROGER\",null],\"limit\":5}";
            String longName = objectMapper.writeValueAsString(new MerchantSuggestionRequest(List.of("K".repeat(256)), 5));

            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(nullName))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(longName))
                    .andExpect(status().isBadRequest());
            verify(merchantService, never()).suggestMerchants(anyLong(), anyList(), anyInt());
        }
    }

    @Nested
    @DisplayName("createMerchant")
    class CreateMerchantTests {

        @Test
        @DisplayName("POST should create the merchant and return the new id")
        void createMerchant_shouldCreate() throws Exception {
            // arrange
            MerchantCreateRequest request = MerchantCreateRequest.builder().userId(USER_ID).name("Trader Joe's").build();
            when(merchantService.createMerchant(eq(USER_ID), any(MerchantCreateRequest.class))).thenReturn(42L);

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("42"));

            verify(merchantService).createMerchant(eq(USER_ID), any(MerchantCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 Bad Request when name is blank")
        void createMerchant_shouldReturn400WhenNameBlank() throws Exception {
            // arrange
            MerchantCreateRequest request = MerchantCreateRequest.builder().userId(USER_ID).name("").build();

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("name"));
        }
    }

    /**
     * {@code PUT /api/v1/merchants} updates the merchant and returns the affected-row count,
     * returns a 400 with field-level {@code validationErrors} for a blank {@code name}, a 404
     * when the service throws {@link ResourceNotFoundException}, and -- unlike the 404 case,
     * which exercises a service-level exception -- a 403 that's confirmed to come from the real
     * {@code @PreAuthorize("@ss.isMerchantOwner(...)")} check itself: the service method is
     * verified never invoked at all when {@code securityService.isMerchantOwner} is stubbed false.
     */
    @Nested
    @DisplayName("updateMerchant")
    class UpdateMerchantTests {

        @Test
        @DisplayName("PUT should update the merchant and return the rows-affected count")
        void updateMerchant_shouldUpdate() throws Exception {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(MERCHANT_ID).name("Corrected Name").build();
            when(merchantService.updateMerchant(eq(USER_ID), any(MerchantUpdateRequest.class))).thenReturn(1);

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(merchantService).updateMerchant(eq(USER_ID), any(MerchantUpdateRequest.class));
        }

        @Test
        @DisplayName("PUT should return 400 Bad Request when name is blank")
        void updateMerchant_shouldReturn400WhenNameBlank() throws Exception {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(MERCHANT_ID).name("").build();

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("name"));
        }

        @Test
        @DisplayName("PUT should return 404 Not Found when the merchant doesn't exist")
        void updateMerchant_shouldReturn404WhenNotFound() throws Exception {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(MERCHANT_ID).name("Corrected Name").build();
            when(merchantService.updateMerchant(eq(USER_ID), any(MerchantUpdateRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Merchant not found."));

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Merchant not found."));
        }

        @Test
        @DisplayName("PUT should return 403 Forbidden when the authenticated user does not own the merchant "
                + "-- exercises the real @PreAuthorize + SecurityService.isMerchantOwner path, not a "
                + "service-level exception standing in for it")
        void updateMerchant_shouldReturn403WhenNotOwner() throws Exception {
            // arrange -- overrides BaseControllerTest's default (permissive) stub for this one test
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(MERCHANT_ID).name("Someone Else's Merchant").build();

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            // the request never reached the service -- @PreAuthorize denied it first
            verify(merchantService, never()).updateMerchant(anyLong(), any());
        }
    }

    /** {@code DELETE /api/v1/merchants/{id}} returns 204 on success, and a real {@code @PreAuthorize}-enforced 403 (service never invoked) when {@code securityService.isMerchantOwner} is stubbed false. */
    @Nested
    @DisplayName("deleteMerchant")
    class DeleteMerchantTests {

        @Test
        @DisplayName("DELETE should delete the merchant and return 204")
        void deleteMerchant_shouldDelete() throws Exception {
            // act & assert & verify
            mockMvc.perform(delete("/api/v1/merchants/{id}", MERCHANT_ID).with(csrf()))
                    .andExpect(status().isNoContent());

            verify(merchantService).deleteMerchant(USER_ID, MERCHANT_ID);
        }

        @Test
        @DisplayName("DELETE should return 403 Forbidden when the authenticated user does not own the merchant")
        void deleteMerchant_shouldReturn403WhenNotOwner() throws Exception {
            // arrange
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);

            // act & assert & verify
            mockMvc.perform(delete("/api/v1/merchants/{id}", MERCHANT_ID).with(csrf()))
                    .andExpect(status().isForbidden());

            verify(merchantService, never()).deleteMerchant(anyLong(), anyLong());
        }
    }

    /** {@code GET /api/v1/merchants/{id}/description-links} returns the merchant's linked descriptions, and a real {@code @PreAuthorize}-enforced 403 when {@code securityService.isMerchantOwner} is stubbed false. */
    @Nested
    @DisplayName("getDescriptionLinks")
    class GetDescriptionLinksTests {

        @Test
        @DisplayName("GET /{id}/description-links should return the merchant's linked descriptions")
        void getDescriptionLinks_shouldReturnLinks() throws Exception {
            // arrange
            MerchantDescriptionLinkDto link = MerchantDescriptionLinkDto.builder()
                    .id(LINK_ID).merchantId(MERCHANT_ID).description(DESCRIPTION_STARBUCKS).build();
            when(merchantService.getDescriptionLinks(USER_ID, MERCHANT_ID)).thenReturn(List.of(link));

            // act & assert & verify
            mockMvc.perform(get(DESCRIPTION_LINKS_URL, MERCHANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].description").value(DESCRIPTION_STARBUCKS));

            verify(merchantService).getDescriptionLinks(USER_ID, MERCHANT_ID);
        }

        @Test
        @DisplayName("GET /{id}/description-links should return 403 Forbidden when not owner")
        void getDescriptionLinks_shouldReturn403WhenNotOwner() throws Exception {
            // arrange
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);

            // act & assert & verify
            mockMvc.perform(get(DESCRIPTION_LINKS_URL, MERCHANT_ID))
                    .andExpect(status().isForbidden());
        }
    }

    /** {@code POST /api/v1/merchants/{id}/description-links} records the link and returns 200 with the undo-ready result, returns a 400 (and never calls the service) for a blank description, and a real {@code @PreAuthorize}-enforced 403 (service never invoked) when {@code securityService.isMerchantOwner} is stubbed false. */
    @Nested
    @DisplayName("addDescriptionLink")
    class AddDescriptionLinkTests {

        @Test
        @DisplayName("POST /{id}/description-links should link, fill and return 200 with the result")
        void addDescriptionLink_shouldRecord() throws Exception {
            // arrange
            MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest(DESCRIPTION_STARBUCKS);
            when(descriptionLinkService.linkDescriptions(USER_ID, MERCHANT_ID, List.of(DESCRIPTION_STARBUCKS))).thenReturn(linkResult());

            // act & assert & verify
            mockMvc.perform(post(DESCRIPTION_LINKS_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID))
                    .andExpect(jsonPath("$.movedCount").value(1))
                    .andExpect(jsonPath("$.transactionsFilled").value(2))
                    .andExpect(jsonPath("$.linkChanges[0].previousMerchantId").value(55))
                    .andExpect(jsonPath("$.filledTransactionIds", hasSize(2)));

            verify(descriptionLinkService).linkDescriptions(USER_ID, MERCHANT_ID, List.of(DESCRIPTION_STARBUCKS));
        }

        @Test
        @DisplayName("POST /{id}/description-links should return 400 Bad Request when description is blank")
        void addDescriptionLink_shouldReturn400WhenBlank() throws Exception {
            // arrange
            MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest("");

            // act & assert & verify
            mockMvc.perform(post(DESCRIPTION_LINKS_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(descriptionLinkService, never()).linkDescriptions(anyLong(), anyLong(), anyList());
        }

        @Test
        @DisplayName("POST /{id}/description-links should return 403 Forbidden when not owner")
        void addDescriptionLink_shouldReturn403WhenNotOwner() throws Exception {
            // arrange
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);
            MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest(DESCRIPTION_STARBUCKS);

            // act & assert & verify
            mockMvc.perform(post(DESCRIPTION_LINKS_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(descriptionLinkService, never()).linkDescriptions(anyLong(), anyLong(), anyList());
        }
    }

    /** {@code POST /api/v1/merchants/{id}/description-links/bulk} records every description and returns 200 with the result, returns a 400 (and never calls the service) for an empty list or a blank entry, and a real {@code @PreAuthorize}-enforced 403 when {@code securityService.isMerchantOwner} is stubbed false. */
    @Nested
    @DisplayName("addDescriptionLinks")
    class AddDescriptionLinksTests {

        private static final String BULK_URL = DESCRIPTION_LINKS_URL + "/bulk";

        @Test
        @DisplayName("POST /{id}/description-links/bulk should link every description and return 200 with the result")
        void addDescriptionLinks_shouldRecord() throws Exception {
            // arrange
            MerchantDescriptionLinkBulkCreateRequest request = new MerchantDescriptionLinkBulkCreateRequest(List.of(DESCRIPTION_STARBUCKS, "SHELL OIL"));
            when(descriptionLinkService.linkDescriptions(USER_ID, MERCHANT_ID, List.of(DESCRIPTION_STARBUCKS, "SHELL OIL"))).thenReturn(linkResult());

            // act & assert & verify
            mockMvc.perform(post(BULK_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.linkedCount").value(1));

            verify(descriptionLinkService).linkDescriptions(USER_ID, MERCHANT_ID, List.of(DESCRIPTION_STARBUCKS, "SHELL OIL"));
        }

        @Test
        @DisplayName("POST /{id}/description-links/bulk should return 400 Bad Request for an empty list")
        void addDescriptionLinks_shouldReturn400WhenEmpty() throws Exception {
            // arrange
            MerchantDescriptionLinkBulkCreateRequest request = new MerchantDescriptionLinkBulkCreateRequest(List.of());

            // act & assert & verify
            mockMvc.perform(post(BULK_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(descriptionLinkService, never()).linkDescriptions(anyLong(), anyLong(), anyList());
        }

        @Test
        @DisplayName("POST /{id}/description-links/bulk should return 400 Bad Request when an entry is blank")
        void addDescriptionLinks_shouldReturn400WhenEntryBlank() throws Exception {
            // arrange
            MerchantDescriptionLinkBulkCreateRequest request = new MerchantDescriptionLinkBulkCreateRequest(List.of(DESCRIPTION_STARBUCKS, " "));

            // act & assert & verify
            mockMvc.perform(post(BULK_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(descriptionLinkService, never()).linkDescriptions(anyLong(), anyLong(), anyList());
        }

        @Test
        @DisplayName("POST /{id}/description-links/bulk should return 403 Forbidden when not owner")
        void addDescriptionLinks_shouldReturn403WhenNotOwner() throws Exception {
            // arrange
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);
            MerchantDescriptionLinkBulkCreateRequest request = new MerchantDescriptionLinkBulkCreateRequest(List.of(DESCRIPTION_STARBUCKS));

            // act & assert & verify
            mockMvc.perform(post(BULK_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(descriptionLinkService, never()).linkDescriptions(anyLong(), anyLong(), anyList());
        }
    }

    /** {@code POST /api/v1/merchants/{id}/description-links/undo} takes a link result back and returns what was restored, rejects a result for another merchant or a malformed one with 400, and returns a real {@code @PreAuthorize}-enforced 403 when not owner. */
    @Nested
    @DisplayName("undoDescriptionLinks")
    class UndoDescriptionLinksTests {

        private static final String UNDO_URL = DESCRIPTION_LINKS_URL + "/undo";

        @Test
        @DisplayName("POST /{id}/description-links/undo should undo and return 200 with the undo result")
        void undo_shouldReturnResult() throws Exception {
            // arrange
            DescriptionLinkResultDto result = linkResult();
            when(descriptionLinkService.undoLink(USER_ID, result)).thenReturn(new DescriptionLinkUndoResultDto(1, 0, 1, List.of(10L)));

            // act & assert & verify
            mockMvc.perform(post(UNDO_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(result)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.linksRestored").value(1))
                    .andExpect(jsonPath("$.transactionsRestored").value(1))
                    .andExpect(jsonPath("$.skippedTransactionIds[0]").value(10));

            verify(descriptionLinkService).undoLink(USER_ID, result);
        }

        @Test
        @DisplayName("POST /{id}/description-links/undo should return 400 when the result is for another merchant")
        void undo_shouldReturn400ForOtherMerchant() throws Exception {
            // arrange
            DescriptionLinkResultDto result = new DescriptionLinkResultDto(999L, 0, 0, 0, List.of(), List.of());

            // act & assert & verify
            mockMvc.perform(post(UNDO_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(result)))
                    .andExpect(status().isBadRequest());

            verify(descriptionLinkService, never()).undoLink(anyLong(), any());
        }

        @Test
        @DisplayName("POST /{id}/description-links/undo should return 400 when the lists are missing")
        void undo_shouldReturn400WhenMalformed() throws Exception {
            // arrange
            String body = "{\"merchantId\":" + MERCHANT_ID + "}";

            // act & assert & verify
            mockMvc.perform(post(UNDO_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(descriptionLinkService, never()).undoLink(anyLong(), any());
        }

        @Test
        @DisplayName("POST /{id}/description-links/undo should return 403 Forbidden when not owner")
        void undo_shouldReturn403WhenNotOwner() throws Exception {
            // arrange
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);

            // act & assert & verify
            mockMvc.perform(post(UNDO_URL, MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(linkResult())))
                    .andExpect(status().isForbidden());

            verify(descriptionLinkService, never()).undoLink(anyLong(), any());
        }
    }

    /** {@code DELETE /api/v1/merchants/{id}/description-links/{linkId}} deletes the link and returns 204, and a real {@code @PreAuthorize}-enforced 403 (service never invoked) when {@code securityService.isMerchantOwner} is stubbed false -- note the ownership check here is still against the parent merchant id, not the link id itself. */
    @Nested
    @DisplayName("deleteDescriptionLink")
    class DeleteDescriptionLinkTests {

        @Test
        @DisplayName("DELETE /{id}/description-links/{linkId} should delete the link and return 204")
        void deleteDescriptionLink_shouldDelete() throws Exception {
            // act & assert & verify
            mockMvc.perform(delete("/api/v1/merchants/{id}/description-links/{linkId}", MERCHANT_ID, LINK_ID).with(csrf()))
                    .andExpect(status().isNoContent());

            verify(merchantService).deleteDescriptionLink(USER_ID, LINK_ID);
        }

        @Test
        @DisplayName("DELETE /{id}/description-links/{linkId} should return 403 Forbidden when not owner")
        void deleteDescriptionLink_shouldReturn403WhenNotOwner() throws Exception {
            // arrange
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);

            // act & assert & verify
            mockMvc.perform(delete("/api/v1/merchants/{id}/description-links/{linkId}", MERCHANT_ID, LINK_ID).with(csrf()))
                    .andExpect(status().isForbidden());

            verify(merchantService, never()).deleteDescriptionLink(anyLong(), anyLong());
        }
    }
}
