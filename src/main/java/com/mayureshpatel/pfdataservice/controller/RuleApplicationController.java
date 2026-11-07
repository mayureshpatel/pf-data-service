package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.rule.CreateBothAndApplyRequest;
import com.mayureshpatel.pfdataservice.dto.rule.CreateBothAndApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.rule.RuleApplicationUndoRequest;
import com.mayureshpatel.pfdataservice.dto.rule.RuleApplicationUndoResultDto;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.RuleApplicationService;
import com.mayureshpatel.pfdataservice.service.RuleApplicationUndoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Undoes a rule the ledger just created and applied (PF-922): puts the changed transactions back
 * and deletes the rule, as one all-or-nothing step scoped to the authenticated user.
 */
@Tag(name = "Rule Applications", description = "Undoing a rule that was created and applied to chosen transactions")
@RestController
@RequestMapping("/api/v1/rule-applications")
@RequiredArgsConstructor
public class RuleApplicationController {

    private final RuleApplicationUndoService ruleApplicationUndoService;
    private final RuleApplicationService ruleApplicationService;

    /**
     * Creates a category rule and a merchant rule from the same keyword and applies both to the
     * transactions the user chose, all or nothing (PF-934): if either kind fails, neither is saved.
     *
     * @param userDetails the authenticated user
     * @param request     the category and merchant rules with the transactions for each
     * @return each kind's result, which together are what an undo needs
     */
    @Operation(operationId = "createBothRulesAndApply", summary = "Create a category rule and a merchant rule and apply both",
            description = "Creates both rules and applies each to its chosen transactions, overwriting existing values; all or nothing across the two kinds")
    @ApiResponse(responseCode = "200", description = "Both rules created and applied; previous values returned for undo")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "403", description = "A chosen transaction or the category is not the user's; nothing changed")
    @ApiResponse(responseCode = "404", description = "The merchant does not exist or is not the authenticated user's")
    @PostMapping("/create-both-and-apply")
    public ResponseEntity<CreateBothAndApplyResultDto> createBothAndApply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CreateBothAndApplyRequest request) {
        return ResponseEntity.ok(ruleApplicationService.createBothAndApply(userDetails.getId(), request));
    }

    /**
     * Undoes a create-and-apply: restores the previous category and/or merchant of each transaction
     * it changed (leaving any that changed since alone) and deletes the rule(s) it created.
     *
     * @param userDetails the authenticated user
     * @param request     the create-and-apply result(s) to undo, sent back as returned
     * @return how many transactions were restored, which were left alone, and which rules were deleted
     */
    @Operation(operationId = "undoRuleApplication", summary = "Undo a created-and-applied rule",
            description = "Restores the previous values the apply reported, leaving rows that changed since alone, and deletes the rule(s) it created")
    @ApiResponse(responseCode = "200", description = "Undone; the response says what was restored and skipped")
    @ApiResponse(responseCode = "400", description = "Neither a category nor a merchant result was provided")
    @PostMapping("/undo")
    public ResponseEntity<RuleApplicationUndoResultDto> undo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid RuleApplicationUndoRequest request) {
        return ResponseEntity.ok(ruleApplicationUndoService.undo(userDetails.getId(), request));
    }
}
