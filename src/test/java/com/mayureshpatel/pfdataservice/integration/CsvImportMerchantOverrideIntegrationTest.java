package com.mayureshpatel.pfdataservice.integration;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportResultDto;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportStatus;
import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
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
 * description do not auto-match the override. Also proves another user's merchant is rejected,
 * that a merchant cleared in review stays cleared (no description-match fallback) only when the
 * request says merchants were reviewed, and that a reviewed category must be the user's own
 * subcategory.
 */
class CsvImportMerchantOverrideIntegrationTest extends BaseIntegrationTest {

    private static final long USER_ID = 8981L;
    private static final long ACCOUNT_ID = 8981L;
    private static final long MERCHANT_ID = 8981L;
    private static final long OTHER_MERCHANT_ID = 8982L;
    private static final String DESCRIPTION = "PF898 SHARED DESCRIPTION";
    private static final String LINKED_DESCRIPTION = "PF898 LINKED DESCRIPTION";

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
        importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(first, MERCHANT_ID), row(second, null)), null, null, false);

        // assert & verify
        assertEquals(MERCHANT_ID, merchantIdOf(first));
        assertNull(merchantIdOf(second));
        assertEquals(linksBefore, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchant_description_links", Integer.class));

        // a later import of that description must not auto-match the override
        Map<String, Long> matches = merchantService.findMatchingMerchantIds(USER_ID, List.of(DESCRIPTION));
        assertTrue(matches.isEmpty());
        OffsetDateTime third = OffsetDateTime.parse("2024-03-03T00:00:00Z");
        importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(third, null)), null, null, false);
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
                () -> importService.saveTransactions(USER_ID, ACCOUNT_ID, List.of(row(date, OTHER_MERCHANT_ID)), null, null, false));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE account_id = ? AND date = ?", Integer.class, ACCOUNT_ID, date));
    }

    @Test
    @DisplayName("saveBulkTransactions_merchantClearedInReview_staysNullButLegacySaveFallsBack")
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (8981, 'pf898a', 'pf898a@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (8981, 8981, 'PF898 Account', 'CHECKING', 1000.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO merchants (id, user_id, name) VALUES (8981, 8981, 'PF898 Override Merchant') ON CONFLICT DO NOTHING;"
    })
    void saveBulkTransactions_merchantClearedInReview_staysNullButLegacySaveFallsBack() {
        // arrange
        merchantService.recordDescriptionLink(USER_ID, MERCHANT_ID, LINKED_DESCRIPTION);
        OffsetDateTime reviewedDate = OffsetDateTime.parse("2024-05-01T00:00:00Z");
        OffsetDateTime legacyDate = OffsetDateTime.parse("2024-05-02T00:00:00Z");
        TransactionDto reviewedRow = TransactionDto.builder().date(reviewedDate).description(LINKED_DESCRIPTION)
                .amount(new BigDecimal("5.00")).type(TransactionType.EXPENSE).build();
        TransactionDto legacyRow = TransactionDto.builder().date(legacyDate).description(LINKED_DESCRIPTION)
                .amount(new BigDecimal("5.00")).type(TransactionType.EXPENSE).build();

        // act
        List<FileImportResultDto> reviewed = importService.saveBulkTransactions(USER_ID, List.of(
                new SaveTransactionRequest(List.of(reviewedRow), "reviewed.csv", "pf898-reviewed-hash", ACCOUNT_ID, true)));
        List<FileImportResultDto> legacy = importService.saveBulkTransactions(USER_ID, List.of(
                new SaveTransactionRequest(List.of(legacyRow), "legacy.csv", "pf898-legacy-hash", ACCOUNT_ID, false)));

        // assert & verify
        assertEquals(FileImportStatus.IMPORTED, reviewed.get(0).status());
        assertEquals(FileImportStatus.IMPORTED, legacy.get(0).status());
        assertNull(merchantIdOf(reviewedDate));
        assertEquals(MERCHANT_ID, merchantIdOf(legacyDate));
    }

    @Test
    @DisplayName("saveBulkTransactions_categoryNotAnOwnedSubcategory_reportsFailedAndSavesNothing")
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (8981, 'pf898a', 'pf898a@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (8982, 'pf898b', 'pf898b@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (8981, 8981, 'PF898 Account', 'CHECKING', 1000.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO categories (id, user_id, name, color, type) VALUES (8981, 8981, 'PF898 Parent', '#000000', 'EXPENSE') ON CONFLICT DO NOTHING;",
            "INSERT INTO categories (id, user_id, name, color, parent_id, type) VALUES (8982, 8981, 'PF898 Child', '#000000', 8981, 'EXPENSE') ON CONFLICT DO NOTHING;",
            "INSERT INTO categories (id, user_id, name, color, type) VALUES (8983, 8982, 'PF898 Foreign Parent', '#000000', 'EXPENSE') ON CONFLICT DO NOTHING;",
            "INSERT INTO categories (id, user_id, name, color, parent_id, type) VALUES (8984, 8982, 'PF898 Foreign Child', '#000000', 8983, 'EXPENSE') ON CONFLICT DO NOTHING;"
    })
    void saveBulkTransactions_categoryNotAnOwnedSubcategory_reportsFailedAndSavesNothing() {
        // arrange
        OffsetDateTime parentDate = OffsetDateTime.parse("2024-06-01T00:00:00Z");
        OffsetDateTime foreignDate = OffsetDateTime.parse("2024-06-02T00:00:00Z");
        OffsetDateTime childDate = OffsetDateTime.parse("2024-06-03T00:00:00Z");

        // act
        List<FileImportResultDto> results = importService.saveBulkTransactions(USER_ID, List.of(
                new SaveTransactionRequest(List.of(categorizedRow(parentDate, 8981L)), "parent.csv", "pf898-parent-hash", ACCOUNT_ID, true),
                new SaveTransactionRequest(List.of(categorizedRow(foreignDate, 8984L)), "foreign.csv", "pf898-foreign-hash", ACCOUNT_ID, true),
                new SaveTransactionRequest(List.of(categorizedRow(childDate, 8982L)), "child.csv", "pf898-child-hash", ACCOUNT_ID, true)));

        // assert & verify
        assertEquals(new FileImportResultDto("parent.csv", FileImportStatus.FAILED, 0, 0,
                "Only subcategories can be assigned to imported transactions."), results.get(0));
        assertEquals(new FileImportResultDto("foreign.csv", FileImportStatus.FAILED, 0, 0, "Category not found."), results.get(1));
        assertEquals(FileImportStatus.IMPORTED, results.get(2).status());
        assertEquals(0, countOn(parentDate));
        assertEquals(0, countOn(foreignDate));
        assertEquals(8982L, jdbcTemplate.queryForObject(
                "SELECT category_id FROM transactions WHERE account_id = ? AND date = ?", Long.class, ACCOUNT_ID, childDate));
    }

    private TransactionDto categorizedRow(OffsetDateTime date, Long categoryId) {
        return TransactionDto.builder()
                .date(date)
                .description("PF898 CATEGORIZED " + date.getDayOfMonth())
                .amount(new BigDecimal("3.00"))
                .type(TransactionType.EXPENSE)
                .category(CategoryDto.builder().id(categoryId).build())
                .build();
    }

    private int countOn(OffsetDateTime date) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE account_id = ? AND date = ?", Integer.class, ACCOUNT_ID, date);
    }
}
