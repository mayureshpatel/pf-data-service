package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.VendorRuleDto;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.model.VendorRule;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.UserRepository;
import com.mayureshpatel.pfdataservice.repository.VendorRuleRepository;
import com.mayureshpatel.pfdataservice.service.categorization.VendorCleaner;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        // Fetch all transactions for user
        // Note: For large datasets, pagination/batching is better. For MVP (personal finance), fetching all is okay-ish.
        // Optimization: Fetch only transactions where vendor_name is null or equals original_vendor_name?
        // Or simply re-process everything to ensure new rules overwrite old cleanups.
        
        List<VendorRule> rules = vendorCleaner.loadRulesForUser(userId);
        // We need a custom query or Specification to fetch all transactions for user
        // findRecentNonTransferTransactions uses JOIN, let's use something similar or add findByUserId
        
        // Let's use a simple query on TransactionRepository
        List<Transaction> transactions = transactionRepository.findByAccount_User_Id(userId);
        
        int updatedCount = 0;
        for (Transaction t : transactions) {
            String original = t.getOriginalVendorName();
            if (original == null) original = t.getDescription(); // Fallback
            
            String newVendor = vendorCleaner.cleanVendorName(original, rules);
            
            // If rule found, update. If no rule found, we might want to revert to original? 
            // Current logic: cleanVendorName returns null if no rule matches.
            // If null, we generally keep existing or revert to original?
            // Let's say: If rule matches, update. If no rule matches, do nothing (preserve manual edits or previous state).
            // OR: Revert to original if no rule matches? That's risky if user manually renamed.
            
            if (newVendor != null && !newVendor.equals(t.getVendorName())) {
                t.setVendorName(newVendor);
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
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        VendorRule rule = new VendorRule();
        rule.setUser(user);
        rule.setKeyword(dto.keyword());
        rule.setVendorName(dto.vendorName());
        rule.setPriority(dto.priority() != null ? dto.priority() : 0);

        return mapToDto(vendorRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        VendorRule rule = vendorRuleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found"));

        if (rule.getUser() != null && !rule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this rule");
        }
        
        // Prevent deleting global rules via this API (unless admin, but for now simple check)
        if (rule.getUser() == null) {
             throw new AccessDeniedException("Cannot delete global rules");
        }

        vendorRuleRepository.delete(rule);
    }

    private VendorRuleDto mapToDto(VendorRule rule) {
        return VendorRuleDto.builder()
                .id(rule.getId())
                .keyword(rule.getKeyword())
                .vendorName(rule.getVendorName())
                .priority(rule.getPriority())
                .build();
    }
}
