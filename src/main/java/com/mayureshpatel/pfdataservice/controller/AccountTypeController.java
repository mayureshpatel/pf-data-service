package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.account.AccountTypeLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/account-types", "/api/account-types"})
@RequiredArgsConstructor
public class AccountTypeController {

    private final AccountTypeLookupRepository accountTypeLookupRepository;

    @GetMapping
    public ResponseEntity<List<AccountTypeLookup>> getAccountTypes() {
        List<AccountTypeLookup> types = accountTypeLookupRepository.findByIsActiveTrueOrderBySortOrder();
        return ResponseEntity.ok(types);
    }
}
