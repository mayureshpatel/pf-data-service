package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.RecurringTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Recurring transaction rules the user has confirmed (e.g. a monthly subscription), plus
 * {@link #getSuggestions}, which detects candidate recurring patterns in the user's transaction
 * history for them to confirm or dismiss.
 */
@Tag(name = "Recurring Transactions", description = "Confirmed recurring transactions and detected recurrence suggestions")
@RestController
@RequestMapping("/api/v1/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;

    /**
     * Detects candidate recurring transaction patterns in the user's history that haven't been
     * confirmed yet.
     *
     * @param userDetails the authenticated user
     * @return the detected suggestions
     */
    @Operation(summary = "Detect recurring suggestions", description = "Detects candidate recurring transaction patterns not yet confirmed")
    @ApiResponse(responseCode = "200", description = "Suggestions returned (possibly empty)")
    @GetMapping("/suggestions")
    public ResponseEntity<List<RecurringSuggestionDto>> getSuggestions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(recurringService.findSuggestions(userDetails.getId()));
    }

    /**
     * Returns the user's confirmed recurring transactions.
     *
     * @param userDetails the authenticated user
     * @return the user's recurring transactions
     */
    @Operation(summary = "List recurring transactions", description = "Returns the authenticated user's confirmed recurring transactions")
    @ApiResponse(responseCode = "200", description = "Recurring transactions returned (possibly empty)")
    @GetMapping
    public ResponseEntity<List<RecurringTransactionDto>> getRecurringTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(recurringService.getRecurringTransactions(userDetails.getId()));
    }

    /**
     * Confirms a new recurring transaction, typically from a suggestion.
     *
     * @param userDetails the authenticated user
     * @param request     the recurring transaction to create
     * @return 201 with the new recurring transaction's generated id
     */
    @Operation(summary = "Create a recurring transaction", description = "Confirms a new recurring transaction, typically accepted from a suggestion")
    @ApiResponse(responseCode = "201", description = "Recurring transaction created, id returned")
    @PostMapping
    public ResponseEntity<Integer> createRecurringTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid RecurringTransactionCreateRequest request) {
        return ResponseEntity.status(201).body(recurringService.createRecurringTransaction(userDetails.getId(), request));
    }

    /**
     * Updates an existing recurring transaction. Ownership-guarded: the record's own id doesn't
     * prove the caller owns it, so this is checked separately from authentication.
     *
     * @param userDetails the authenticated user
     * @param request     the recurring transaction to update, including its id
     * @return the updated record's id
     */
    @Operation(summary = "Update a recurring transaction", description = "Updates a recurring transaction owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Recurring transaction updated, id returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this recurring transaction")
    @PutMapping
    @PreAuthorize("@ss.isRecurringTransactionOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateRecurringTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid RecurringTransactionUpdateRequest request) {
        return ResponseEntity.ok(recurringService.updateRecurringTransaction(userDetails.getId(), request));
    }

    /**
     * Deletes a recurring transaction by id. Ownership-guarded the same way as
     * {@link #updateRecurringTransaction}.
     *
     * @param userDetails the authenticated user
     * @param id          the recurring transaction id to delete
     * @return 204 once deleted
     */
    @Operation(summary = "Delete a recurring transaction", description = "Deletes a recurring transaction owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Recurring transaction deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this recurring transaction")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isRecurringTransactionOwner(#id, principal)")
    public ResponseEntity<Integer> deleteRecurringTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        recurringService.deleteRecurringTransaction(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
