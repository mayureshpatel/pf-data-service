package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.model.Category;
import com.mayureshpatel.pfdataservice.model.CategoryRule;
import com.mayureshpatel.pfdataservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class RuleBasedCategorizationStrategy implements CategorizationStrategy {

    @Override
    public Optional<String> categorize(Transaction transaction, CategorizationContext context) {
        if (transaction.getDescription() == null || context.getRules() == null) {
            return Optional.empty();
        }

        String descUpper = transaction.getDescription().toUpperCase();

        for (CategoryRule rule : context.getRules()) {
            if (descUpper.contains(rule.getKeyword().toUpperCase())) {
                String categoryName = rule.getCategoryName();

                // Validate against categories if context provides them
                if (context.getCategories() != null && !context.getCategories().isEmpty()) {
                    Category matchedCategory = context.getCategories().stream()
                            .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                            .findFirst()
                            .orElse(null);

                    // Skip if it's a parent category
                    if (matchedCategory != null && matchedCategory.getParent() == null) {
                        log.debug("Skipping parent category '{}' in auto-categorization for transaction: {}",
                                categoryName, transaction.getDescription());
                        continue;
                    }
                }

                return Optional.of(categoryName);
            }
        }

        return Optional.empty();
    }

    @Override
    public int getOrder() {
        return 100; // Primary rule-based logic
    }
}
