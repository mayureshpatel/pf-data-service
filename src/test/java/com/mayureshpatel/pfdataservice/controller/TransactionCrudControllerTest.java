package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.UserRepository;
import com.mayureshpatel.pfdataservice.repository.specification.TransactionSpecification.TransactionFilter;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.security.SecurityService;
import com.mayureshpatel.pfdataservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionCrudController.class)
@AutoConfigureMockMvc(addFilters = false) 
class TransactionCrudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean(name = "ss")
    private SecurityService securityService;

    @MockitoBean
    private JwtService jwtService;
    
    @MockitoBean
    private UserRepository userRepository;

    // We use @Autowired because we defined it in TestConfig
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }
    }

    @Test
    @WithCustomMockUser
    void getTransactions_ShouldReturnPage() throws Exception {
        TransactionDto dto = TransactionDto.builder()
                .amount(BigDecimal.TEN)
                .description("Test")
                .date(LocalDate.now())
                .type(TransactionType.EXPENSE)
                .build();
        Page<TransactionDto> page = new PageImpl<>(Collections.singletonList(dto));

        when(transactionService.getTransactions(any(), any(TransactionFilter.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Test"));
    }

    @Test
    @WithCustomMockUser
    void updateTransaction_ShouldReturnUpdated() throws Exception {
        Long id = 1L;
        TransactionDto input = TransactionDto.builder()
                .amount(BigDecimal.valueOf(20))
                .description("Updated")
                .date(LocalDate.now())
                .type(TransactionType.INCOME)
                .build();

        // Mock Security Check
        when(securityService.isTransactionOwner(eq(id), any())).thenReturn(true);
        when(transactionService.updateTransaction(any(), eq(id), any())).thenReturn(input);

        mockMvc.perform(put("/api/v1/transactions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(20));
    }

    @Test
    @WithCustomMockUser
    void deleteTransaction_ShouldReturnNoContent() throws Exception {
        Long id = 1L;

        // Mock Security Check
        when(securityService.isTransactionOwner(eq(id), any())).thenReturn(true);

        mockMvc.perform(delete("/api/v1/transactions/{id}", id))
                .andExpect(status().isNoContent());

        verify(transactionService).deleteTransaction(any(), eq(id));
    }
}
