package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.account.AccountTypeCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.mapper.AccountTypeDtoMapper;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/account-types", "/api/account-types"})
@RequiredArgsConstructor
public class AccountTypeController {

    private final AccountTypeRepository accountTypeRepository;

    @GetMapping
    public ResponseEntity<List<AccountTypeDto>> getAccountTypes() {
        List<AccountTypeDto> types = AccountTypeDtoMapper.toDto(accountTypeRepository.findByIsActiveTrueOrderBySortOrder());
        return ResponseEntity.ok(types);
    }

    @PostMapping
    public ResponseEntity<Integer> createAccountType(
            @RequestBody @Valid AccountTypeCreateRequest request) {
        return ResponseEntity.ok(accountTypeRepository.insert(request));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Integer> deleteAccountType(@PathVariable String code) {
        return ResponseEntity.ok(accountTypeRepository.deleteByCode(code));
    }

}
