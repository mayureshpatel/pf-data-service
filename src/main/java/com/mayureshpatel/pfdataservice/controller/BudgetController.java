package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.BudgetStatusDto;
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
        
        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        int y = (year != null) ? year : LocalDate.now().getYear();
        
        return ResponseEntity.ok(budgetService.getBudgets(userDetails.getId(), m, y));
    }

    @GetMapping("/status")
    public ResponseEntity<List<BudgetStatusDto>> getBudgetStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        int y = (year != null) ? year : LocalDate.now().getYear();
        
        return ResponseEntity.ok(budgetService.getBudgetStatus(userDetails.getId(), m, y));
    }

    @GetMapping("/all")
    public ResponseEntity<List<BudgetDto>> getAllBudgets(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(budgetService.getAllBudgets(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<BudgetDto> setBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid BudgetDto dto) {
        return ResponseEntity.ok(budgetService.setBudget(userDetails.getId(), dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        budgetService.deleteBudget(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
