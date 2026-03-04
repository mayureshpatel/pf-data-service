package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;

    @GetMapping("/suggestions")
    public ResponseEntity<List<RecurringSuggestionDto>> getSuggestions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(recurringService.findSuggestions(userDetails.getId()));
    }

    @GetMapping
    public ResponseEntity<List<RecurringTransactionDto>> getRecurringTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(recurringService.getRecurringTransactions(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<Integer> createRecurringTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid RecurringTransactionCreateRequest request) {
        return ResponseEntity.status(201).body(recurringService.createRecurringTransaction(userDetails.getId(), request));
    }

    @PutMapping
    public ResponseEntity<Integer> updateRecurringTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid RecurringTransactionUpdateRequest request) {
        return ResponseEntity.ok(recurringService.updateRecurringTransaction(userDetails.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> deleteRecurringTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        recurringService.deleteRecurringTransaction(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
