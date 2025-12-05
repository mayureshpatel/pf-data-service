package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class TransactionCategorizer {
    private static final Map<String, String> KEYWORD_RULES = new HashMap<>();

    static {
        // Groceries
        KEYWORD_RULES.put("PUBLIX", "Groceries");
        KEYWORD_RULES.put("KROGER", "Groceries");
        KEYWORD_RULES.put("WHOLE FOODS", "Groceries");
        KEYWORD_RULES.put("TRADER JOE", "Groceries");
        KEYWORD_RULES.put("WALMART", "Groceries");
        KEYWORD_RULES.put("WEGMANS", "Groceries");

        // Dining
        KEYWORD_RULES.put("MCDONALD", "Dining Out");
        KEYWORD_RULES.put("STARBUCKS", "Dining Out");
        KEYWORD_RULES.put("DUNKIN", "Dining Out");
        KEYWORD_RULES.put("TACO BELL", "Dining Out");
        KEYWORD_RULES.put("CHIPOTLE", "Dining Out");
        KEYWORD_RULES.put("UBER EATS", "Dining Out");
        KEYWORD_RULES.put("DOMINO", "Dining Out");

        // Utilities/Services
        KEYWORD_RULES.put("AT&T", "Utilities");
        KEYWORD_RULES.put("VERIZON", "Utilities");
        KEYWORD_RULES.put("XFINITY", "Utilities");
        KEYWORD_RULES.put("POWER", "Utilities");
        KEYWORD_RULES.put("WATER", "Utilities");

        // Entertainment
        KEYWORD_RULES.put("NETFLIX", "Entertainment");
        KEYWORD_RULES.put("SPOTIFY", "Entertainment");
        KEYWORD_RULES.put("STEAM", "Entertainment");
        KEYWORD_RULES.put("NINTENDO", "Entertainment");

        // Transport
        KEYWORD_RULES.put("UBER", "Transportation"); // Note: Order matters if checking contains vs exact
        KEYWORD_RULES.put("LYFT", "Transportation");
        KEYWORD_RULES.put("SHELL", "Gas");
        KEYWORD_RULES.put("CHEVRON", "Gas");
        KEYWORD_RULES.put("EXXON", "Gas");
    }

    /**
     * Analyzes the transaction description and returns a best-guess category name.
     * Logic: Returns the category associated with the LONGEST matching keyword.
     */
    public String guessCategory(Transaction transaction) {
        if (transaction.getDescription() == null) {
            return "Uncategorized";
        }

        String descUpper = transaction.getDescription().toUpperCase();

        Optional<Map.Entry<String, String>> bestMatch = KEYWORD_RULES.entrySet().stream()
                .filter(entry -> descUpper.contains(entry.getKey()))
                .max(Comparator.comparingInt(entry -> entry.getKey().length()));

        return bestMatch.map(Map.Entry::getValue).orElse("Uncategorized");
    }
}
