package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.repository.category.model.Category;
import com.mayureshpatel.pfdataservice.repository.category.model.CategoryRule;
import com.mayureshpatel.pfdataservice.repository.transaction.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionCategorizer {

    private final List<CategorizationStrategy> strategies;

    /**
     * Analyzes the transaction description and returns a best-guess category name.
     */
    public String guessCategory(Transaction transaction, List<CategoryRule> rules) {
        return guessCategory(transaction, rules, null);
    }

    /**
     * Analyzes the transaction description and returns a best-guess category name using multiple strategies.
     *
     * @param transaction The transaction to categorize
     * @param rules       The category rules to match against
     * @param categories  Optional list of categories to validate against
     * @return The suggested category name, or "Uncategorized" if no match found
     */
    public String guessCategory(Transaction transaction, List<CategoryRule> rules, List<Category> categories) {
        CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                .userId(transaction.getAccount() != null ? transaction.getAccount().getUser().getId() : null)
                .rules(rules)
                .categories(categories)
                .build();

        return strategies.stream()
                .sorted(java.util.Comparator.comparingInt(CategorizationStrategy::getOrder))
                .map(s -> s.categorize(transaction, context))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .findFirst()
                .orElse("Uncategorized");
    }
}
