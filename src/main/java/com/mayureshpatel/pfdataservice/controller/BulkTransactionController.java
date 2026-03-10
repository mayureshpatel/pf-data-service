package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class BulkTransactionController {
    private final TransactionImportService transactionImportService;

    @PostMapping("/bulk")
    public ResponseEntity<String> saveBulkTransactions(@RequestBody @Valid List<SaveTransactionRequest> requests,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        int count = transactionImportService.saveBulkTransactions(userDetails.getId(), requests);
        return ResponseEntity.ok("Successfully saved " + count + " transactions.");
    }
}