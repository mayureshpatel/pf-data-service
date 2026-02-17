package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.vendor.VendorRule;
import com.mayureshpatel.pfdataservice.dto.vendor.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.vendor.UnmatchedVendorDto;
import com.mayureshpatel.pfdataservice.dto.vendor.VendorRuleDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.repository.vendor.VendorRuleRepository;
import com.mayureshpatel.pfdataservice.service.categorization.VendorCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorRuleService {

    private final VendorRuleRepository vendorRuleRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final VendorCleaner vendorCleaner;

    public List<VendorRuleDto> getRules(Long userId) {
        return vendorRuleRepository.findByUserOrGlobal(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void applyRules(Long userId) {
        List<VendorRule> rules = vendorCleaner.loadRulesForUser(userId);
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        int updatedCount = 0;
        for (Transaction t : transactions) {
            String original = t.getOriginalVendorName();
            if (original == null) original = t.getDescription();

            String newVendor = vendorCleaner.cleanVendorName(original, rules);

            if (newVendor != null && !newVendor.equals(t.getVendor().getName())) {
                t.getVendor().setName(newVendor);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            transactionRepository.saveAll(transactions);
        }
    }

    @Transactional
    public VendorRuleDto createRule(Long userId, VendorRuleDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VendorRule rule = new VendorRule();
        rule.setUser(user);
        rule.setKeyword(dto.keyword());
        rule.getVendor().setName(dto.vendorName());
        rule.setPriority(dto.priority() != null ? dto.priority() : 0);

        return mapToDto(vendorRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        VendorRule rule = vendorRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        if (rule.getUser() != null && !rule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this rule");
        }

        // Prevent deleting global rules via this API (unless admin, but for now simple check)
        if (rule.getUser() == null) {
            throw new AccessDeniedException("Cannot delete global rules");
        }

        vendorRuleRepository.delete(rule);
    }

    public List<RuleChangePreviewDto> previewApply(Long userId) {
        List<VendorRule> rules = vendorCleaner.loadRulesForUser(userId);
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        List<RuleChangePreviewDto> previews = new ArrayList<>();

        for (Transaction t : transactions) {
            String original = t.getOriginalVendorName();
            if (original == null) original = t.getDescription();

            String newVendor = vendorCleaner.cleanVendorName(original, rules);

            if (newVendor != null && !newVendor.equals(t.getVendor().getName())) {
                previews.add(new RuleChangePreviewDto(
                        t.getDescription(),
                        t.getVendor().getName(),
                        newVendor
                ));
            }
        }

        return previews;
    }

    public List<UnmatchedVendorDto> getUnmatchedVendors(Long userId) {
        List<VendorRule> rules = vendorCleaner.loadRulesForUser(userId);
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        Map<String, Long> grouped = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getOriginalVendorName() != null ? t.getOriginalVendorName() : t.getDescription(),
                        Collectors.counting()
                ));

        List<UnmatchedVendorDto> unmatched = new ArrayList<>();

        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            String name = entry.getKey();
            if (vendorCleaner.cleanVendorName(name, rules) == null) {
                unmatched.add(new UnmatchedVendorDto(name, entry.getValue().intValue()));
            }
        }

        unmatched.sort((a, b) -> Integer.compare(b.count(), a.count()));

        return unmatched;
    }

    private VendorRuleDto mapToDto(VendorRule rule) {
        return VendorRuleDto.builder()
                .id(rule.getId())
                .keyword(rule.getKeyword())
                .vendorName(rule.getVendor().getName())
                .priority(rule.getPriority())
                .build();
    }
}
