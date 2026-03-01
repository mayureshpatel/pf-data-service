package com.mayureshpatel.pfdataservice.repository.transaction.specification;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionSpecification Unit Tests")
class TransactionSpecificationTest {

    @Test
    @DisplayName("should build where clause with basic user filter")
    void shouldBuildBasicWhereClause() {
        TransactionSpecification.FilterResult result = TransactionSpecification.withFilter(1L, null);

        assertThat(result.whereClause()).contains("a.user_id = :userId");
        assertThat(result.whereClause()).contains("t.deleted_at IS NULL");
        assertThat(result.parameters()).containsEntry("userId", 1L);
    }

    @Test
    @DisplayName("should build where clause with account filter")
    void shouldBuildAccountWhereClause() {
        TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                10L, null, null, null, null, null, null, null, null
        );
        TransactionSpecification.FilterResult result = TransactionSpecification.withFilter(1L, filter);

        assertThat(result.whereClause()).contains("t.account_id = :accountId");
        assertThat(result.parameters()).containsEntry("accountId", 10L);
    }

    @Test
    @DisplayName("should build where clause with type filter")
    void shouldBuildTypeWhereClause() {
        TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                null, TransactionType.INCOME, null, null, null, null, null, null, null
        );
        TransactionSpecification.FilterResult result = TransactionSpecification.withFilter(1L, filter);

        assertThat(result.whereClause()).contains("t.type = :type");
        assertThat(result.parameters()).containsEntry("type", "INCOME");
    }

    @Test
    @DisplayName("should handle TRANSFER type specially")
    void shouldHandleTransferType() {
        TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                null, TransactionType.TRANSFER, null, null, null, null, null, null, null
        );
        TransactionSpecification.FilterResult result = TransactionSpecification.withFilter(1L, filter);

        assertThat(result.whereClause()).contains("t.type IN ('TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT')");
        assertThat(result.parameters()).doesNotContainKey("type");
    }

    @Test
    @DisplayName("should build where clause with description filter")
    void shouldBuildDescriptionWhereClause() {
        TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                null, null, "Groceries", null, null, null, null, null, null
        );
        TransactionSpecification.FilterResult result = TransactionSpecification.withFilter(1L, filter);

        assertThat(result.whereClause()).contains("LOWER(t.description) LIKE :description");
        assertThat(result.parameters()).containsEntry("description", "%groceries%");
    }

    @Test
    @DisplayName("should build where clause with amount range")
    void shouldBuildAmountRangeWhereClause() {
        TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                null, null, null, null, null, new BigDecimal("50.00"), new BigDecimal("100.00"), null, null
        );
        TransactionSpecification.FilterResult result = TransactionSpecification.withFilter(1L, filter);

        assertThat(result.whereClause()).contains("t.amount >= :minAmount");
        assertThat(result.whereClause()).contains("t.amount <= :maxAmount");
        assertThat(result.parameters()).containsEntry("minAmount", new BigDecimal("50.00"));
        assertThat(result.parameters()).containsEntry("maxAmount", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("should build where clause with date range")
    void shouldBuildDateRangeWhereClause() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                null, null, null, null, null, null, null, start, end
        );
        TransactionSpecification.FilterResult result = TransactionSpecification.withFilter(1L, filter);

        assertThat(result.whereClause()).contains("t.date >= :startDate");
        assertThat(result.whereClause()).contains("t.date <= :endDate");
        assertThat(result.parameters()).containsEntry("startDate", start);
        assertThat(result.parameters()).containsEntry("endDate", end);
    }

    @Test
    @DisplayName("should handle null category filter")
    void shouldHandleNullCategoryFilter() {
        TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                null, null, null, "null", null, null, null, null, null
        );
        TransactionSpecification.FilterResult result = TransactionSpecification.withFilter(1L, filter);

        assertThat(result.whereClause()).contains("t.category_id IS NULL");
    }
}
