package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/accounts", "/api/accounts"})
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAccounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(accountService.getAllAccountsByUserId(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<Integer> createAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AccountCreateRequest request) {
        return ResponseEntity.status(201).body(accountService.createAccount(userDetails.getId(), request));
    }

    @PutMapping()
    public ResponseEntity<Integer> updateAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(userDetails.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        accountService.deleteAccount(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reconcile")
    public ResponseEntity<Integer> reconcileAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody BigDecimal newBalance) {
        return ResponseEntity.ok(accountService.reconcileAccount(userDetails.getId(), id, newBalance));
    }
}
