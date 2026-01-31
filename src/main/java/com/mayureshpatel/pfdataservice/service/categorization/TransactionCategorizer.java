package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.model.Category;
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
        return guessCategory(transaction, rules, null);
    }

    /**
     * Analyzes the transaction description and returns a best-guess category name.
     * Logic: Returns the category associated with the highest priority (then longest) matching keyword.
     * Skips parent categories (categories without a parent) to ensure only child categories are suggested.
     *
     * @param transaction The transaction to categorize
     * @param rules The category rules to match against
     * @param categories Optional list of categories to validate against (filters out parent categories)
     * @return The suggested category name, or "Uncategorized" if no match found
     */
    public String guessCategory(Transaction transaction, List<CategoryRule> rules, List<Category> categories) {
        if (transaction.getDescription() == null) {
            return "Uncategorized";
        }

        String descUpper = transaction.getDescription().toUpperCase();

        // Rules are expected to be ordered by Priority DESC, then Length DESC
        for (CategoryRule rule : rules) {
            if (descUpper.contains(rule.getKeyword().toUpperCase())) {
                String categoryName = rule.getCategoryName();

                // If categories list provided, skip parent categories
                if (categories != null && !categories.isEmpty()) {
                    Category matchedCategory = categories.stream()
                            .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                            .findFirst()
                            .orElse(null);

                    // Skip if it's a parent category (no parent itself)
                    if (matchedCategory != null && matchedCategory.getParent() == null) {
                        log.debug("Skipping parent category '{}' in auto-categorization for transaction: {}",
                                categoryName, transaction.getDescription());
                        continue;
                    }
                }

                return categoryName;
            }
        }

        return "Uncategorized";
    }
}
