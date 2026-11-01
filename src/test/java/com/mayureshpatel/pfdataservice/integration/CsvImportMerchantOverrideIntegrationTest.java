package com.mayureshpatel.pfdataservice.integration;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PF-898: proves, against a real database, that a per-row merchant chosen during CSV review is
 * saved on that row only and never teaches the app -- no {@code merchant_description_links} row is
 * created, sibling rows with the same description stay untouched, and later imports of that
 * description do not auto-match the override. Also proves another user's merchant is rejected.
 */
class CsvImportMerchantOverrideIntegrationTest extends BaseIntegrationTest {

    private static final long USER_ID = 8981L;
    private static final long ACCOUNT_ID = 8981L;
    private static final long MERCHANT_ID = 8981L;
    private static final long OTHER_MERCHANT_ID = 8982L;
    private static final String DESCRIPTION = "PF898 SHARED DESCRIPTION";

    @Autowired
    private TransactionImportService importService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TransactionDto row(OffsetDateTime date, Long merchantId) {
        return TransactionDto.builder()
                .date(date)
                .description(DESCRIPTION)
                .amount(new BigDecimal("12.00"))
                .type(TransactionType.EXPENSE)
                .merchant(merchantId == null ? null : MerchantDto.builder().id(merchantId).build())
                .build();
    }

    private Long merchantIdOf(OffsetDateTime date) {
        return jdbcTemplate.queryForObject(
                "SELECT merchant_id FROM transactions WHERE account_id = ? AND date = ?", Long.class, ACCOUNT_ID, date);
    }

    @Test
    @DisplayName("saveTransactions_withMerchantOverride_createsNoDescriptionLinkAndLeavesOtherRowsUntouched")
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (8981, 'pf898a', 'pf898a@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (8982, 'pf898b', 'pf898b@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (8981, 8981, 'PF898 Account', 'CHECKING', 1000.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (8981, 8981, 'PF898 Override Merchant') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (8982, 8982, 'PF898 Foreign Merchant') ON CONFLICT DO NOTHING;"
    })
    void saveTransactions_withMerchantOverride_createsNoDescriptionLinkAndLeavesOtherRowsUntouched() {
        // arrange
        OffsetDateTime first = OffsetDateTime.parse("2024-03-01T00:00:00Z");
        OffsetDateTime second = OffsetDateTime.parse("2024-03-02T00:00:00Z");
        int linksBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchant_description_links", Integer.class);

        // act
        importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(first, MERCHANT_ID), row(second, null)), null, null);

        // assert & verify
        assertEquals(MERCHANT_ID, merchantIdOf(first));
        assertNull(merchantIdOf(second));
        assertEquals(linksBefore, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchant_description_links", Integer.class));

        // a later import of that description must not auto-match the override
        Map<String, Long> matches = merchantService.findMatchingMerchantIds(USER_ID, List.of(DESCRIPTION));
        assertTrue(matches.isEmpty());
        OffsetDateTime third = OffsetDateTime.parse("2024-03-03T00:00:00Z");
        importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(third, null)), null, null);
        assertNull(merchantIdOf(third));
    }

    @Test
    @DisplayName("saveTransactions_merchantOwnedByAnotherUser_isRejectedAndNothingSaved")
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (8981, 'pf898a', 'pf898a@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (8982, 'pf898b', 'pf898b@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (8981, 8981, 'PF898 Account', 'CHECKING', 1000.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (8982, 8982, 'PF898 Foreign Merchant') ON CONFLICT DO NOTHING;"
    })
    void saveTransactions_merchantOwnedByAnotherUser_isRejectedAndNothingSaved() {
        // arrange
        OffsetDateTime date = OffsetDateTime.parse("2024-04-01T00:00:00Z");

        // act & assert & verify
        assertThrows(ResourceNotFoundException.class,
                () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(date, OTHER_MERCHANT_ID)), null, null));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE account_id = ? AND date = ?", Integer.class, ACCOUNT_ID, date));
    }
}
