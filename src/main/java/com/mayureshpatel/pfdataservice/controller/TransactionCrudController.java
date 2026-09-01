package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.*;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.TransactionFilter;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * REST endpoints for querying, creating, updating, and deleting transactions. All operations are
 * scoped to the authenticated user via {@link CustomUserDetails} -- no endpoint accepts a userId
 * directly from the client. Single-transaction mutations that take an id ({@link
 * #updateTransaction}, {@link #deleteTransaction}) are additionally guarded by {@code @PreAuthorize}
 * ownership checks, since a transaction's own id alone doesn't prove the caller owns it.
 */
@Tag(name = "Transactions", description = "Querying, creating, updating, and deleting the authenticated user's transactions")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionCrudController {

    private final TransactionService transactionService;

    /**
     * Finds transaction pairs that look like transfers between the user's own accounts (e.g. a
     * withdrawal from one account matching a deposit into another), for the user to confirm.
     *
     * @param userDetails the authenticated user
     * @return candidate transfer pairs, empty if none found
     */
    @Operation(summary = "Suggest potential transfers", description = "Finds transaction pairs that look like transfers between the user's own accounts")
    @ApiResponse(responseCode = "200", description = "Suggestions returned (possibly empty)")
    @GetMapping("/suggestions/transfers")
    public ResponseEntity<List<TransferSuggestionDto>> getTransferSuggestions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(transactionService.findPotentialTransfers(userDetails.getId()));
    }

    /**
     * Confirms a list of transactions as transfers (accepting a transfer suggestion), so they're
     * treated as internal movement rather than income/expense in aggregates.
     *
     * @param userDetails    the authenticated user
     * @param transactionIds the transaction ids to mark, capped at 1000 per request
     * @return 200 with no body once marked
     */
    @Operation(summary = "Mark transactions as transfers", description = "Confirms a batch of transactions as internal transfers rather than income/expense")
    @ApiResponse(responseCode = "200", description = "Transactions marked as transfers")
    @PostMapping("/mark-as-transfer")
    public ResponseEntity<Void> markAsTransfer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Size(max = 1000, message = "Cannot process more than 1000 items at once") List<Long> transactionIds) {
        transactionService.markAsTransfer(userDetails.getId(), transactionIds);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns a paginated, filtered page of the user's transactions. Every filter parameter is
     * optional; omitted ones simply aren't applied.
     *
     * @param userDetails the authenticated user
     * @param pageable    page number/size/sort, defaults to sorting by date descending
     * @param accountId   restrict to one account
     * @param type        restrict to one transaction type
     * @param description substring match on description
     * @param categoryName restrict to one category
     * @param vendorName  restrict to one merchant/vendor
     * @param minAmount   lower bound (inclusive) on amount
     * @param maxAmount   upper bound (inclusive) on amount
     * @param startDate   lower bound (inclusive) on transaction date
     * @param endDate     upper bound (inclusive) on transaction date
     * @return the matching page of transactions
     */
    @Operation(summary = "List transactions", description = "Paginated, filtered list of the authenticated user's transactions")
    @ApiResponse(responseCode = "200", description = "Page of matching transactions returned")
    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(sort = "date", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {

        TransactionFilter filter = new TransactionFilter(
                accountId, type, description, categoryName, vendorName, minAmount, maxAmount, startDate, endDate
        );

        return ResponseEntity.ok(transactionService.getTransactions(userDetails.getId(), filter, pageable));
    }

    /**
     * Returns the user's transaction count grouped by category, for dashboard/summary views.
     *
     * @param userDetails the authenticated user
     * @return per-category transaction counts
     */
    @Operation(summary = "Count transactions by category", description = "Transaction counts grouped by category, for summary/dashboard views")
    @ApiResponse(responseCode = "200", description = "Counts returned (possibly empty)")
    @GetMapping("/count-by-category")
    public ResponseEntity<List<CategoryTransactionsDto>> getCountByCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(transactionService.getCountByCategory(userDetails.getId()));
    }

    /**
     * Returns only the categories that have at least one transaction, for populating filter
     * dropdowns without offering categories that would always return an empty result.
     *
     * @param userDetails the authenticated user
     * @return categories with at least one transaction
     */
    @Operation(summary = "List categories with transactions", description = "Categories that have at least one transaction, for filter dropdowns")
    @ApiResponse(responseCode = "200", description = "Categories returned (possibly empty)")
    @GetMapping("/existing-categories")
    public ResponseEntity<List<CategoryDto>> getAllCategoriesWithTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(transactionService.getCategoriesWithTransactions(userDetails.getId()));
    }

    /**
     * Returns only the merchants that have at least one transaction, for the same reason as
     * {@link #getAllCategoriesWithTransactions}.
     *
     * @param userDetails the authenticated user
     * @return merchants with at least one transaction
     */
    @Operation(summary = "List merchants with transactions", description = "Merchants that have at least one transaction, for filter dropdowns")
    @ApiResponse(responseCode = "200", description = "Merchants returned (possibly empty)")
    @GetMapping("/existing-merchants")
    public ResponseEntity<List<MerchantDto>> getAllMerchantsWithTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(transactionService.getMerchantsWithTransactions(userDetails.getId()));
    }

    /**
     * Creates a new transaction for the authenticated user.
     *
     * @param userDetails the authenticated user
     * @param request     the transaction to create
     * @return 201 with the new transaction's generated id
     */
    @Operation(summary = "Create a transaction", description = "Creates a new transaction for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Transaction created, id returned")
    @PostMapping
    public ResponseEntity<Integer> createTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid TransactionCreateRequest request) {

        return ResponseEntity.status(201).body(transactionService.createTransaction(userDetails.getId(), request));
    }

    /**
     * Updates up to 1000 transactions in a single request.
     *
     * @param userDetails the authenticated user
     * @param requests    the transactions to update, each including its id, capped at 1000 per request
     * @return the number of transactions updated
     */
    @Operation(summary = "Bulk update transactions", description = "Updates up to 1000 transactions in a single request")
    @ApiResponse(responseCode = "200", description = "Number of transactions updated returned")
    @ApiResponse(responseCode = "404", description = "One or more transaction ids not found")
    @PatchMapping("/bulk")
    public ResponseEntity<Integer> updateTransactionsBulk(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid @Size(max = 1000, message = "Cannot process more than 1000 items at once") List<TransactionUpdateRequest> requests) {
        return ResponseEntity.ok(transactionService.updateTransactionsBulk(userDetails.getId(), requests));
    }

    /**
     * Deletes up to 1000 transactions in a single request.
     *
     * @param userDetails the authenticated user
     * @param ids         the transaction ids to delete, capped at 1000 per request
     * @return 204 once deleted
     */
    @Operation(summary = "Bulk delete transactions", description = "Deletes up to 1000 transactions in a single request")
    @ApiResponse(responseCode = "204", description = "Transactions deleted")
    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteTransactionsBulk(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Size(max = 1000, message = "Cannot process more than 1000 items at once") List<Long> ids) {
        transactionService.deleteTransactions(userDetails.getId(), ids);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates a single transaction. Ownership is enforced separately from authentication: the
     * transaction id alone doesn't prove the caller owns it, so this is guarded by
     * {@code @PreAuthorize} in addition to scoping by {@code userDetails}.
     *
     * @param userDetails the authenticated user
     * @param request     the transaction to update, including its id
     * @return the number of rows updated (0 or 1)
     */
    @Operation(summary = "Update a transaction", description = "Updates a single transaction owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Update applied, rows-affected count returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this transaction")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @PutMapping
    @PreAuthorize("@ss.isTransactionOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid TransactionUpdateRequest request) {

        return ResponseEntity.ok(transactionService.updateTransaction(userDetails.getId(), request));
    }

    /**
     * Deletes a single transaction by id. Ownership-guarded the same way as
     * {@link #updateTransaction}.
     *
     * @param id          the transaction id to delete
     * @param userDetails the authenticated user
     * @return 204 once deleted
     */
    @Operation(summary = "Delete a transaction", description = "Deletes a single transaction owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Transaction deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this transaction")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isTransactionOwner(#id, principal)")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        transactionService.deleteTransaction(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
