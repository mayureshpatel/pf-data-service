package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.model.CategoryRule;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.repository.CategoryRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionCategorizer {

    private final CategoryRuleRepository categoryRuleRepository;
    private List<CategoryRule> cachedRules = Collections.emptyList();

    @PostConstruct
    public void init() {
        refreshRules();
    }

    public void refreshRules() {
        try {
            this.cachedRules = categoryRuleRepository.findAllOrdered();
            log.info("Loaded {} categorization rules from database.", cachedRules.size());
        } catch (Exception e) {
            log.error("Failed to load categorization rules", e);
        }
    }

    /**
     * Analyzes the transaction description and returns a best-guess category name.
     * Logic: Returns the category associated with the highest priority (then longest) matching keyword.
     */
    public String guessCategory(Transaction transaction) {
        if (transaction.getDescription() == null) {
            return "Uncategorized";
        }

        String descUpper = transaction.getDescription().toUpperCase();

        // Rules are ordered by Priority DESC, then Length DESC in the query
        for (CategoryRule rule : cachedRules) {
            if (descUpper.contains(rule.getKeyword().toUpperCase())) {
                return rule.getCategoryName();
            }
        }

        return "Uncategorized";
    }
}