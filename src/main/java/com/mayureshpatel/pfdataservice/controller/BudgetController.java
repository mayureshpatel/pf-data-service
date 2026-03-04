package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.budget.BudgetCreateRequest;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetDto>> getBudgets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        int monthValue = (month != null) ? month : LocalDate.now().getMonthValue();
        int yearValue = (year != null) ? year : LocalDate.now().getYear();
        
        return ResponseEntity.ok(budgetService.getBudgets(userDetails.getId(), monthValue, yearValue));
    }

    @GetMapping("/status")
    public ResponseEntity<List<BudgetStatusDto>> getBudgetStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        int monthValue = (month != null) ? month : LocalDate.now().getMonthValue();
        int yearValue = (year != null) ? year : LocalDate.now().getYear();
        
        return ResponseEntity.ok(budgetService.getBudgetStatus(userDetails.getId(), monthValue, yearValue));
    }

    @GetMapping("/all")
    public ResponseEntity<List<BudgetDto>> getAllBudgets(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(budgetService.getAllBudgets(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<BudgetDto> createBudget (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid BudgetCreateRequest request) {
        return ResponseEntity.status(201).body(budgetService.create(userDetails.getId(), request));
    }

    @PutMapping
    public ResponseEntity<BudgetDto> updateBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid BudgetUpdateRequest request
    ) {
        return ResponseEntity.ok(budgetService.update(userDetails.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        budgetService.delete(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
