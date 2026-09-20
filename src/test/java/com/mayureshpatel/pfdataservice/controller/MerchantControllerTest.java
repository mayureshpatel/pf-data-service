package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
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

@DisplayName("MerchantController Unit Tests")
@WithCustomMockUser()
class MerchantControllerTest extends BaseControllerTest {

    private static final Long MERCHANT_ID = 101L;
    private static final Long LINK_ID = 201L;

    @Nested
    @DisplayName("getMerchants")
    class GetMerchantsTests {

        @Test
        @DisplayName("GET should return a page of the authenticated user's merchants")
        void getMerchants_shouldReturnPage() throws Exception {
            // arrange
            MerchantDto dto = MerchantDto.builder().id(MERCHANT_ID).userId(USER_ID).name("Starbucks").build();
            Page<MerchantDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
            when(merchantService.getAllMerchants(eq(USER_ID), eq(null), any(Pageable.class))).thenReturn(page);

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(MERCHANT_ID))
                    .andExpect(jsonPath("$.content[0].name").value("Starbucks"));

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
    @DisplayName("createMerchant")
    class CreateMerchantTests {

        @Test
        @DisplayName("POST should create the merchant and return the new id")
        void createMerchant_shouldCreate() throws Exception {
            // arrange
            MerchantCreateRequest request = MerchantCreateRequest.builder().userId(USER_ID).name("Trader Joe's").build();
            when(merchantService.createMerchant(eq(USER_ID), any(MerchantCreateRequest.class))).thenReturn(42L);

            // act & assert & verify
            mockMvc.perform(post("/api/v1/merchants")
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
            mockMvc.perform(post("/api/v1/merchants")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("name"));
        }
    }

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
            mockMvc.perform(put("/api/v1/merchants")
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
            mockMvc.perform(put("/api/v1/merchants")
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
            mockMvc.perform(put("/api/v1/merchants")
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

    @Nested
    @DisplayName("getDescriptionLinks")
    class GetDescriptionLinksTests {

        @Test
        @DisplayName("GET /{id}/description-links should return the merchant's linked descriptions")
        void getDescriptionLinks_shouldReturnLinks() throws Exception {
            // arrange
            MerchantDescriptionLinkDto link = MerchantDescriptionLinkDto.builder()
                    .id(LINK_ID).merchantId(MERCHANT_ID).description("STARBUCKS #1").build();
            when(merchantService.getDescriptionLinks(USER_ID, MERCHANT_ID)).thenReturn(List.of(link));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants/{id}/description-links", MERCHANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].description").value("STARBUCKS #1"));

            verify(merchantService).getDescriptionLinks(USER_ID, MERCHANT_ID);
        }

        @Test
        @DisplayName("GET /{id}/description-links should return 403 Forbidden when not owner")
        void getDescriptionLinks_shouldReturn403WhenNotOwner() throws Exception {
            // arrange
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);

            // act & assert & verify
            mockMvc.perform(get("/api/v1/merchants/{id}/description-links", MERCHANT_ID))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("addDescriptionLink")
    class AddDescriptionLinkTests {

        @Test
        @DisplayName("POST /{id}/description-links should record the link and return 204")
        void addDescriptionLink_shouldRecord() throws Exception {
            // arrange
            MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest("STARBUCKS #1");

            // act & assert & verify
            mockMvc.perform(post("/api/v1/merchants/{id}/description-links", MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(merchantService).recordDescriptionLink(USER_ID, MERCHANT_ID, "STARBUCKS #1");
        }

        @Test
        @DisplayName("POST /{id}/description-links should return 400 Bad Request when description is blank")
        void addDescriptionLink_shouldReturn400WhenBlank() throws Exception {
            // arrange
            MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest("");

            // act & assert & verify
            mockMvc.perform(post("/api/v1/merchants/{id}/description-links", MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(merchantService, never()).recordDescriptionLink(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("POST /{id}/description-links should return 403 Forbidden when not owner")
        void addDescriptionLink_shouldReturn403WhenNotOwner() throws Exception {
            // arrange
            when(securityService.isMerchantOwner(eq(MERCHANT_ID), any())).thenReturn(false);
            MerchantDescriptionLinkCreateRequest request = new MerchantDescriptionLinkCreateRequest("STARBUCKS #1");

            // act & assert & verify
            mockMvc.perform(post("/api/v1/merchants/{id}/description-links", MERCHANT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(merchantService, never()).recordDescriptionLink(anyLong(), anyLong(), anyString());
        }
    }

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
