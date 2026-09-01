package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final TransactionImportService transactionImportService;

    /**
     * Saves multiple previewed transactions in a single request.
     *
     * @param requests    the transactions to save
     * @param userDetails the authenticated user
     * @return a confirmation message including how many were saved
     */
    @Operation(summary = "Bulk save transactions", description = "Saves multiple previewed transactions, possibly across accounts, in a single request")
    @ApiResponse(responseCode = "200", description = "Transactions saved, confirmation message returned")
    @PostMapping("/bulk")
    public ResponseEntity<String> saveBulkTransactions(@RequestBody @Valid List<SaveTransactionRequest> requests,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        int count = transactionImportService.saveBulkTransactions(userDetails.getId(), requests);
        return ResponseEntity.ok("Successfully saved " + count + " transactions.");
    }
}
