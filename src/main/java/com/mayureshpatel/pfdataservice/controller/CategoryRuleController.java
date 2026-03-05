package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.CategoryRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/category-rules")
@RequiredArgsConstructor
public class CategoryRuleController {

    private final CategoryRuleService categoryRuleService;

    @GetMapping
    public ResponseEntity<List<CategoryRuleDto>> getRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryRuleService.getRules(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<Integer> createRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CategoryRuleCreateRequest request) {
        return ResponseEntity.ok(categoryRuleService.createRule(userDetails.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integer> updateRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody @Valid CategoryRuleUpdateRequest request) {
        return ResponseEntity.ok(categoryRuleService.updateRule(userDetails.getId(), id, request));
    }

    @GetMapping("/preview")
    public ResponseEntity<List<RuleChangePreviewDto>> previewApply(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryRuleService.previewApply(userDetails.getId()));
    }

    @PostMapping("/apply")
    public ResponseEntity<Void> applyRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        categoryRuleService.applyRules(userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        categoryRuleService.deleteRule(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
