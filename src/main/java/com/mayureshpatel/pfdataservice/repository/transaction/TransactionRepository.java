package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.report.CategoryReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.MonthlyReportDataDto;
import com.mayureshpatel.pfdataservice.dto.transaction.CategoryTransactionsDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRowMapper;
import com.mayureshpatel.pfdataservice.repository.tag.mapper.TagRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.CategoryBreakdownRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.CategoryReportDataRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.CategoryTransactionsRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.MonthlyReportDataRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.TransactionDetailRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.query.TransactionQueries;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository("jdbcTransactionRepository")
@RequiredArgsConstructor
public class TransactionRepository implements JdbcRepository<Transaction, Long>, SoftDeleteSupport {

    // PF-828: every method below that takes a LocalDate binds it as an explicit UTC
    // OffsetDateTime before it reaches SQL, rather than letting a bare LocalDate parameter get
    // implicitly cast using the database session's timezone (America/New_York in production --
    // see application.yml). Same root cause as TransactionSpecification's date-range filtering.
    private static final ZoneOffset UTC_ZONE = ZoneOffset.UTC;

    private final JdbcClient jdbcClient;
    private final TransactionDetailRowMapper rowMapper;
    private final CategoryBreakdownRowMapper categoryBreakdownRowMapper;
    private final CategoryTransactionsRowMapper categoryTransactionsDtoMapper;
    private final CategoryRowMapper categoryRowMapper;
    private final MerchantRowMapper merchantRowMapper;
    private final CategoryReportDataRowMapper categoryReportDataRowMapper;
    private final MonthlyReportDataRowMapper monthlyReportDataRowMapper;

    @Override
    public Optional<Transaction> findById(Long id) {
        throw new UnsupportedOperationException("Use findById with userId");
    }

    @Override
    public Optional<Transaction> findById(Long id, Long userId) {
        return jdbcClient.sql(TransactionQueries.FIND_BY_ID_WITH_DETAILS)
                .param("id", id)
                .param("userId", userId)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Transaction> findAll() {
        return jdbcClient.sql(TransactionQueries.FIND_ALL)
                .query(rowMapper)
                .list();
    }

    public List<Transaction> findByUserId(Long userId) {
        return jdbcClient.sql(TransactionQueries.FIND_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public List<CategoryBreakdownDto> findCategoryTotals(Long userId, OffsetDateTime start, OffsetDateTime end) {
        return jdbcClient.sql(TransactionQueries.FIND_CATEGORY_TOTALS)
                .param("userId", userId)
                .param("startDate", start)
                .param("endDate", end)
                .query(categoryBreakdownRowMapper)
                .list();
    }

    /**
     * PF-823: Reports' Categories tab data for the given range, aggregated fully server-side --
     * no row cap, unlike the client-side approach it replaces.
     */
    public List<CategoryReportDataDto> findCategoryReportData(Long userId, OffsetDateTime start, OffsetDateTime end) {
        return jdbcClient.sql(TransactionQueries.FIND_CATEGORY_REPORT_DATA)
                .param("userId", userId)
                .param("startDate", start)
                .param("endDate", end)
                .query(categoryReportDataRowMapper)
                .list();
    }

    /**
     * PF-823: Reports' Cash Flow tab data for the given range, one row per month with income and
     * expense already pivoted side by side.
     */
    public List<MonthlyReportDataDto> findMonthlyIncomeExpense(Long userId, OffsetDateTime start, OffsetDateTime end) {
        return jdbcClient.sql(TransactionQueries.FIND_MONTHLY_INCOME_EXPENSE)
                .param("userId", userId)
                .param("startDate", start)
                .param("endDate", end)
                .query(monthlyReportDataRowMapper)
                .list();
    }

    public List<Transaction> findExistingForDuplicateCheck(Long accountId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return jdbcClient.sql(TransactionQueries.FIND_EXISTING_FOR_DUPLICATE_CHECK)
                .param("accountId", accountId)
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query(rowMapper)
                .list();
    }

    public int insert(TransactionCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(TransactionQueries.INSERT)
                .param("accountId", request.getAccountId())
                .param("categoryId", request.getCategoryId())
                .param("amount", request.getAmount())
                .param("date", request.getTransactionDate())
                .param("postDate", request.getPostDate())
                .param("description", request.getDescription())
                .param("type", request.getType())
                .param("merchantId", request.getMerchantId())
                .update(keyHolder);

        return keyHolder.getKey().intValue();
    }

    /**
     * Inserts a fully-resolved transaction (account, auto-guessed/explicit category, and
     * auto-matched merchant already set by the service layer). The {@code T}-typed
     * {@code JdbcRepository.insert(T)} default can't be overridden by {@link
     * #insert(TransactionCreateRequest)} above -- that's a different, unrelated overload, not an
     * override -- so callers passing a {@link Transaction} were silently hitting the interface's
     * throwing default instead of ever reaching real SQL.
     *
     * @param transaction the resolved transaction to insert
     * @return the newly inserted transaction's generated id
     */
    @Override
    public int insert(Transaction transaction) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(TransactionQueries.INSERT)
                .param("accountId", transaction.getAccount().getId())
                .param("categoryId", transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .param("amount", transaction.getAmount())
                .param("date", transaction.getTransactionDate())
                .param("postDate", transaction.getPostDate())
                .param("description", transaction.getDescription())
                .param("type", transaction.getType().name())
                .param("merchantId", transaction.getMerchant() != null ? transaction.getMerchant().getId() : null)
                .update(keyHolder);

        return keyHolder.getKey().intValue();
    }

    public int update(Long userId, Transaction transaction) {
        return jdbcClient.sql(TransactionQueries.UPDATE)
                .param("id", transaction.getId())
                .param("userId", userId)
                .param("categoryId", transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .param("amount", transaction.getAmount())
                .param("date", transaction.getTransactionDate())
                .param("postDate", transaction.getPostDate())
                .param("description", transaction.getDescription())
                .param("type", transaction.getType().name())
                .param("merchantId", transaction.getMerchant() != null ? transaction.getMerchant().getId() : null)
                .param("accountId", transaction.getAccount() != null ? transaction.getAccount().getId() : null)
                .update();
    }

    public Integer insertAll(List<TransactionCreateRequest> requestList) {
        if (requestList == null || requestList.isEmpty()) {
            return 0;
        }

        int totalInserted = 0;
        int batchSize = 500;

        for (int i = 0; i < requestList.size(); i += batchSize) {
            int toIndex = Math.min(i + batchSize, requestList.size());
            List<TransactionCreateRequest> chunk = requestList.subList(i, toIndex);
            totalInserted += insertChunk(chunk);
        }

        return totalInserted;
    }

    private int insertChunk(List<TransactionCreateRequest> chunk) {
        StringBuilder sql = new StringBuilder("""
            insert into transactions
                (amount, date, post_date, description, merchant_id, type, account_id, category_id, created_at, updated_at)
            values
            """);

        Map<String, Object> params = new HashMap<>();

        for (int i = 0; i < chunk.size(); i++) {
            TransactionCreateRequest req = chunk.get(i);
            
            sql.append(String.format("( :amount_%d, :date_%d, :postDate_%d, :description_%d, :merchantId_%d, :type_%d, :accountId_%d, :categoryId_%d, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP )", 
                i, i, i, i, i, i, i, i));
            
            if (i < chunk.size() - 1) {
                sql.append(",\n");
            }

            params.put("amount_" + i, req.getAmount());
            params.put("date_" + i, req.getTransactionDate());
            params.put("postDate_" + i, req.getPostDate());
            params.put("description_" + i, req.getDescription());
            params.put("merchantId_" + i, req.getMerchantId());
            params.put("type_" + i, req.getType());
            params.put("accountId_" + i, req.getAccountId());
            params.put("categoryId_" + i, req.getCategoryId());
        }

        return jdbcClient.sql(sql.toString())
                .params(params)
                .update();
    }

    public Integer updateAll(Long userId, List<Transaction> requestList) {
        return requestList.stream()
                .map(t -> this.update(userId, t))
                .mapToInt(Integer::intValue).sum();
    }

    @Override
    public int deleteById(Long id, Long userId) {
        return jdbcClient.sql(TransactionQueries.DELETE_BY_ID)
                .param("id", id)
                .param("userId", userId)
                .update();
    }

    @Override
    public int deleteById(Long id) {
        throw new UnsupportedOperationException("Use deleteById with userId");
    }

    @Override
    public long count() {
        return jdbcClient.sql(TransactionQueries.COUNT)
                .query(Long.class)
                .single();
    }

    public long countByAccountId(Long accountId) {
        return jdbcClient.sql(TransactionQueries.COUNT_BY_ACCOUNT_ID)
                .param("accountId", accountId)
                .query(Long.class)
                .single();
    }

    public long countByCategoryId(Long categoryId) {
        return jdbcClient.sql(TransactionQueries.COUNT_BY_CATEGORY_ID)
                .param("categoryId", categoryId)
                .query(Long.class)
                .single();
    }

    public List<CategoryTransactionsDto> getCountByCategory(Long userId) {
        return jdbcClient.sql(TransactionQueries.COUNT_BY_CATEGORY)
                .param("userId", userId)
                .query(categoryTransactionsDtoMapper)
                .list();
    }

    public List<Category> getCategoriesWithTransactions(Long userId) {
        return jdbcClient.sql(TransactionQueries.CATEGORIES_WITH_TRANSACTIONS)
                .param("userId", userId)
                .query(categoryRowMapper)
                .list();
    }

    public List<Merchant> getMerchantsWithTransactions(Long userId) {
        return jdbcClient.sql(TransactionQueries.MERCHANTS_WITH_TRANSACTIONS)
                .param("userId", userId)
                .query(merchantRowMapper)
                .list();
    }

    public List<Object[]> findMonthlySums(Long userId, LocalDate startDate) {
        return jdbcClient.sql(TransactionQueries.FIND_MONTHLY_SUMS)
                .param("userId", userId)
                .param("startDate", startDate.atStartOfDay(UTC_ZONE).toOffsetDateTime())
                .query((rs, rowNum) -> new Object[]{
                        rs.getInt("year"),
                        rs.getInt("month"),
                        rs.getString("type"),
                        rs.getBigDecimal("total")
                })
                .list();
    }

    public BigDecimal getUncategorizedExpenseTotals(Long userId) {
        return jdbcClient.sql(TransactionQueries.GET_UNCATEGORIZED_EXPENSE_TOTALS)
                .param("userId", userId)
                .query(BigDecimal.class)
                .optional()
                .orElse(BigDecimal.ZERO);
    }

    public long getUncategorizedExpenseCount(Long userId) {
        return jdbcClient.sql(TransactionQueries.GET_UNCATEGORIZED_EXPENSE_COUNT)
                .param("userId", userId)
                .query(Long.class)
                .single();
    }

    public List<Transaction> findRecentNonTransferTransactions(Long userId, LocalDate startDate) {
        return jdbcClient.sql(TransactionQueries.FIND_RECENT_NON_TRANSFER)
                .param("userId", userId)
                .param("startDate", startDate.atStartOfDay(UTC_ZONE).toOffsetDateTime())
                .query(rowMapper)
                .list();
    }

    public List<Transaction> findTransferInOnCreditCardAccounts(Long userId) {
        return jdbcClient.sql(TransactionQueries.FIND_TRANSFER_IN_ON_CREDIT_CARD_ACCOUNTS)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public List<Transaction> findAllById(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jdbcClient.sql(TransactionQueries.FIND_ALL_BY_IDS_WITH_DETAILS)
                .param("ids", ids)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public void deleteAll(Long userId, List<Transaction> transactions) {
        transactions.forEach(t -> {
            if (t.getId() != null) deleteById(t.getId(), userId);
        });
    }

    public BigDecimal getNetFlowAfterDate(Long accountId, LocalDate date) {
        return jdbcClient.sql(TransactionQueries.GET_NET_FLOW_AFTER_DATE)
                .param("accountId", accountId)
                .param("date", date.atStartOfDay(UTC_ZONE).toOffsetDateTime())
                .query(BigDecimal.class)
                .optional()
                .orElse(BigDecimal.ZERO);
    }

    public List<Transaction> findExpensesSince(Long userId, LocalDate startDate) {
        return jdbcClient.sql(TransactionQueries.FIND_EXPENSES_SINCE)
                .param("userId", userId)
                .param("startDate", startDate.atStartOfDay(UTC_ZONE).toOffsetDateTime())
                .query(rowMapper)
                .list();
    }

    public Page<Transaction> findAll(TransactionSpecification.FilterResult filter, Pageable pageable) {
        String baseFrom = "from transactions " +
                TransactionQueries.ENRICHED_JOINS + " " +
                "where " + filter.whereClause();

        long total = jdbcClient.sql("select count(*) " + baseFrom)
                .params(filter.parameters())
                .query(Long.class)
                .single();

        String sortClause = " order by transactions.date desc";
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String col = switch (order.getProperty()) {
                case "date" -> "transactions.date";
                case "description" -> "transactions.description";
                case "merchant.name" -> "merchants.name";
                case "category.name" -> "categories.name";
                case "amount" -> "transactions.amount";
                case "type" -> "transactions.type";
                case "account.name" -> "accounts.name";
                default -> "transactions.date";
            };
            String direction = order.getDirection().isAscending() ? "asc" : "desc";
            sortClause = " order by " + col + " " + direction;
        }

        String pageSql = "select " + TransactionQueries.ENRICHED_COLUMNS + " " + baseFrom + sortClause +
                " limit :limit offset :offset";

        Map<String, Object> params = new HashMap<>(filter.parameters());
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Transaction> content = jdbcClient.sql(pageSql)
                .params(params)
                .query(rowMapper)
                .list();

        Map<Long, List<Tag>> tagsByTransactionId = findTagsByTransactionIds(
                content.stream().map(Transaction::getId).toList());
        // plain loop, not stream().map() -- Transaction's @SuperBuilder toBuilder() return type
        // doesn't unify cleanly through a method-reference/lambda type witness in a stream here
        List<Transaction> withTags = new java.util.ArrayList<>(content.size());
        for (Transaction t : content) {
            withTags.add(t.toBuilder().tags(tagsByTransactionId.getOrDefault(t.getId(), List.of())).build());
        }
        content = withTags;

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * PF-308: one query for a whole page of transactions, not one per row -- grouped by
     * transaction_id in Java afterward.
     */
    private Map<Long, List<Tag>> findTagsByTransactionIds(List<Long> transactionIds) {
        if (transactionIds.isEmpty()) {
            return Map.of();
        }
        return jdbcClient.sql(TransactionQueries.FIND_TAGS_BY_TRANSACTION_IDS)
                .param("transactionIds", transactionIds)
                .query((rs, rowNum) -> Map.entry(rs.getLong("transaction_id"), TagRowMapper.mapRow(rs, "")))
                .list()
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    public BigDecimal getSumByDateRange(Long userId, OffsetDateTime start, OffsetDateTime end, TransactionType type) {
        return jdbcClient.sql(TransactionQueries.GET_SUM_BY_DATE_RANGE)
                .param("userId", userId)
                .param("startDate", start)
                .param("endDate", end)
                .param("type", type.name())
                .query(BigDecimal.class)
                .optional()
                .orElse(BigDecimal.ZERO);

    }
}
