package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.FileImportResultDto;
import com.mayureshpatel.pfdataservice.dto.transaction.ImportHashCheckRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Saves multiple already-previewed transactions across accounts in a single request. Distinct
 * from {@link TransactionController#saveTransactions}, which saves transactions for one specific
 * account: this endpoint accepts a batch that can span accounts.
 */
@Tag(name = "Transactions", description = "Querying, creating, updating, and deleting the authenticated user's transactions")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class BulkTransactionController {
    /** Maximum number of (account, hash) pairs accepted by a single import-check call. */
    private static final int MAX_IMPORT_CHECK_ITEMS = 1000;

    private final TransactionImportService transactionImportService;
    private final Validator validator;

    /**
     * Saves multiple previewed transactions in a single request.
     *
     * @param requests    the transactions to save
     * @param userDetails the authenticated user
     * @return one import result per request, in request order
     */
    @Operation(summary = "Bulk save transactions", description = "Saves multiple previewed transactions, possibly across accounts, in a single request")
    @ApiResponse(responseCode = "200", description = "Per-file import results returned; each file commits independently")
    @PostMapping("/bulk")
    public ResponseEntity<List<FileImportResultDto>> saveBulkTransactions(@RequestBody @Valid List<SaveTransactionRequest> requests,
                                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(transactionImportService.saveBulkTransactions(userDetails.getId(), requests));
    }

    /**
     * Reports which of the given (account, file hash) pairs were already imported, so the client
     * can flag a file before the user commits it.
     *
     * @param candidates  the (account, hash) pairs to check, at most 1000
     * @param userDetails the authenticated user
     * @return the subset of candidates already imported
     */
    @Operation(summary = "Check already-imported files", description = "Returns the subset of the given (account, file hash) pairs whose file was already imported")
    @ApiResponse(responseCode = "200", description = "Already-imported pairs returned; empty if none")
    @ApiResponse(responseCode = "403", description = "A candidate account belongs to another user")
    @PostMapping("/import-check")
    public ResponseEntity<List<ImportHashCheckRequest>> checkAlreadyImported(
            @RequestBody List<ImportHashCheckRequest> candidates,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // validated by hand (not via a class-level @Validated) so the existing /bulk endpoint's validation is untouched
        if (candidates == null || candidates.size() > MAX_IMPORT_CHECK_ITEMS) {
            throw new IllegalArgumentException("Between 0 and " + MAX_IMPORT_CHECK_ITEMS + " files can be checked at once");
        }
        for (ImportHashCheckRequest candidate : candidates) {
            if (candidate == null) {
                throw new IllegalArgumentException("Import check entries must not be null");
            }
            var violations = validator.validate(candidate);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
        return ResponseEntity.ok(transactionImportService.findAlreadyImported(userDetails.getId(), candidates));
    }
}
