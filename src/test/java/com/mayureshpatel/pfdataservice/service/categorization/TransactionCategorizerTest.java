package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.model.Transaction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCategorizerTest {

    private final TransactionCategorizer categorizer = new TransactionCategorizer();

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
        assertThat(categorizer.guessCategory(t)).isEqualTo("Uncategorized");
    }

    private void assertCategory(String desc, String expectedCategory) {
        Transaction t = new Transaction();
        t.setDescription(desc);
        assertThat(categorizer.guessCategory(t)).isEqualTo(expectedCategory);
    }
}