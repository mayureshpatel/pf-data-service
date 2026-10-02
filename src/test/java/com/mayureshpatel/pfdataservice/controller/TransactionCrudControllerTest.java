package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.TransactionFilter;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link TransactionCrudController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("TransactionCrudController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class TransactionCrudControllerTest extends BaseControllerTest {

    private static final String BASE_URL = "/api/v1/transactions";
    private static final Long TRANSACTION_ID = 1L;

    /** {@code GET /api/v1/transactions/suggestions/transfers} delegates to {@code TransactionService#findPotentialTransfers}. */
    @Nested
    @DisplayName("getTransferSuggestions")
    class GetTransferSuggestionsTests {
        @Test
        @DisplayName("GET /suggestions/transfers should return suggestions")
        void getTransferSuggestions_shouldReturnList() throws Exception {
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of());
            mockMvc.perform(get("/api/v1/transactions/suggestions/transfers"))
                    .andExpect(status().isOk());
            verify(transactionService).findPotentialTransfers(USER_ID);
        }
    }

    /** {@code POST /api/v1/transactions/mark-as-transfer} passes the request body's id list straight through to {@code TransactionService#markAsTransfer}. */
    @Nested
    @DisplayName("markAsTransfer")
    class MarkAsTransferTests {
        @Test
        @DisplayName("POST /mark-as-transfer should mark transactions as transfers")
        void markAsTransfer_shouldReturnOk() throws Exception {
            List<Long> ids = List.of(1L, 2L);
            mockMvc.perform(post("/api/v1/transactions/mark-as-transfer")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ids)))
                    .andExpect(status().isOk());
            verify(transactionService).markAsTransfer(USER_ID, ids);
        }
    }

    /** A PF-831 addition: {@code POST /api/v1/transactions/unmark-as-transfer} passes the request body's id list straight through to {@code TransactionService#unmarkAsTransfer}. */
    @Nested
    @DisplayName("unmarkAsTransfer (PF-831)")
    class UnmarkAsTransferTests {
        @Test
        @DisplayName("POST /unmark-as-transfer should unmark transactions")
        void unmarkAsTransfer_shouldReturnOk() throws Exception {
            List<Long> ids = List.of(1L, 2L);
            mockMvc.perform(post("/api/v1/transactions/unmark-as-transfer")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ids)))
                    .andExpect(status().isOk());
            verify(transactionService).unmarkAsTransfer(USER_ID, ids);
        }
    }

    /** A PF-848 addition: {@code POST /api/v1/transactions/backfill/transfer-types} returns the real corrected-row count from {@code TransactionService#backfillTransferTypes}. */
    @Nested
    @DisplayName("backfillTransferTypes (PF-848)")
    class BackfillTransferTypesTests {
        @Test
        @DisplayName("POST /backfill/transfer-types should return the corrected count")
        void backfillTransferTypes_shouldReturnCount() throws Exception {
            when(transactionService.backfillTransferTypes(USER_ID)).thenReturn(3);
            mockMvc.perform(post("/api/v1/transactions/backfill/transfer-types")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("3"));
            verify(transactionService).backfillTransferTypes(USER_ID);
        }
    }

    /** {@code GET /api/v1/transactions} returns a {@link Page} built from request query params, and -- a PF-308 case -- a {@code tagId} query param is genuinely parsed into the {@link TransactionFilter} passed to the service, confirmed by inspecting the captured filter rather than just asserting the call happened. */
    @Nested
    @DisplayName("getTransactions")
    class GetTransactionsTests {
        @Test
        @DisplayName("GET should return paginated transactions")
        void getTransactions_shouldReturnPage() throws Exception {
            Page<TransactionDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            // Resolve ambiguity by explicitly using TransactionFilter class
            when(transactionService.getTransactions(eq(USER_ID), any(TransactionFilter.class), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get(BASE_URL)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @DisplayName("PF-308: GET should pass the tagId query param through to the filter")
        void getTransactions_shouldPassThroughTagId() throws Exception {
            Page<TransactionDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(transactionService.getTransactions(eq(USER_ID), any(TransactionFilter.class), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get(BASE_URL)
                            .param("tagId", "42"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactions(
                    eq(USER_ID),
                    argThat((TransactionFilter filter) -> filter.tagId() != null && filter.tagId().equals(42L)),
                    any(Pageable.class));
        }
    }

    /** {@code GET /api/v1/transactions/count-by-category} delegates to {@code TransactionService#getCountByCategory}. */
    @Nested
    @DisplayName("getCountByCategory")
    class GetCountByCategoryTests {
        @Test
        @DisplayName("GET /count-by-category should return counts")
        void getCountByCategory_shouldReturnList() throws Exception {
            when(transactionService.getCountByCategory(USER_ID)).thenReturn(List.of());
            mockMvc.perform(get("/api/v1/transactions/count-by-category"))
                    .andExpect(status().isOk());
        }
    }

    /** {@code GET /api/v1/transactions/existing-categories} delegates to {@code TransactionService#getCategoriesWithTransactions}. */
    @Nested
    @DisplayName("getAllCategoriesWithTransactions")
    class GetAllCategoriesWithTransactionsTests {
        @Test
        @DisplayName("GET /existing-categories should return categories")
        void getAllCategoriesWithTransactions_shouldReturnList() throws Exception {
            when(transactionService.getCategoriesWithTransactions(USER_ID)).thenReturn(List.of());
            mockMvc.perform(get("/api/v1/transactions/existing-categories"))
                    .andExpect(status().isOk());
        }
    }

    /** {@code GET /api/v1/transactions/existing-merchants} delegates to {@code TransactionService#getMerchantsWithTransactions}. */
    @Nested
    @DisplayName("getAllMerchantsWithTransactions")
    class GetAllMerchantsWithTransactionsTests {
        @Test
        @DisplayName("GET /existing-merchants should return merchants")
        void getAllMerchantsWithTransactions_shouldReturnList() throws Exception {
            when(transactionService.getMerchantsWithTransactions(USER_ID)).thenReturn(List.of());
            mockMvc.perform(get("/api/v1/transactions/existing-merchants"))
                    .andExpect(status().isOk());
        }
    }

    /** {@code POST /api/v1/transactions} creates the transaction and returns the affected-row count, and returns a 400 for a request missing required fields. */
    @Nested
    @DisplayName("createTransaction")
    class CreateTransactionTests {
        @Test
        @DisplayName("POST should create a new transaction and return rows affected")
        void createTransaction_shouldReturnRowsAffected() throws Exception {
            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(10L)
                    .amount(new BigDecimal("100.00"))
                    .transactionDate(OffsetDateTime.now())
                    .description("Test")
                    .type("EXPENSE")
                    .build();

            when(transactionService.createTransaction(eq(USER_ID), any(TransactionCreateRequest.class))).thenReturn(1);

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("1"));
        }

        @Test
        @DisplayName("POST should return 400 when validation fails")
        void createTransaction_shouldReturn400() throws Exception {
            TransactionCreateRequest request = TransactionCreateRequest.builder().build(); // Missing required fields

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    /** {@code PATCH /api/v1/transactions/bulk} passes the request body's update list straight through to {@code TransactionService#updateTransactionsBulk} and returns its aggregated affected-row count. */
    @Nested
    @DisplayName("updateTransactionsBulk")
    class UpdateTransactionsBulkTests {
        @Test
        @DisplayName("PATCH /bulk should update multiple transactions")
        void updateTransactionsBulk_shouldReturnOk() throws Exception {
            TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                    .id(1L)
                    .accountId(1L)
                    .amount(BigDecimal.ONE)
                    .transactionDate(OffsetDateTime.now())
                    .description("T")
                    .type("EXPENSE")
                    .build();
            List<TransactionUpdateRequest> requests = List.of(request);

            when(transactionService.updateTransactionsBulk(eq(USER_ID), anyList())).thenReturn(1);

            mockMvc.perform(patch("/api/v1/transactions/bulk")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));
        }
    }

    /** {@code DELETE /api/v1/transactions/bulk} passes the request body's id list straight through to {@code TransactionService#deleteTransactions} and returns 204. */
    @Nested
    @DisplayName("deleteTransactionsBulk")
    class DeleteTransactionsBulkTests {
        @Test
        @DisplayName("DELETE /bulk should remove multiple transactions")
        void deleteTransactionsBulk_shouldReturnNoContent() throws Exception {
            List<Long> ids = List.of(1L, 2L);
            mockMvc.perform(delete("/api/v1/transactions/bulk")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ids)))
                    .andExpect(status().isNoContent());
            verify(transactionService).deleteTransactions(USER_ID, ids);
        }
    }

    /** {@code PUT /api/v1/transactions} updates the single transaction and returns the affected-row count. */
    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransactionTests {
        @Test
        @DisplayName("PUT should update transaction and return status")
        void updateTransaction_shouldReturnOk() throws Exception {
            TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                    .id(TRANSACTION_ID)
                    .accountId(1L)
                    .amount(new BigDecimal("150.00"))
                    .transactionDate(OffsetDateTime.now())
                    .description("Updated")
                    .type("EXPENSE")
                    .build();

            when(transactionService.updateTransaction(eq(USER_ID), any(TransactionUpdateRequest.class))).thenReturn(1);

            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));
        }
    }

    /** {@code DELETE /api/v1/transactions/{id}} returns 204 once {@code securityService.isTransactionOwner} authorizes the request, delegating the actual delete to {@code TransactionService#deleteTransaction}. */
    @Nested
    @DisplayName("deleteTransaction")
    class DeleteTransactionTests {
        @Test
        @DisplayName("DELETE should remove transaction")
        void deleteTransaction_shouldReturnNoContent() throws Exception {
            when(securityService.isTransactionOwner(eq(TRANSACTION_ID), any())).thenReturn(true);

            mockMvc.perform(delete("/api/v1/transactions/{id}", TRANSACTION_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
            verify(transactionService).deleteTransaction(USER_ID, TRANSACTION_ID);
        }
    }

    /** {@code DELETE} returns 404 when an authorized caller's transaction is nonetheless not found -- {@code TransactionService#deleteTransaction} throwing {@link ResourceNotFoundException} after {@code securityService.isTransactionOwner} already passed. */
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {
        @Test
        @DisplayName("DELETE should return 404 when transaction not found")
        void deleteTransaction_shouldReturn404() throws Exception {
            when(securityService.isTransactionOwner(eq(TRANSACTION_ID), any())).thenReturn(true);

            org.mockito.Mockito.doThrow(new ResourceNotFoundException("Not found"))
                    .when(transactionService).deleteTransaction(USER_ID, TRANSACTION_ID);

            mockMvc.perform(delete("/api/v1/transactions/{id}", TRANSACTION_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
