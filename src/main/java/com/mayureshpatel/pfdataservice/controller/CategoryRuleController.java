package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.CategoryRuleService;
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
 * Rules that automatically assign a category to a transaction based on its description or
 * merchant. Rules are matched in order against a user's transactions; {@link #previewApply} and
 * {@link #applyRules} let a user see and then commit the effect of running the current rule set.
 */
@Tag(name = "Category Rules", description = "Auto-categorization rules and their apply/preview flow")
@RestController
@RequestMapping("/api/v1/category-rules")
@RequiredArgsConstructor
public class CategoryRuleController {

    private final CategoryRuleService categoryRuleService;

    /**
     * Returns the authenticated user's category rules.
     *
     * @param userDetails the authenticated user
     * @return the user's rules
     */
    @Operation(summary = "List category rules", description = "Returns the authenticated user's category rules")
    @ApiResponse(responseCode = "200", description = "Rules returned (possibly empty)")
    @GetMapping
    public ResponseEntity<List<CategoryRuleDto>> getRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryRuleService.getRules(userDetails.getId()));
    }

    /**
     * Creates a new category rule.
     *
     * @param userDetails the authenticated user
     * @param request     the rule to create
     * @return the new rule's generated id
     */
    @Operation(summary = "Create a category rule", description = "Creates a new category rule for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Rule created, id returned")
    @PostMapping
    public ResponseEntity<Integer> createRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CategoryRuleCreateRequest request) {
        return ResponseEntity.ok(categoryRuleService.createRule(userDetails.getId(), request));
    }

    /**
     * Updates an existing category rule. Ownership-guarded: the rule's own id doesn't prove the
     * caller owns it, so this is checked separately from authentication.
     *
     * @param userDetails the authenticated user
     * @param request     the rule to update, including its id
     * @return the updated rule's id
     */
    @Operation(summary = "Update a category rule", description = "Updates a category rule owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Rule updated, id returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this rule")
    @PutMapping
    @PreAuthorize("@ss.isRuleOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CategoryRuleUpdateRequest request) {
        return ResponseEntity.ok(categoryRuleService.updateRule(userDetails.getId(), request));
    }

    /**
     * Previews which transactions would be recategorized if the user's current rule set were
     * applied, without actually changing anything.
     *
     * @param userDetails the authenticated user
     * @return the transactions that would change, and what they'd change to
     */
    @Operation(summary = "Preview applying rules", description = "Shows which transactions would be recategorized if the rule set were applied, without changing anything")
    @ApiResponse(responseCode = "200", description = "Preview returned (possibly empty)")
    @GetMapping("/preview")
    public ResponseEntity<List<RuleChangePreviewDto>> previewApply(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryRuleService.previewApply(userDetails.getId()));
    }

    /**
     * Applies the user's current rule set, recategorizing matching transactions.
     *
     * @param userDetails the authenticated user
     * @return 200 with no body once applied
     */
    @Operation(summary = "Apply rules", description = "Applies the current rule set, recategorizing matching transactions")
    @ApiResponse(responseCode = "200", description = "Rules applied")
    @PostMapping("/apply")
    public ResponseEntity<Void> applyRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        categoryRuleService.applyRules(userDetails.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a category rule by id. Ownership-guarded the same way as {@link #updateRule}.
     *
     * @param userDetails the authenticated user
     * @param id          the rule id to delete
     * @return 204 once deleted
     */
    @Operation(summary = "Delete a category rule", description = "Deletes a category rule owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Rule deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this rule")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isRuleOwner(#id, principal)")
    public ResponseEntity<Void> deleteRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        categoryRuleService.deleteRule(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
