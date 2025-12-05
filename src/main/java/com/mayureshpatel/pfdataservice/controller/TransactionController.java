package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
            @RequestParam("bankName") String bankName
    ) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        byte[] fileContent = file.getBytes();
        String fileName = file.getOriginalFilename();

        List<TransactionPreview> preview = transactionImportService.previewTransactions(accountId, bankName, fileContent, fileName);

        return ResponseEntity.ok(preview);
    }

    @PostMapping("/transactions")
    public ResponseEntity<String> saveTransactions(
            @PathVariable Long accountId,
            @RequestBody @Valid SaveTransactionRequest request
    ) {
        int count = transactionImportService.saveTransactions(
                accountId,
                request.getTransactions(),
                request.getFileName(),
                request.getFileHash()
        );

        return ResponseEntity.ok("Successfully saved " + count + " transactions.");
    }
}
