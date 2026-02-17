package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class RuleBasedCategorizationStrategy implements CategorizationStrategy {

    @Override
    public Optional<Long> categorize(Transaction transaction, CategorizationContext context) {
        if (transaction.getDescription() == null || context.getRules() == null) {
            return Optional.empty();
        }

        String descUpper = transaction.getDescription().toUpperCase();

        for (CategoryRule rule : context.getRules()) {
            if (descUpper.contains(rule.getKeyword().toUpperCase())) {
                Long categoryName = rule.getCategoryId();

                // Validate against categories if context provides them
                if (context.getCategories() != null && !context.getCategories().isEmpty()) {
                    Category matchedCategory = context.getCategories().stream()
                            .filter(c -> c.getId().equals(categoryName))
                            .findFirst()
                            .orElse(null);
                }

                return Optional.of(categoryName);
            }
        }

        return Optional.empty();
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
