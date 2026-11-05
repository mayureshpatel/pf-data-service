package com.mayureshpatel.pfdataservice.integration;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.service.MerchantService;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PF-906: proves, against a real database, that the legacy save path (merchantsReviewed=false)
 * chooses each row's merchant link first, then the highest-ranked matching merchant rule, then
 * none; that a link recorded with different case and spacing still matches (the folded link key)
 * while rules match the raw description; that an amount-range rule decides per row; and that
 * applying a rule never creates a description link.
 */
class CsvImportMerchantRuleIntegrationTest extends BaseIntegrationTest {

    private static final long USER_ID = 9061L;
    private static final long ACCOUNT_ID = 9061L;
    private static final long LINKED_MERCHANT_ID = 9061L;
    private static final long RULE_MERCHANT_ID = 9062L;
    private static final long SMALL_MERCHANT_ID = 9063L;

    @Autowired
    private TransactionImportService importService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TransactionDto row(OffsetDateTime date, String description, String amount) {
        return TransactionDto.builder().date(date).description(description)
                .amount(new BigDecimal(amount)).type(TransactionType.EXPENSE).build();
    }

    private Long merchantIdOf(OffsetDateTime date) {
        return jdbcTemplate.queryForObject(
                "SELECT merchant_id FROM transactions WHERE account_id = ? AND date = ?", Long.class, ACCOUNT_ID, date);
    }

    @Test
    @DisplayName("saveTransactions_legacyPath_linkBeatsRuleRuleFillsAndAmountRangeDecidesPerRow")
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (9061, 'pf906a', 'pf906a@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (9061, 9061, 'PF906 Account', 'CHECKING', 1000.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (9061, 9061, 'PF906 Linked') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (9062, 9061, 'PF906 Rule Big') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (9063, 9061, 'PF906 Rule Small') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchant_rules (id, merchant_id, priority, min_amount, max_amount, match_type, user_id, created_at, updated_at) VALUES (9061, 9062, 0, 30.01, NULL, 'OR', 9061, NOW(), NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO merchant_rules (id, merchant_id, priority, min_amount, max_amount, match_type, user_id, created_at, updated_at) VALUES (9062, 9063, 0, NULL, 30.00, 'OR', 9061, NOW(), NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO merchant_rule_keywords (rule_id, keyword) VALUES (9061, 'pf906 kroger'), (9062, 'pf906 kroger');"
    })
    void saveTransactions_legacyPath_linkBeatsRuleRuleFillsAndAmountRangeDecidesPerRow() {
        // arrange
        // the link is recorded with different case and spacing than the imported description
        merchantService.recordDescriptionLink(USER_ID, LINKED_MERCHANT_ID, "pf906   KROGER #1");
        int linksBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchant_description_links", Integer.class);
        OffsetDateTime linked = OffsetDateTime.parse("2024-07-01T00:00:00Z");
        OffsetDateTime small = OffsetDateTime.parse("2024-07-02T00:00:00Z");
        OffsetDateTime big = OffsetDateTime.parse("2024-07-03T00:00:00Z");
        OffsetDateTime none = OffsetDateTime.parse("2024-07-04T00:00:00Z");

        // act
        importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(
                row(linked, "PF906 Kroger #1", "12.00"),
                row(small, "PF906 KROGER #2", "12.00"),
                row(big, "PF906 KROGER #2", "120.00"),
                row(none, "PF906 SOMEWHERE ELSE", "12.00")), null, null, false);

        // assert & verify
        assertEquals(LINKED_MERCHANT_ID, merchantIdOf(linked));
        assertEquals(SMALL_MERCHANT_ID, merchantIdOf(small));
        assertEquals(RULE_MERCHANT_ID, merchantIdOf(big));
        assertNull(merchantIdOf(none));
        assertEquals(linksBefore, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchant_description_links", Integer.class));
    }
}
