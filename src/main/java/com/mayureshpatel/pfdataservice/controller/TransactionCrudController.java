package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.specification.TransactionSpecification.TransactionFilter;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionCrudController {

    private final TransactionService transactionService;

    @GetMapping("/suggestions/transfers")
    public ResponseEntity<List<TransferSuggestionDto>> getTransferSuggestions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(transactionService.findPotentialTransfers(userDetails.getId()));
    }

    @PostMapping("/mark-as-transfer")
    public ResponseEntity<Void> markAsTransfer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody List<Long> transactionIds) {
        transactionService.markAsTransfer(userDetails.getId(), transactionIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(sort = "date", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
            ) {

        TransactionFilter filter = new TransactionFilter(
                accountId, type, description, categoryName, vendorName, minAmount, maxAmount, startDate, endDate
        );

        return ResponseEntity.ok(transactionService.getTransactions(userDetails.getId(), filter, pageable));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid TransactionDto dto) {

        return ResponseEntity.ok(transactionService.createTransaction(userDetails.getId(), dto));
    }

    @PatchMapping("/bulk")
    public ResponseEntity<List<TransactionDto>> updateTransactionsBulk(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid List<TransactionDto> dtos) {
        return ResponseEntity.ok(transactionService.updateTransactions(userDetails.getId(), dtos));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteTransactionsBulk(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody List<Long> ids) {
        transactionService.deleteTransactions(userDetails.getId(), ids);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.isTransactionOwner(#id, principal)")
    public ResponseEntity<TransactionDto> updateTransaction(
            @PathVariable Long id,
            @RequestBody @Valid TransactionDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(transactionService.updateTransaction(userDetails.getId(), id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isTransactionOwner(#id, principal)")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        transactionService.deleteTransaction(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
