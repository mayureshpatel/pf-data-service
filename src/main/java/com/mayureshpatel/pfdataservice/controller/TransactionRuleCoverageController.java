package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.TransactionRuleCoverageDto;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TransactionRuleCoverageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only endpoint that tells the ledger which of a page of transactions the user's saved rules
 * already cover (PF-919). Everything is scoped to the authenticated user; ids that are not theirs
 * are silently left out of the answer.
 */
@Tag(name = "Transactions", description = "Querying, creating, updating, and deleting the authenticated user's transactions")
@RestController
@RequestMapping("/api/v1/transactions/rule-coverage")
@RequiredArgsConstructor
@Validated
public class TransactionRuleCoverageController {

    /** The most ids one call accepts, comfortably above the ledger's largest page size. */
    public static final int MAX_IDS = 200;

    private final TransactionRuleCoverageService transactionRuleCoverageService;

    /**
     * Reports, for each given transaction, whether a category rule and whether a merchant rule
     * covers its description.
     *
     * @param userDetails the authenticated user
     * @param ids         the ids of one page of transactions, at least one and at most {@value #MAX_IDS}
     * @return one entry per transaction the user owns among the ids
     */
    @Operation(operationId = "getTransactionRuleCoverage", summary = "Get rule coverage for a page of transactions",
            description = "Says, for each given transaction the user owns, whether a category rule and whether a merchant rule covers its description")
    @ApiResponse(responseCode = "200", description = "Coverage returned (ids the user does not own are left out)")
    @ApiResponse(responseCode = "400", description = "No ids, or more than the allowed number")
    @GetMapping
    public ResponseEntity<List<TransactionRuleCoverageDto>> getCoverage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @NotEmpty @Size(max = MAX_IDS) List<Long> ids) {
        return ResponseEntity.ok(transactionRuleCoverageService.findCoverage(userDetails.getId(), ids));
    }
}
