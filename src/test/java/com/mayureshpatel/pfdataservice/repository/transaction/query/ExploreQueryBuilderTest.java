package com.mayureshpatel.pfdataservice.repository.transaction.query;

import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreFilter;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import com.mayureshpatel.pfdataservice.repository.transaction.query.ExploreQueryBuilder.BuiltQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code ExploreQueryBuilder}: every grouping maps to a whitelisted SQL expression, and every filter value stays a named parameter, never part of the SQL text. */
@DisplayName("ExploreQueryBuilder Unit Tests")
class ExploreQueryBuilderTest {

    private static final ExploreFilter NO_FILTER = ExploreFilter.none();

    private static Stream<Arguments> groupings() {
        return Stream.of(
                Arguments.of(ExploreGroupBy.CATEGORY, "coalesce(parent_categories.id, categories.id)"),
                Arguments.of(ExploreGroupBy.SUBCATEGORY, "categories.id"),
                Arguments.of(ExploreGroupBy.MERCHANT, "merchants.id"),
                Arguments.of(ExploreGroupBy.ACCOUNT, "accounts.id"),
                Arguments.of(ExploreGroupBy.TAG, "tags.id"),
                Arguments.of(ExploreGroupBy.MONTH, "date_trunc('month', transactions.date at time zone 'UTC')"),
                Arguments.of(ExploreGroupBy.WEEK, "date_trunc('week', transactions.date at time zone 'UTC')"),
                Arguments.of(ExploreGroupBy.DAY_OF_WEEK, "extract(isodow from transactions.date at time zone 'UTC')"),
                Arguments.of(ExploreGroupBy.DAY, "(transactions.date at time zone 'UTC')::date"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("groupings")
    @DisplayName("should group by the whitelisted expression")
    void shouldGroupByWhitelistedExpression(ExploreGroupBy groupBy, String expression) {
        // act
        BuiltQuery query = ExploreQueryBuilder.build(groupBy, NO_FILTER);

        // assert & verify
        assertTrue(query.sql().contains(expression), query.sql());
        assertTrue(query.sql().contains("group by"));
        assertTrue(query.sql().contains("order by"));
        assertTrue(query.sql().contains("accounts.user_id = :userId"));
        assertTrue(query.sql().contains("transactions.deleted_at is null"));
        assertTrue(query.sql().contains("transactions.type in ('INCOME', 'EXPENSE')"));
    }

    @Test
    @DisplayName("should add one named parameter per filter and keep every value out of the SQL")
    void shouldKeepValuesOutOfTheSql() {
        // arrange
        ExploreFilter a = new ExploreFilter(List.of(11L, 12L), 21L, false, 31L, false, 41L, false, new BigDecimal("5"), new BigDecimal("500"));
        ExploreFilter b = new ExploreFilter(List.of(91L, 92L), 22L, false, 32L, false, 42L, false, new BigDecimal("6"), new BigDecimal("600"));

        // act
        BuiltQuery first = ExploreQueryBuilder.build(ExploreGroupBy.CATEGORY, a);
        BuiltQuery second = ExploreQueryBuilder.build(ExploreGroupBy.CATEGORY, b);

        // assert & verify
        assertEquals(first.sql(), second.sql());
        assertEquals(List.of(11L, 12L), first.params().get("accountIds"));
        assertEquals(21L, first.params().get("categoryId"));
        assertEquals(31L, first.params().get("merchantId"));
        assertEquals(41L, first.params().get("tagId"));
        assertEquals(new BigDecimal("5"), first.params().get("minAmount"));
        assertEquals(new BigDecimal("500"), first.params().get("maxAmount"));
        assertFalse(first.sql().contains("21"));
        assertFalse(first.sql().contains("500"));
    }

    @Test
    @DisplayName("should add no parameters and no conditions for an empty filter")
    void shouldAddNothingForEmptyFilter() {
        // act
        BuiltQuery query = ExploreQueryBuilder.build(ExploreGroupBy.MONTH, NO_FILTER);

        // assert & verify
        assertTrue(query.params().isEmpty());
        assertFalse(query.sql().contains(":accountIds"));
        assertFalse(query.sql().contains(":categoryId"));
    }

    @Test
    @DisplayName("should expand a parent category filter to its subcategories")
    void shouldExpandParentCategoryFilter() {
        // act
        BuiltQuery query = ExploreQueryBuilder.build(ExploreGroupBy.SUBCATEGORY,
                new ExploreFilter(List.of(), 7L, false, null, false, null, false, null, null));

        // assert & verify
        assertTrue(query.sql().contains("(transactions.category_id = :categoryId or categories.parent_id = :categoryId)"));
    }

    @Test
    @DisplayName("should express uncategorized, no merchant and no tag as is-null conditions")
    void shouldExpressNoneAsIsNull() {
        // act
        BuiltQuery query = ExploreQueryBuilder.build(ExploreGroupBy.ACCOUNT,
                new ExploreFilter(List.of(), null, true, null, true, null, true, null, null));

        // assert & verify
        assertTrue(query.sql().contains("transactions.category_id is null"));
        assertTrue(query.sql().contains("transactions.merchant_id is null"));
        assertTrue(query.sql().contains("not exists (select 1 from transaction_tags"));
        assertTrue(query.params().isEmpty());
    }

    @Test
    @DisplayName("should filter by a tag with an exists subquery so a transaction is not duplicated")
    void shouldFilterByTagWithExists() {
        // act
        BuiltQuery query = ExploreQueryBuilder.build(ExploreGroupBy.CATEGORY,
                new ExploreFilter(List.of(), null, false, null, false, 3L, false, null, null));

        // assert & verify
        assertTrue(query.sql().contains("exists (select 1 from transaction_tags"));
        assertTrue(query.sql().contains("transaction_tags.tag_id = :tagId"));
    }

    @Test
    @DisplayName("should build the excluded-count query over transfers and adjustments with the same filters")
    void shouldBuildExcludedCountQuery() {
        // act
        BuiltQuery query = ExploreQueryBuilder.buildExcludedCount(
                new ExploreFilter(List.of(1L), null, false, null, false, null, false, null, null));

        // assert & verify
        assertTrue(query.sql().startsWith("select count(*)"));
        assertTrue(query.sql().contains("transactions.type not in ('INCOME', 'EXPENSE')"));
        assertTrue(query.sql().contains("accounts.id in (:accountIds)"));
    }

    @Test
    @DisplayName("should build the transaction list newest first with the same filters, a types list and paging")
    void shouldBuildTransactionList() {
        // act
        BuiltQuery query = ExploreQueryBuilder.buildTransactionList(
                new ExploreFilter(List.of(1L), 7L, false, null, false, null, false, null, null));

        // assert & verify
        assertTrue(query.sql().contains("merchants.name as merchant_name"));
        assertTrue(query.sql().contains("transactions.type in (:types)"));
        assertTrue(query.sql().contains("order by transactions.date desc, transactions.id desc"));
        assertTrue(query.sql().contains("limit :limit offset :offset"));
        assertTrue(query.sql().contains("accounts.id in (:accountIds)"));
        assertTrue(query.sql().contains("accounts.user_id = :userId"));
        assertTrue(query.sql().contains("transactions.deleted_at is null"));
        assertEquals(7L, query.params().get("categoryId"));
    }

    @Test
    @DisplayName("should build the transaction count with the same conditions and no paging")
    void shouldBuildTransactionCount() {
        // act
        BuiltQuery query = ExploreQueryBuilder.buildTransactionCount(ExploreFilter.none());

        // assert & verify
        assertTrue(query.sql().startsWith("select count(*)"));
        assertTrue(query.sql().contains("transactions.type in (:types)"));
        assertFalse(query.sql().contains("limit"));
    }
}
