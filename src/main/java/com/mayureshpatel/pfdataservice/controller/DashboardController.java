package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.DashboardData;
import com.mayureshpatel.pfdataservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<DashboardData> getDashboardData(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "1") Long userId) {

        return ResponseEntity.ok(this.transactionService.getDashboardData(userId, month, year));
    }
}
