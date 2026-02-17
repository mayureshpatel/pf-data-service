package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.RecurringTransactionDto;
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
    public ResponseEntity<RecurringTransactionDto> createRecurringTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid RecurringTransactionDto dto) {
        return ResponseEntity.ok(recurringService.createRecurringTransaction(userDetails.getId(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionDto> updateRecurringTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody @Valid RecurringTransactionDto dto) {
        return ResponseEntity.ok(recurringService.updateRecurringTransaction(userDetails.getId(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        recurringService.deleteRecurringTransaction(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
