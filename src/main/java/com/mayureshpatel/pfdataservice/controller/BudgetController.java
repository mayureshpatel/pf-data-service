package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.budget.BudgetCreateRequest;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * CRUD for the authenticated user's per-category, per-month budgets, plus
 * {@link #getBudgetStatus} for tracking allocated vs. actual spending against them.
 */
@Tag(name = "Budgets", description = "The authenticated user's budgets and their status")
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * Returns the user's budgets for a month/year, defaulting to the current month.
     *
     * @param userDetails the authenticated user
     * @param month       the budget month, defaults to the current month
     * @param year        the budget year, defaults to the current year
     * @return the budgets for that period
     */
    @Operation(summary = "List budgets for a period", description = "Returns the user's budgets for a month/year, defaulting to the current month")
    @ApiResponse(responseCode = "200", description = "Budgets returned (possibly empty)")
    @GetMapping
    public ResponseEntity<List<BudgetDto>> getBudgets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        int monthValue = (month != null) ? month : LocalDate.now().getMonthValue();
        int yearValue = (year != null) ? year : LocalDate.now().getYear();

        return ResponseEntity.ok(budgetService.getBudgets(userDetails.getId(), monthValue, yearValue));
    }

    /**
     * Returns budget status (allocated vs. spent) for a month/year, defaulting to the current
     * month.
     *
     * @param userDetails the authenticated user
     * @param month       the budget month, defaults to the current month
     * @param year        the budget year, defaults to the current year
     * @return the budget statuses for that period
     */
    @Operation(summary = "Get budget status for a period", description = "Allocated vs. spent for each budget in a month/year, defaulting to the current month")
    @ApiResponse(responseCode = "200", description = "Budget statuses returned (possibly empty)")
    @GetMapping("/status")
    public ResponseEntity<List<BudgetStatusDto>> getBudgetStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        int monthValue = (month != null) ? month : LocalDate.now().getMonthValue();
        int yearValue = (year != null) ? year : LocalDate.now().getYear();

        return ResponseEntity.ok(budgetService.getBudgetStatus(userDetails.getId(), monthValue, yearValue));
    }

    /**
     * Returns every budget the user has across all periods.
     *
     * @param userDetails the authenticated user
     * @return all of the user's budgets
     */
    @Operation(summary = "List all budgets", description = "Returns every budget the user has across all periods")
    @ApiResponse(responseCode = "200", description = "Budgets returned (possibly empty)")
    @GetMapping("/all")
    public ResponseEntity<List<BudgetDto>> getAllBudgets(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(budgetService.getAllBudgets(userDetails.getId()));
    }

    /**
     * Creates a new budget for a category and period.
     *
     * @param userDetails the authenticated user
     * @param request     the budget to create
     * @return 201 with the new budget's generated id
     */
    @Operation(summary = "Create a budget", description = "Creates a new budget for a category and period")
    @ApiResponse(responseCode = "201", description = "Budget created, id returned")
    @PostMapping
    public ResponseEntity<Integer> createBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid BudgetCreateRequest request) {
        return ResponseEntity.status(201).body(budgetService.create(userDetails.getId(), request));
    }

    /**
     * Updates an existing budget. Ownership-guarded: the budget's own id doesn't prove the
     * caller owns it, so this is checked separately from authentication.
     *
     * @param userDetails the authenticated user
     * @param request     the budget to update, including its id
     * @return the updated budget's id
     */
    @Operation(summary = "Update a budget", description = "Updates a budget owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Budget updated, id returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this budget")
    @PutMapping
    @PreAuthorize("@ss.isBudgetOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid BudgetUpdateRequest request
    ) {
        return ResponseEntity.ok(budgetService.update(userDetails.getId(), request));
    }

    /**
     * Deletes a budget by id. Ownership-guarded the same way as {@link #updateBudget}.
     *
     * @param userDetails the authenticated user
     * @param id          the budget id to delete
     * @return 204 once deleted
     */
    @Operation(summary = "Delete a budget", description = "Deletes a budget owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Budget deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this budget")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isBudgetOwner(#id, principal)")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        budgetService.delete(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
