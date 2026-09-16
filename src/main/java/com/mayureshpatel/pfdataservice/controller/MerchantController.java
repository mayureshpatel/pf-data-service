package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantMergeRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantReviewClusterDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@Validated
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * Returns a page of merchants for the authenticated user (PF-320), optionally narrowed by a
     * case-insensitive search term matched against either name column -- replaces the previous
     * unbounded response, which returned the user's entire merchant history in one call
     * (PF-319). Mirrors {@code TransactionCrudController.getTransactions}'s
     * {@code Pageable}/{@code Page<T>} convention.
     *
     * @param userDetails the authenticated user
     * @param pageable    the requested page, size, and sort, defaulting to clean name ascending
     * @param search      an optional case-insensitive substring to match against clean/original name
     * @return the requested page of the user's merchants
     */
    @Operation(summary = "List merchants", description = "Returns a page of merchants for the authenticated user, optionally filtered by a search term")
    @ApiResponse(responseCode = "200", description = "Merchants returned (possibly empty)")
    @GetMapping
    public ResponseEntity<Page<MerchantDto>> getMerchants(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(sort = "cleanName") Pageable pageable,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(merchantService.getAllMerchants(userDetails.getId(), search, pageable));
    }

    /**
     * Returns a page of the authenticated user's distinct, non-blank clean names (PF-842),
     * optionally narrowed by a search term -- backs the two-level clean-name picker and the
     * grouped Merchants view's outer rows.
     *
     * @param userDetails the authenticated user
     * @param pageable    the requested page and size
     * @param search      an optional case-insensitive substring to match against clean name
     * @return the requested page of distinct clean names
     */
    @Operation(summary = "List distinct clean names", description = "Returns a page of the authenticated user's distinct, non-blank clean names, optionally filtered by a search term")
    @ApiResponse(responseCode = "200", description = "Clean names returned (possibly empty)")
    @GetMapping("/clean-names")
    public ResponseEntity<Page<String>> getDistinctCleanNames(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(merchantService.getDistinctCleanNames(userDetails.getId(), search, pageable));
    }

    /**
     * Returns every merchant sharing an exact clean name (PF-842) -- a grouped view's expanded
     * detail rows for one outer group.
     *
     * @param userDetails the authenticated user
     * @param cleanName   the exact clean name to look up
     * @return the group's member merchants
     */
    @Operation(summary = "List merchants by exact clean name", description = "Returns every merchant owned by the authenticated user sharing the given exact clean name")
    @ApiResponse(responseCode = "200", description = "Merchants returned (possibly empty)")
    @GetMapping("/by-clean-name")
    public ResponseEntity<List<MerchantDto>> getMerchantsByCleanName(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String cleanName
    ) {
        return ResponseEntity.ok(merchantService.getMerchantsByCleanName(userDetails.getId(), cleanName));
    }

    /**
     * Returns the authenticated user's merchants needing review, clustered by suggested clean
     * name (PF-842) -- resolves PF-833, since a merchant already mislabeled by the normalizer's
     * old chain-stripping bug is flagged by the exact same condition as a brand-new, never-reviewed
     * one.
     *
     * @param userDetails the authenticated user
     * @return the user's review clusters, largest first
     */
    @Operation(summary = "List merchants needing review", description = "Returns the authenticated user's merchants whose clean name is blank or differs from a fresh normalizer suggestion, clustered by that suggestion")
    @ApiResponse(responseCode = "200", description = "Review clusters returned (possibly empty)")
    @GetMapping("/needs-review")
    public ResponseEntity<List<MerchantReviewClusterDto>> getMerchantsNeedingReview(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(merchantService.getMerchantsNeedingReview(userDetails.getId()));
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

    /**
     * Updates up to 1000 merchants' clean names in a single request (PF-842) -- confirming a
     * whole review cluster in one action.
     *
     * @param userDetails the authenticated user
     * @param requests    the corrections to apply, each including its merchant id, capped at 1000
     * @return the number of merchants updated
     */
    @Operation(summary = "Bulk update merchant clean names", description = "Updates up to 1000 merchants' clean names in a single request")
    @ApiResponse(responseCode = "200", description = "Number of merchants updated returned")
    @PatchMapping("/bulk")
    public ResponseEntity<Integer> updateMerchantsBulk(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid @Size(max = 1000, message = "Cannot process more than 1000 items at once") List<MerchantUpdateRequest> requests
    ) {
        return ResponseEntity.ok(merchantService.updateMerchantsBulk(userDetails.getId(), requests));
    }

    /**
     * Merges one merchant into another: the merged-away merchant's transactions and recurring
     * transactions move to the survivor, and the merged-away record is deleted. Ownership-guarded
     * on both merchants -- a caller who owns the survivor but not the merged-away record (or vice
     * versa) is still forbidden.
     *
     * @param userDetails the authenticated user
     * @param request     which merchant survives and which gets merged away
     * @return 204 once the merge completes
     */
    @Operation(summary = "Merge two merchants", description = "Reassigns the merged-away merchant's transactions and recurring transactions to the survivor, then deletes it")
    @ApiResponse(responseCode = "204", description = "Merchants merged")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own both merchants")
    @PostMapping("/merge")
    @PreAuthorize("@ss.isMerchantOwner(#request.survivingMerchantId, principal) and @ss.isMerchantOwner(#request.mergedAwayMerchantId, principal)")
    public ResponseEntity<Void> mergeMerchants(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MerchantMergeRequest request
    ) {
        merchantService.mergeMerchants(userDetails.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
