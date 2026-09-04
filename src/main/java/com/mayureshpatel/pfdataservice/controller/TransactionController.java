package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionPreviewDto;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * CSV import for a single account: {@link #uploadTransactions} parses a bank statement file into
 * a preview without saving it, and {@link #saveTransactions} commits a previously previewed batch.
 * Both endpoints are ownership-guarded on the {@code accountId} path variable.
 */
@Tag(name = "Transaction Import", description = "CSV upload, preview, and save for a single account")
@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionImportService transactionImportService;

    /**
     * Parses an uploaded CSV file in the given bank's format into a preview of the transactions
     * it contains, without saving them.
     *
     * @param accountId   the account to associate the import with
     * @param file        the CSV file to parse
     * @param bankName    the bank format to parse the file as
     * @param userDetails the authenticated user
     * @return the previewed transactions
     * @throws IOException if the file can't be read
     */
    @Operation(summary = "Preview a CSV upload", description = "Parses an uploaded CSV file into a preview of its transactions, without saving them")
    @ApiResponse(responseCode = "200", description = "Preview returned")
    @ApiResponse(responseCode = "400", description = "File is empty or malformed")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this account")
    @PostMapping("/upload")
    @PreAuthorize("@ss.isAccountOwner(#accountId, principal)")
    public ResponseEntity<List<TransactionPreviewDto>> uploadTransactions(
            @PathVariable Long accountId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("bankName") String bankName,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        try (InputStream inputStream = file.getInputStream()) {
            String fileName = file.getOriginalFilename();
            List<TransactionPreviewDto> preview = transactionImportService.previewTransactions(
                    userDetails.getId(), accountId, bankName, inputStream, fileName);
            return ResponseEntity.ok(preview);
        }
    }

    /**
     * Saves a previously previewed batch of transactions to this account.
     *
     * @param accountId   the account to save the transactions to
     * @param request     the previewed transactions, source filename, and file hash (for
     *                    duplicate-import detection)
     * @param userDetails the authenticated user
     * @return a confirmation message including how many were saved
     */
    @Operation(summary = "Save previewed transactions", description = "Saves a previously previewed batch of transactions to this account")
    @ApiResponse(responseCode = "200", description = "Transactions saved, confirmation message returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this account")
    @PostMapping("/transactions")
    @PreAuthorize("@ss.isAccountOwner(#accountId, principal)")
    public ResponseEntity<String> saveTransactions(
            @PathVariable Long accountId,
            @RequestBody @Valid SaveTransactionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int count = transactionImportService.saveTransactions(
                userDetails.getId(),
                accountId,
                request.transactions(),
                request.fileName(),
                request.fileHash()
        );

        return ResponseEntity.ok("Successfully saved " + count + " transactions.");
    }
}
