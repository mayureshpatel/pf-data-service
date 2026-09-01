package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.account.AccountTypeCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.mapper.AccountTypeDtoMapper;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The set of account types (checking, savings, credit card, etc.) accounts can be assigned.
 * Reading the list is open to any authenticated request; creating or deleting a type is
 * admin-only, since types are shared reference data rather than per-user data.
 */
@Tag(name = "Account Types", description = "Shared reference data for account types; admin-only writes")
@RestController
@RequestMapping({"/api/v1/account-types", "/api/account-types"})
@RequiredArgsConstructor
public class AccountTypeController {

    private final AccountTypeRepository accountTypeRepository;

    /**
     * Returns every active account type, ordered for display.
     *
     * @return the active account types
     */
    @Operation(summary = "List account types", description = "Returns every active account type, in display order")
    @ApiResponse(responseCode = "200", description = "Account types returned")
    @GetMapping
    public ResponseEntity<List<AccountTypeDto>> getAccountTypes() {
        List<AccountTypeDto> types = AccountTypeDtoMapper.toDto(accountTypeRepository.findByIsActiveTrueOrderBySortOrder());
        return ResponseEntity.ok(types);
    }

    /**
     * Creates a new account type. Admin-only.
     *
     * @param request the account type to create
     * @return the number of rows inserted
     */
    @Operation(summary = "Create an account type", description = "Creates a new account type. Admin-only.")
    @ApiResponse(responseCode = "200", description = "Account type created")
    @ApiResponse(responseCode = "403", description = "Forbidden -- caller is not an admin")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> createAccountType(
            @RequestBody @Valid AccountTypeCreateRequest request) {
        return ResponseEntity.ok(accountTypeRepository.insert(request));
    }

    /**
     * Deletes an account type by its code. Admin-only.
     *
     * @param code the account type code to delete
     * @return the number of rows deleted
     */
    @Operation(summary = "Delete an account type", description = "Deletes an account type by code. Admin-only.")
    @ApiResponse(responseCode = "200", description = "Rows-deleted count returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- caller is not an admin")
    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> deleteAccountType(@PathVariable String code) {
        return ResponseEntity.ok(accountTypeRepository.deleteByCode(code));
    }
}
