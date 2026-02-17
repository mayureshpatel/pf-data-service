package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.vendor.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.vendor.UnmatchedVendorDto;
import com.mayureshpatel.pfdataservice.dto.vendor.VendorRuleDto;
import com.mayureshpatel.pfdataservice.service.VendorRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendorRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
class VendorRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VendorRuleService vendorRuleService;

    @Test
    @WithCustomMockUser
    void getRules_ShouldReturnList() throws Exception {
        VendorRuleDto rule = VendorRuleDto.builder().id(1L).keyword("STARBUCKS").vendorName("Starbucks").build();
        when(vendorRuleService.getRules(1L)).thenReturn(List.of(rule));

        mockMvc.perform(get("/api/v1/vendor-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyword").value("STARBUCKS"));
    }

    @Test
    @WithCustomMockUser
    void createRule_ShouldReturnCreated() throws Exception {
        VendorRuleDto dto = VendorRuleDto.builder().keyword("New").vendorName("Vendor").build();
        VendorRuleDto response = VendorRuleDto.builder().id(10L).keyword("New").vendorName("Vendor").build();
        
        when(vendorRuleService.createRule(eq(1L), any(VendorRuleDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/vendor-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithCustomMockUser
    void applyRules_ShouldReturnOk() throws Exception {
        doNothing().when(vendorRuleService).applyRules(1L);

        mockMvc.perform(post("/api/v1/vendor-rules/apply"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomMockUser
    void previewApply_ShouldReturnList() throws Exception {
        RuleChangePreviewDto preview = new RuleChangePreviewDto("Desc", "Old", "New");
        when(vendorRuleService.previewApply(1L)).thenReturn(List.of(preview));

        mockMvc.perform(get("/api/v1/vendor-rules/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalDescription").value("Desc"));
    }

    @Test
    @WithCustomMockUser
    void getUnmatchedVendors_ShouldReturnList() throws Exception {
        UnmatchedVendorDto unmatched = new UnmatchedVendorDto("Unknown", 5);
        when(vendorRuleService.getUnmatchedVendors(1L)).thenReturn(List.of(unmatched));

        mockMvc.perform(get("/api/v1/vendor-rules/unmatched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalName").value("Unknown"));
    }

    @Test
    @WithCustomMockUser
    void deleteRule_ShouldReturnNoContent() throws Exception {
        doNothing().when(vendorRuleService).deleteRule(1L, 1L);

        mockMvc.perform(delete("/api/v1/vendor-rules/1"))
                .andExpect(status().isNoContent());
    }
}
