package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.UnmatchedVendorDto;
import com.mayureshpatel.pfdataservice.dto.VendorRuleDto;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.VendorRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vendor-rules")
@RequiredArgsConstructor
public class VendorRuleController {

    private final VendorRuleService vendorRuleService;

    @GetMapping
    public ResponseEntity<List<VendorRuleDto>> getRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(vendorRuleService.getRules(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<VendorRuleDto> createRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid VendorRuleDto dto) {
        return ResponseEntity.ok(vendorRuleService.createRule(userDetails.getId(), dto));
    }

    @PostMapping("/apply")
    public ResponseEntity<Void> applyRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        vendorRuleService.applyRules(userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preview")
    public ResponseEntity<List<RuleChangePreviewDto>> previewApply(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(vendorRuleService.previewApply(userDetails.getId()));
    }

    @GetMapping("/unmatched")
    public ResponseEntity<List<UnmatchedVendorDto>> getUnmatchedVendors(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(vendorRuleService.getUnmatchedVendors(userDetails.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        vendorRuleService.deleteRule(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
