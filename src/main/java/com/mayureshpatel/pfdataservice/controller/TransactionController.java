package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<String> saveTransactions(
            @PathVariable Long accountId,
            @RequestBody @Valid SaveTransactionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Transaction> transactions = request.getTransactions().stream()
                .map(this::mapToEntity)
                .toList();

        int count = transactionImportService.saveTransactions(
                userDetails.getId(),
                accountId,
                transactions,
                request.getFileName(),
                request.getFileHash()
        );

        return ResponseEntity.ok("Successfully saved " + count + " transactions.");
    }

    private Transaction mapToEntity(TransactionDto dto) {
        Transaction transaction = new Transaction();
        transaction.setDate(dto.getDate());
        transaction.setDescription(dto.getDescription());
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        return transaction;
    }
}