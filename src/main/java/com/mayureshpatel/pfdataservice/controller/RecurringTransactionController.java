package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.recurring.DismissPatternRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.DismissedPatternDto;
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

    /**
     * Lists the recurring patterns the user dismissed from the review.
     *
     * @param userDetails the authenticated user
     * @return the user's dismissed patterns, newest first
     */
    @Operation(summary = "List dismissed recurring patterns", description = "Returns the patterns the authenticated user dismissed from the detected-patterns review")
    @ApiResponse(responseCode = "200", description = "Dismissed patterns returned (possibly empty)")
    @GetMapping("/dismissals")
    public ResponseEntity<List<DismissedPatternDto>> getDismissedPatterns(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(recurringService.getDismissedPatterns(userDetails.getId()));
    }

    /**
     * Dismisses a detected pattern so it is no longer suggested. Dismissing the same pattern twice
     * is harmless.
     *
     * @param userDetails the authenticated user
     * @param request     the pattern key and display name
     * @return the dismissal id
     */
    @Operation(summary = "Dismiss a recurring pattern", description = "Stops a detected pattern from being suggested until it is restored")
    @ApiResponse(responseCode = "200", description = "Pattern dismissed; the dismissal id is returned")
    @PostMapping("/dismissals")
    public ResponseEntity<Long> dismissPattern(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DismissPatternRequest request) {
        return ResponseEntity.ok(recurringService.dismissPattern(userDetails.getId(), request));
    }

    /**
     * Restores a dismissed pattern so it can be suggested again.
     *
     * @param userDetails the authenticated user
     * @param id          the dismissal id
     * @return 204 once restored
     */
    @Operation(summary = "Restore a dismissed pattern", description = "Removes a dismissal so the pattern can be suggested again")
    @ApiResponse(responseCode = "204", description = "Dismissal removed")
    @ApiResponse(responseCode = "404", description = "The dismissal does not exist or is not the user's")
    @DeleteMapping("/dismissals/{id}")
    public ResponseEntity<Void> restoreDismissedPattern(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        recurringService.restoreDismissedPattern(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
