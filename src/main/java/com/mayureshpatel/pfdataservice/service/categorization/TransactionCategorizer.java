package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionCategorizer {

    private final List<CategorizationStrategy> strategies;

    /**
     * Analyzes the transaction description and returns a best-guess category name.
     */
    public Long guessCategory(Transaction transaction, List<CategoryRule> rules) {
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
    public Long guessCategory(Transaction transaction, List<CategoryRule> rules, List<Category> categories) {
        CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                .userId(transaction.getAccount() != null
                        ? transaction.getAccount().getUser() != null
                        ? transaction.getAccount().getUser().getId()
                        : null
                        : null)
                .rules(rules)
                .categories(categories)
                .build();

        return this.strategies.stream()
                .sorted(Comparator.comparingInt(CategorizationStrategy::getOrder))
                .map(s -> s.categorize(transaction, context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(-1L);
    }
}
