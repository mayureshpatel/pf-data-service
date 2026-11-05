package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.MerchantRuleApplyService;
import com.mayureshpatel.pfdataservice.service.MerchantRuleMatchPreviewService;
import com.mayureshpatel.pfdataservice.service.MerchantRuleService;
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
 * Rules that point keywords at one of the user's merchants (PF-904), so a chain can be
 * recognized from any store's description. This controller covers storage only: list, create,
 * update and delete, plus the read-only match preview of a draft rule (PF-905), and previewing and
 * applying the saved rules to existing transactions (PF-907).
 */
@Tag(name = "Merchant Rules", description = "Keyword rules that point at a merchant")
@RestController
@RequestMapping("/api/v1/merchant-rules")
@RequiredArgsConstructor
public class MerchantRuleController {

    private final MerchantRuleService merchantRuleService;
    private final MerchantRuleMatchPreviewService merchantRuleMatchPreviewService;
    private final MerchantRuleApplyService merchantRuleApplyService;

    /**
     * Returns the authenticated user's merchant rules, in rule priority order.
     *
     * @param userDetails the authenticated user
     * @return the user's rules
     */
    @Operation(summary = "List merchant rules", description = "Returns the authenticated user's merchant rules in priority order")
    @ApiResponse(responseCode = "200", description = "Rules returned (possibly empty)")
    @GetMapping
    public ResponseEntity<List<MerchantRuleDto>> getRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(merchantRuleService.getRules(userDetails.getId()));
    }

    /**
     * Creates a new merchant rule.
     *
     * @param userDetails the authenticated user
     * @param request     the rule to create
     * @return the new rule's generated id
     */
    @Operation(summary = "Create a merchant rule", description = "Creates a new merchant rule for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Rule created, id returned")
    @ApiResponse(responseCode = "400", description = "Validation failed, for example a blank keyword or more than 20 keywords")
    @ApiResponse(responseCode = "404", description = "The merchant does not exist or is not the authenticated user's")
    @PostMapping
    public ResponseEntity<Long> createRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MerchantRuleCreateRequest request) {
        return ResponseEntity.ok(merchantRuleService.createRule(userDetails.getId(), request));
    }

    /**
     * Updates an existing merchant rule. Ownership-guarded: the rule's own id doesn't prove the
     * caller owns it, so this is checked separately from authentication.
     *
     * @param userDetails the authenticated user
     * @param request     the rule to update, including its id
     * @return the number of rows updated
     */
    @Operation(summary = "Update a merchant rule", description = "Updates a merchant rule owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Rule updated, rows affected returned")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this rule")
    @ApiResponse(responseCode = "404", description = "The merchant does not exist or is not the authenticated user's")
    @PutMapping
    @PreAuthorize("@ss.isMerchantRuleOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MerchantRuleUpdateRequest request) {
        return ResponseEntity.ok(merchantRuleService.updateRule(userDetails.getId(), request));
    }

    /**
     * Deletes a merchant rule by id. Ownership-guarded the same way as {@link #updateRule}.
     *
     * @param userDetails the authenticated user
     * @param id          the rule id to delete
     * @return 204 once deleted
     */
    @Operation(summary = "Delete a merchant rule", description = "Deletes a merchant rule owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Rule deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this rule")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isMerchantRuleOwner(#id, principal)")
    public ResponseEntity<Void> deleteRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        merchantRuleService.deleteRule(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Previews which of the user's transactions a draft merchant rule would match and what would
     * happen to each, without saving or changing anything. A POST because the draft travels in
     * the body.
     *
     * @param userDetails the authenticated user
     * @param request     the draft rule; {@code ruleId} is set when editing a saved rule
     * @return the match counts and up to 50 matching rows
     */
    @Operation(summary = "Preview a draft merchant rule's matches", description = "Shows which transactions a draft merchant rule would match and whether it would assign a merchant, lose to another rule, or leave the transaction alone; read-only")
    @ApiResponse(responseCode = "200", description = "Preview returned (possibly empty)")
    @ApiResponse(responseCode = "400", description = "Validation failed, for example a blank keyword or more than 20 keywords")
    @ApiResponse(responseCode = "404", description = "ruleId or merchantId is not the authenticated user's")
    @PostMapping("/match-preview")
    public ResponseEntity<MerchantRuleMatchPreviewDto> matchPreview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MerchantRuleMatchPreviewRequest request) {
        return ResponseEntity.ok(merchantRuleMatchPreviewService.preview(userDetails.getId(), request));
    }

    /**
     * Lists the changes applying the user's merchant rules would make to transactions that have
     * no merchant, without changing anything.
     *
     * @param userDetails the authenticated user
     * @return the transactions that would get a merchant, and the rule that decided each
     */
    @Operation(summary = "Preview applying merchant rules", description = "Lists the merchant-less transactions the current rules would give a merchant, and the rule that decided each; read-only")
    @ApiResponse(responseCode = "200", description = "Preview returned (possibly empty)")
    @GetMapping("/preview")
    public ResponseEntity<List<MerchantRuleChangePreviewDto>> previewApply(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(merchantRuleApplyService.preview(userDetails.getId()));
    }

    /**
     * Applies the user's merchant rules to transactions that have no merchant, in one batch.
     * Transactions that already have a merchant or a linked description are never changed.
     *
     * @param userDetails the authenticated user
     * @return the number of transactions that got a merchant
     */
    @Operation(summary = "Apply merchant rules", description = "Gives a merchant to the merchant-less transactions the current rules match and returns how many changed; never touches a transaction that already has a merchant")
    @ApiResponse(responseCode = "200", description = "Rules applied, number of changed transactions returned")
    @PostMapping("/apply")
    public ResponseEntity<Integer> applyRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(merchantRuleApplyService.apply(userDetails.getId()));
    }
}
