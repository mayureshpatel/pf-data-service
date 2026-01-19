package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.AccountDto;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/accounts", "/api/accounts"})
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAccounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AccountDto accountDto) {
        return ResponseEntity.ok(accountService.createAccount(userDetails.getId(), accountDto));
    }
}
