package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkUndoResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkBulkCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantSuggestionRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.DescriptionLinkService;
import com.mayureshpatel.pfdataservice.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint for the authenticated user's merchants -- vendors the user deliberately names and
 * manages directly (create, edit, delete), never auto-created from a transaction description.
 * Also manages each merchant's description links: the raw transaction descriptions that
 * auto-match to it on import.
 */
@Tag(name = "Merchants", description = "The authenticated user's merchants")
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@Validated
public class MerchantController {

    /** How many merchants a suggestion request gets per name when it does not say. */
    private static final int DEFAULT_SUGGESTION_LIMIT = 5;

    private final MerchantService merchantService;
    private final DescriptionLinkService descriptionLinkService;

    /**
     * Returns a page of merchants for the authenticated user (PF-320), optionally narrowed by a
     * case-insensitive search term matched against name or city.
     *
     * @param userDetails the authenticated user
     * @param pageable    the requested page, size, and sort, defaulting to name ascending
     * @param search      an optional case-insensitive substring to match against name/city
     * @return the requested page of the user's merchants
     */
    @Operation(summary = "List merchants", description = "Returns a page of merchants for the authenticated user, optionally filtered by a search term")
    @ApiResponse(responseCode = "200", description = "Merchants returned (possibly empty)")
    @GetMapping
    public ResponseEntity<Page<MerchantDto>> getMerchants(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(sort = "name") Pageable pageable,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(merchantService.getAllMerchants(userDetails.getId(), search, pageable));
    }

    /**
     * Suggests, for each description group name, the authenticated user's merchants whose names
     * fit it best (PF-982): the Unlinked tab sends a page of group names in one call so moving to
     * the next group never waits on a request. Read-only despite the POST; see
     * {@link MerchantSuggestionRequest} for why it takes a body.
     *
     * @param userDetails the authenticated user
     * @param request     the group names and the most merchants per name
     * @return one entry per name, in order, each with up to {@code limit} merchants, best first
     */
    @Operation(summary = "Suggest merchants for group names", description = "Read-only. For each description group name, the authenticated user's merchants whose names share a word with it, best fit first")
    @ApiResponse(responseCode = "200", description = "Suggestions returned, one entry per name in order")
    @ApiResponse(responseCode = "400", description = "More than 100 names, a null or too-long name, or a limit outside 1 to 20")
    @PostMapping("/suggestions")
    public ResponseEntity<List<MerchantSuggestionDto>> suggestMerchants(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MerchantSuggestionRequest request
    ) {
        int limit = request.limit() != null ? request.limit() : DEFAULT_SUGGESTION_LIMIT;
        return ResponseEntity.ok(merchantService.suggestMerchants(userDetails.getId(), request.names(), limit));
    }

    /**
     * Creates a new merchant owned by the authenticated user.
     *
     * @param userDetails the authenticated user
     * @param request     the merchant's name and optional location
     * @return the new merchant's generated id
     */
    @Operation(summary = "Create a merchant", description = "Creates a new merchant owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Merchant created, new id returned")
    @PostMapping
    public ResponseEntity<Long> createMerchant(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MerchantCreateRequest request
    ) {
        return ResponseEntity.ok(merchantService.createMerchant(userDetails.getId(), request));
    }

    /**
     * Updates a merchant's name and location. Ownership-guarded: the merchant's own id doesn't
     * prove the caller owns it, so this is checked separately from authentication -- the same
     * pattern as {@code AccountController#updateAccount}.
     *
     * @param userDetails the authenticated user
     * @param request     the merchant's new name and location fields, including its id
     * @return the number of rows updated
     */
    @Operation(summary = "Update a merchant", description = "Updates the name and location of a merchant owned by the authenticated user")
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
     * Deletes a merchant owned by the authenticated user. Dependent transactions are left with a
     * blank merchant rather than erroring; the merchant's description links are removed with it.
     *
     * @param userDetails the authenticated user
     * @param id          the merchant id to delete
     * @return 204 once the merchant is deleted
     */
    @Operation(summary = "Delete a merchant", description = "Deletes a merchant owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Merchant deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this merchant")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isMerchantOwner(#id, principal)")
    public ResponseEntity<Void> deleteMerchant(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        merchantService.deleteMerchant(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns every description linked to a merchant owned by the authenticated user.
     *
     * @param userDetails the authenticated user
     * @param id          the merchant id
     * @return the merchant's linked descriptions
     */
    @Operation(summary = "List a merchant's description links", description = "Returns every raw transaction description linked to a merchant owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Description links returned (possibly empty)")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this merchant")
    @GetMapping("/{id}/description-links")
    @PreAuthorize("@ss.isMerchantOwner(#id, principal)")
    public ResponseEntity<List<MerchantDescriptionLinkDto>> getDescriptionLinks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(merchantService.getDescriptionLinks(userDetails.getId(), id));
    }

    /**
     * Explicitly links a raw description to a merchant owned by the authenticated user --
     * overwrites any existing link for that description (last-write-wins), and gives the merchant
     * to the user's existing transactions with that description that have no merchant yet (PF-980;
     * a merchant already set is never overwritten). The explicit half of the auto-capture +
     * explicit management design.
     *
     * @param userDetails the authenticated user
     * @param id          the merchant id to link the description to
     * @param request     the raw description to link
     * @return 200 with what changed, which {@link #undoDescriptionLinks} takes back to reverse it
     */
    @Operation(summary = "Link a description to a merchant", description = "Creates or overwrites the link from a raw description to a merchant owned by the authenticated user, and fills that merchant in on existing transactions with the description and no merchant")
    @ApiResponse(responseCode = "200", description = "Description link recorded; the body lists what changed for undo")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this merchant")
    @PostMapping("/{id}/description-links")
    @PreAuthorize("@ss.isMerchantOwner(#id, principal)")
    public ResponseEntity<DescriptionLinkResultDto> addDescriptionLink(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody @Valid MerchantDescriptionLinkCreateRequest request
    ) {
        return ResponseEntity.ok(descriptionLinkService.linkDescriptions(userDetails.getId(), id, List.of(request.description())));
    }

    /**
     * Links many raw descriptions to a merchant owned by the authenticated user in one call --
     * the batch form of {@link #addDescriptionLink}, with the same last-write-wins behavior for a
     * description already linked elsewhere and the same fill of transactions with no merchant.
     * All-or-nothing: one transaction.
     *
     * @param userDetails the authenticated user
     * @param id          the merchant id to link the descriptions to
     * @param request     the raw descriptions to link (1 to 500)
     * @return 200 with what changed, which {@link #undoDescriptionLinks} takes back to reverse it
     */
    @Operation(summary = "Link many descriptions to a merchant", description = "Creates or overwrites the links from up to 500 raw descriptions to a merchant owned by the authenticated user, and fills that merchant in on existing transactions with those descriptions and no merchant, in one transaction")
    @ApiResponse(responseCode = "200", description = "Description links recorded; the body lists what changed for undo")
    @ApiResponse(responseCode = "400", description = "No descriptions, more than 500, or a blank or too-long description")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this merchant")
    @PostMapping("/{id}/description-links/bulk")
    @PreAuthorize("@ss.isMerchantOwner(#id, principal)")
    public ResponseEntity<DescriptionLinkResultDto> addDescriptionLinks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody @Valid MerchantDescriptionLinkBulkCreateRequest request
    ) {
        return ResponseEntity.ok(descriptionLinkService.linkDescriptions(userDetails.getId(), id, request.descriptions()));
    }

    /**
     * Reverses a link request from the result it returned (stateless, like the rule-application
     * undo): removes the links it created, moves the links it moved back to their previous merchant,
     * and clears the merchant again on the transactions it filled -- only where nothing has changed
     * since; everything else is reported as skipped.
     *
     * @param userDetails the authenticated user
     * @param id          the merchant id the descriptions were linked to
     * @param result      the link result, as returned by either link endpoint
     * @return 200 with what was put back and what was skipped
     * @throws IllegalArgumentException when the result belongs to a different merchant than the path
     */
    @Operation(summary = "Undo a description link", description = "Reverses a link request from the result it returned, leaving anything changed since")
    @ApiResponse(responseCode = "200", description = "Undo applied; the body says what was restored and what was skipped")
    @ApiResponse(responseCode = "400", description = "The result is malformed, too large, or for a different merchant")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this merchant")
    @PostMapping("/{id}/description-links/undo")
    @PreAuthorize("@ss.isMerchantOwner(#id, principal)")
    public ResponseEntity<DescriptionLinkUndoResultDto> undoDescriptionLinks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody @Valid DescriptionLinkResultDto result
    ) {
        if (!id.equals(result.merchantId())) {
            throw new IllegalArgumentException("The link result is for a different merchant.");
        }
        return ResponseEntity.ok(descriptionLinkService.undoLink(userDetails.getId(), result));
    }

    /**
     * Removes a single description link owned by the authenticated user. Never touches
     * transactions already assigned that merchant -- only affects future matching.
     *
     * @param userDetails the authenticated user
     * @param id          the merchant id the link belongs to
     * @param linkId      the link id to delete
     * @return 204 once the link is deleted
     */
    @Operation(summary = "Delete a description link", description = "Removes a single description link owned by the authenticated user; does not affect transactions already assigned that merchant")
    @ApiResponse(responseCode = "204", description = "Description link deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this merchant")
    @DeleteMapping("/{id}/description-links/{linkId}")
    @PreAuthorize("@ss.isMerchantOwner(#id, principal)")
    public ResponseEntity<Void> deleteDescriptionLink(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @PathVariable Long linkId
    ) {
        merchantService.deleteDescriptionLink(userDetails.getId(), linkId);
        return ResponseEntity.noContent().build();
    }
}
