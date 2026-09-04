package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountReconcileRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD for the authenticated user's accounts, plus {@link #reconcileAccount} for correcting an
 * account's balance against a real-world statement.
 */
@Tag(name = "Accounts", description = "The authenticated user's accounts")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Returns all accounts for the authenticated user.
     *
     * @param userDetails the authenticated user
     * @return the user's accounts
     */
    @Operation(summary = "List accounts", description = "Returns all accounts for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Accounts returned (possibly empty)")
    @GetMapping
    public ResponseEntity<List<AccountDto>> getAccounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(accountService.getAllAccountsByUserId(userDetails.getId()));
    }

    /**
     * Creates a new account.
     *
     * @param userDetails the authenticated user
     * @param request     the account to create
     * @return 201 with the new account's generated id
     */
    @Operation(summary = "Create an account", description = "Creates a new account for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Account created, id returned")
    @PostMapping
    public ResponseEntity<Integer> createAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AccountCreateRequest request) {
        return ResponseEntity.status(201).body(accountService.createAccount(userDetails.getId(), request));
    }

    /**
     * Updates an existing account. Ownership-guarded: the account's own id doesn't prove the
     * caller owns it, so this is checked separately from authentication.
     *
     * @param userDetails the authenticated user
     * @param request     the account to update, including its id
     * @return the updated account's id
     */
    @Operation(summary = "Update an account", description = "Updates an account owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Account updated, id returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this account")
    @PutMapping()
    @PreAuthorize("@ss.isAccountOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(userDetails.getId(), request));
    }

    /**
     * Deletes an account by id. Ownership-guarded the same way as {@link #updateAccount}.
     *
     * @param userDetails the authenticated user
     * @param id          the account id to delete
     * @return 204 once deleted
     */
    @Operation(summary = "Delete an account", description = "Deletes an account owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Account deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this account")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isAccountOwner(#id, principal)")
    public ResponseEntity<Integer> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        accountService.deleteAccount(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reconciles an account's balance against a real-world statement, creating an adjustment
     * transaction if they differ. Ownership-guarded on the request's account id.
     *
     * @param userDetails the authenticated user
     * @param request     the reconciliation payload
     * @return the id of the resulting adjustment transaction, if one was created
     */
    @Operation(summary = "Reconcile an account", description = "Reconciles an account's balance against a statement, creating an adjustment transaction if they differ")
    @ApiResponse(responseCode = "200", description = "Reconciled; adjustment transaction id returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this account")
    @PostMapping("/reconcile")
    @PreAuthorize("@ss.isAccountOwner(#request.accountId, principal)")
    public ResponseEntity<Integer> reconcileAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AccountReconcileRequest request) {
        return ResponseEntity.ok(accountService.reconcileAccount(userDetails.getId(), request));
    }
}
