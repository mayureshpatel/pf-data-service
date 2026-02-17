package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

public interface CategorizationStrategy {
    
    @Getter
    @Builder
    class CategorizationContext {
        private final Long userId;
        private final List<CategoryRule> rules;
        private final List<Category> categories;
    }

    /**
     * Attempts to categorize the transaction.
     * @return Optional containing the category id if matched, or empty if not matched.
     */
    Optional<Long> categorize(Transaction transaction, CategorizationContext context);

    /**
     * Determines the order in which strategies are applied. Lower values run first.
     */
    int getOrder();
}
