package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionPreview;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionImportService transactionImportService;

    @PostMapping("/upload")
    @PreAuthorize("@ss.isAccountOwner(#accountId, principal)")
    public ResponseEntity<List<TransactionPreview>> uploadTransactions(
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
            List<TransactionPreview> preview = transactionImportService.previewTransactions(
                    userDetails.getId(), accountId, bankName, inputStream, fileName);
            return ResponseEntity.ok(preview);
        }
    }

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