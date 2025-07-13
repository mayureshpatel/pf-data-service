package com.mayureshpatel.pfdataservice.web.controller;

import com.mayureshpatel.pfdataservice.application.mapper.BudgetMapper;
import com.mayureshpatel.pfdataservice.domain.Budget;
import com.mayureshpatel.pfdataservice.service.BudgetService;
import com.mayureshpatel.pfdataservice.web.dto.request.CreateBudgetRequest;
import com.mayureshpatel.pfdataservice.web.dto.response.BudgetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetMapper budgetMapper;

    @PostMapping
    public ResponseEntity<BudgetResponse> create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateBudgetRequest body) {

        Budget created = budgetService.create(body, userId);
        return ResponseEntity
                .created(URI.create("/api/v1/budgets/" + created.getId()))
                .body(budgetMapper.toResponse(created));
    }

    /** GET /​budgets/{yyyy}/{mm} – retrieve */
    @GetMapping("/{year}/{month}")
    public BudgetResponse get(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable int year,
            @PathVariable int month) {

        Budget b = budgetService.fetch(userId, YearMonth.of(year, month));
        return budgetMapper.toResponse(b);
    }

    /* ------------ Optional error mapping ------------- */
    @ExceptionHandler({
            com.mayureshpatel.pfdataservice.service.exception.DuplicateBudgetException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    void conflict() { /* 409 */ }

    @ExceptionHandler({
            com.mayureshpatel.pfdataservice.service.exception.BudgetNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void notFound() { /* 404 */ }
}
