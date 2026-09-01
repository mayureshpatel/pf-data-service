package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Guesses a category for a transaction by running every registered {@link CategorizationStrategy}
 * in priority order and taking the first match. Currently there's only one strategy
 * ({@link RuleBasedCategorizationStrategy}), but new ones can be added without touching this
 * class.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionCategorizer {

    private final List<CategorizationStrategy> strategies;

    /**
     * Guesses a category, without validating the guess against a known category list.
     *
     * @param transaction the transaction to categorize
     * @param rules       the category rules to match against
     * @return the guessed category id, or -1 if no strategy matched
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
                .userId(transaction.getAccount() != null && transaction.getAccount().getUserId() != null
                        ? transaction.getAccount().getUserId()
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

    /**
     * Same as {@link #guessCategory(Transaction, List, List)}, for a transaction that's being
     * edited rather than one already persisted.
     *
     * @param userId      the user id, used to scope the categorization context
     * @param transaction the transaction being edited
     * @param rules       the category rules to match against
     * @param categories  the categories to validate the guess against
     * @return the guessed category id, or -1 if no strategy matched
     */
    public Long guessCategory(Long userId, TransactionUpdateRequest transaction, List<CategoryRule> rules, List<Category> categories) {
        CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                .userId(userId)
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
