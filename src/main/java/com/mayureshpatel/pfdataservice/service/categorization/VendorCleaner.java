package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.vendor.VendorRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VendorCleaner {

    private final VendorRuleRepository vendorRuleRepository;

    public List<VendorRule> loadRulesForUser(Long userId) {
        return vendorRuleRepository.findByUserOrGlobal(userId);
    }

    /**
     * Analyzes the transaction description and returns a clean vendor name.
     * Logic: Returns the vendor name associated with the highest priority (then longest) matching keyword.
     * Returns null if no rule matches.
     */
    public String cleanVendorName(String description, List<VendorRule> rules) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String descUpper = description.toUpperCase();

        // Rules are expected to be ordered by Priority DESC, then Length DESC
        for (VendorRule rule : rules) {
            if (descUpper.contains(rule.getKeyword().toUpperCase())) {
                return rule.getVendorName();
            }
        }

        return null;
    }
}
