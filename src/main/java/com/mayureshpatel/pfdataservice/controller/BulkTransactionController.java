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
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class BulkTransactionController {
    private final TransactionImportService transactionImportService;

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
            @RequestBody @Valid @Size(max = 1000, message = "At most 1000 files can be checked at once") List<@Valid ImportHashCheckRequest> candidates,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(transactionImportService.findAlreadyImported(userDetails.getId(), candidates));
    }
}
