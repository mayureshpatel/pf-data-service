package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint for the authenticated user's merchants (the cleaned-up, deduplicated counterparties
 * transactions are matched to during import).
 */
@Tag(name = "Merchants", description = "The authenticated user's merchants")
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * Returns all merchants for the authenticated user.
     *
     * @param userDetails the authenticated user
     * @return the user's merchants
     */
    @Operation(summary = "List merchants", description = "Returns all merchants for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Merchants returned (possibly empty)")
    @GetMapping
    public ResponseEntity<List<MerchantDto>> getMerchants(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(merchantService.getAllMerchants(userDetails.getId()));
    }

    /**
     * Manually corrects a merchant's display name. Ownership-guarded: the merchant's own id
     * doesn't prove the caller owns it (and global, unowned merchants exist), so this is checked
     * separately from authentication -- the same pattern as {@code AccountController#updateAccount}.
     *
     * @param userDetails the authenticated user
     * @param request     the correction: the merchant id and its new clean name
     * @return the number of rows updated
     */
    @Operation(summary = "Correct a merchant's name", description = "Updates the display name of a merchant owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Merchant updated, rows-affected count returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this merchant")
    @PutMapping
    @PreAuthorize("@ss.isMerchantOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateMerchant(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MerchantUpdateRequest request
    ) {
        return ResponseEntity.ok(merchantService.updateMerchant(userDetails.getId(), request));
    }
}
