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

    public List<CategoryRule> loadRulesForUser(Long userId) {
        return categoryRuleRepository.findByUserOrGlobal(userId);
    }

    /**
     * Analyzes the transaction description and returns a best-guess category name.
     * Logic: Returns the category associated with the highest priority (then longest) matching keyword.
     */
    public String guessCategory(Transaction transaction, List<CategoryRule> rules) {
        if (transaction.getDescription() == null) {
            return "Uncategorized";
        }

        String descUpper = transaction.getDescription().toUpperCase();

        // Rules are expected to be ordered by Priority DESC, then Length DESC
        for (CategoryRule rule : rules) {
            if (descUpper.contains(rule.getKeyword().toUpperCase())) {
                return rule.getCategoryName();
            }
        }

        return "Uncategorized";
    }
}
