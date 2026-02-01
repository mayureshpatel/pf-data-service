package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.model.CategoryRule;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.repository.CategoryRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionCategorizerTest {

    @Mock
    private CategoryRuleRepository categoryRuleRepository;

    private TransactionCategorizer categorizer;
    private List<CategoryRule> rules;

    @BeforeEach
    void setUp() {
        categorizer = new TransactionCategorizer(categoryRuleRepository, List.of(new RuleBasedCategorizationStrategy()));

        // Mock rules - Order matters (simulating Priority DESC, Length DESC)
        // Constructor: id, keyword, categoryName, priority, user, createdAt, updatedAt
        rules = List.of(
                new CategoryRule(4L, "UBER EATS", "Dining Out", 5, null, null, null), 
                new CategoryRule(1L, "PUBLIX", "Groceries", 1, null, null, null),
                new CategoryRule(2L, "KROGER", "Groceries", 1, null, null, null),
                new CategoryRule(3L, "MCDONALD", "Dining Out", 1, null, null, null),
                new CategoryRule(5L, "UBER", "Transportation", 1, null, null, null), 
                new CategoryRule(6L, "SHELL", "Gas", 1, null, null, null),
                new CategoryRule(7L, "NETFLIX", "Entertainment", 1, null, null, null)
        );
    }

    @Test
    void shouldCategorizeKnownMerchants() {
        assertCategory("PUBLIX #123", "Groceries");
        assertCategory("KROGER FUEL", "Groceries");
        assertCategory("McDonalds #44", "Dining Out");
        assertCategory("Uber *Trip", "Transportation");
        assertCategory("Uber Eats", "Dining Out");
        assertCategory("Shell Oil", "Gas");
        assertCategory("Netflix.com", "Entertainment");
    }

    @Test
    void shouldDefaultToUncategorizedForUnknown() {
        assertCategory("Mystery Shop", "Uncategorized");
        assertCategory("Check #123", "Uncategorized");
    }

    @Test
    void shouldHandleNullDescription() {
        Transaction t = new Transaction();
        t.setDescription(null);
        assertThat(categorizer.guessCategory(t, rules)).isEqualTo("Uncategorized");
    }

    private void assertCategory(String desc, String expectedCategory) {
        Transaction t = new Transaction();
        t.setDescription(desc);
        assertThat(categorizer.guessCategory(t, rules)).isEqualTo(expectedCategory);
    }
}
