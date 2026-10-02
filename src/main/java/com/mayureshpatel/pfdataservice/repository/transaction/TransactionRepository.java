package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.CategoryTransactionsDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRowMapper;
import com.mayureshpatel.pfdataservice.repository.tag.mapper.TagRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.CategoryTransactionsRowMapper;
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

/**
 * JDBC-backed persistence for {@link Transaction} -- CRUD, CSV-import batch inserts, pagination/
 * filtering, and the handful of aggregate lookups too small to warrant their own repository.
 * Dashboard/Reports' own larger aggregation queries live in
 * {@link TransactionReportRepository} instead (PF-809: split out once this class's CBO grew past
 * PMD's threshold). Tags are fetched and attached separately in Java rather than joined (see
 * {@link #findTagsByTransactionIds}), and every method taking a {@link LocalDate} converts it to
 * an explicit UTC {@link OffsetDateTime} before it reaches SQL -- see the {@link #UTC_ZONE} field
 * comment for why.
 */
@Repository("jdbcTransactionRepository")
@RequiredArgsConstructor
public class TransactionRepository implements JdbcRepository<Transaction, Long>, SoftDeleteSupport {

    // PF-828: every method below that takes a LocalDate binds it as an explicit UTC
    // OffsetDateTime before it reaches SQL, rather than letting a bare LocalDate parameter get
    // implicitly cast using the database session's timezone (America/New_York in production --
    // see application.yml). Same root cause as TransactionSpecification's date-range filtering.
    private static final ZoneOffset UTC_ZONE = ZoneOffset.UTC;
    private static final String PARAM_START_DATE = "startDate";
    private static final String PARAM_END_DATE = "endDate";
    private static final String PARAM_ACCOUNT_ID = "accountId";
    private static final String PARAM_CATEGORY_ID = "categoryId";
    private static final String PARAM_AMOUNT = "amount";
    private static final String PARAM_DATE = "date";
    private static final String PARAM_DESCRIPTION = "description";
    private static final String PARAM_TYPE = "type";

    private final JdbcClient jdbcClient;
    private final TransactionDetailRowMapper rowMapper;
    private final CategoryTransactionsRowMapper categoryTransactionsDtoMapper;
    private final CategoryRowMapper categoryRowMapper;
    private final MerchantRowMapper merchantRowMapper;

    @Override
    public Optional<Transaction> findById(Long id) {
        throw new UnsupportedOperationException("Use findById with userId");
    }

    @Override
    public Optional<Transaction> findById(Long id, Long userId) {
        return jdbcClient.sql(TransactionQueries.FIND_BY_ID_WITH_DETAILS)
                .param("id", id)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Transaction> findAll() {
        return jdbcClient.sql(TransactionQueries.FIND_ALL)
                .query(rowMapper)
                .list();
    }

    /**
     * Every non-deleted transaction for a user, fully hydrated (account, category, merchant), most
     * recent first. Unlike {@link #findAll(TransactionSpecification.FilterResult, Pageable)}, this
     * has no filtering or pagination -- used where the full set is genuinely needed.
     *
     * @param userId the owning user's id
     * @return every non-deleted transaction the user has
     */
    public List<Transaction> findByUserId(Long userId) {
        return jdbcClient.sql(TransactionQueries.FIND_BY_USER_ID)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .list();
    }

    /**
     * Fetches an account's transactions in a date window for CSV-import duplicate detection --
     * candidates are compared against incoming rows in Java, not matched via SQL, so this simply
     * narrows the candidate set to a plausible range rather than doing exact matching itself.
     *
     * @param accountId the account being imported into
     * @param startDate the inclusive window start
     * @param endDate   the inclusive window end
     * @return every transaction on the account within the window
     */
    public List<Transaction> findExistingForDuplicateCheck(Long accountId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return jdbcClient.sql(TransactionQueries.FIND_EXISTING_FOR_DUPLICATE_CHECK)
                .param(PARAM_ACCOUNT_ID, accountId)
                .param(PARAM_START_DATE, startDate)
                .param(PARAM_END_DATE, endDate)
                .query(rowMapper)
                .list();
    }

    /**
     * Inserts a single transaction from its raw create-request shape -- as opposed to
     * {@link #insert(Transaction)}, an unrelated overload (not an override of this one) for
     * inserting an already-resolved domain object. See that method's own doc for why the two
     * exist separately.
     *
     * @param request the transaction to create
     * @return the generated transaction id
     */
    public int insert(TransactionCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(TransactionQueries.INSERT)
                .param(PARAM_ACCOUNT_ID, request.getAccountId())
                .param(PARAM_CATEGORY_ID, request.getCategoryId())
                .param(PARAM_AMOUNT, request.getAmount())
                .param(PARAM_DATE, request.getTransactionDate())
                .param("postDate", request.getPostDate())
                .param(PARAM_DESCRIPTION, request.getDescription())
                .param(PARAM_TYPE, request.getType())
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
                .param(PARAM_ACCOUNT_ID, transaction.getAccount().getId())
                .param(PARAM_CATEGORY_ID, transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .param(PARAM_AMOUNT, transaction.getAmount())
                .param(PARAM_DATE, transaction.getTransactionDate())
                .param("postDate", transaction.getPostDate())
                .param(PARAM_DESCRIPTION, transaction.getDescription())
                .param(PARAM_TYPE, transaction.getType().name())
                .param("merchantId", transaction.getMerchant() != null ? transaction.getMerchant().getId() : null)
                .update(keyHolder);

        return keyHolder.getKey().intValue();
    }

    /**
     * Updates a transaction's editable fields, ownership-scoped via {@code userId}.
     *
     * @param userId      the requesting user's id
     * @param transaction the transaction's new field values, identified by {@code transaction.getId()}
     * @return the number of rows updated (0 or 1)
     */
    public int update(Long userId, Transaction transaction) {
        return jdbcClient.sql(TransactionQueries.UPDATE)
                .param("id", transaction.getId())
                .param(SqlParams.USER_ID, userId)
                .param(PARAM_CATEGORY_ID, transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .param(PARAM_AMOUNT, transaction.getAmount())
                .param(PARAM_DATE, transaction.getTransactionDate())
                .param("postDate", transaction.getPostDate())
                .param(PARAM_DESCRIPTION, transaction.getDescription())
                .param(PARAM_TYPE, transaction.getType().name())
                .param("merchantId", transaction.getMerchant() != null ? transaction.getMerchant().getId() : null)
                .param(PARAM_ACCOUNT_ID, transaction.getAccount() != null ? transaction.getAccount().getId() : null)
                .update();
    }

    /**
     * Bulk-inserts transactions (e.g. a CSV import batch) in chunks of 500 rows per statement via
     * {@link #insertChunk} -- one multi-row {@code INSERT} per chunk rather than one round-trip per
     * transaction, since CSV imports can easily be thousands of rows.
     *
     * @param requestList the transactions to create; {@code null} or empty is a no-op
     * @return the total number of rows inserted across all chunks
     */
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

    /**
     * Bulk form of {@link #update(Long, Transaction)} -- unlike {@link #insertAll}, this is a
     * plain per-row loop, not a batched multi-row statement (an {@code UPDATE} can't be
     * multi-valued the way an {@code INSERT} can).
     *
     * @param userId      the requesting user's id
     * @param requestList the transactions to update, each identified by its own id
     * @return the total number of rows updated
     */
    public Integer updateAll(Long userId, List<Transaction> requestList) {
        return requestList.stream()
                .map(t -> this.update(userId, t))
                .mapToInt(Integer::intValue).sum();
    }

    @Override
    public int deleteById(Long id, Long userId) {
        return jdbcClient.sql(TransactionQueries.DELETE_BY_ID)
                .param("id", id)
                .param(SqlParams.USER_ID, userId)
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

    /**
     * @param accountId the account to check
     * @return the number of non-deleted transactions on this account
     */
    public long countByAccountId(Long accountId) {
        return jdbcClient.sql(TransactionQueries.COUNT_BY_ACCOUNT_ID)
                .param(PARAM_ACCOUNT_ID, accountId)
                .query(Long.class)
                .single();
    }

    /**
     * @param categoryId the category to check
     * @return the number of non-deleted transactions (across all users) assigned this category
     */
    public long countByCategoryId(Long categoryId) {
        return jdbcClient.sql(TransactionQueries.COUNT_BY_CATEGORY_ID)
                .param(PARAM_CATEGORY_ID, categoryId)
                .query(Long.class)
                .single();
    }

    /**
     * Per-category transaction counts for a user (including each category's parent, for display
     * grouping) -- backs the Categories feature's per-category usage indicator.
     *
     * @param userId the owning user's id
     * @return one row per category the user has transactions in, most-used first
     */
    public List<CategoryTransactionsDto> getCountByCategory(Long userId) {
        return jdbcClient.sql(TransactionQueries.COUNT_BY_CATEGORY)
                .param(SqlParams.USER_ID, userId)
                .query(categoryTransactionsDtoMapper)
                .list();
    }

    /**
     * @param userId the owning user's id
     * @return every subcategory the user has at least one transaction assigned to, alphabetical
     */
    public List<Category> getCategoriesWithTransactions(Long userId) {
        return jdbcClient.sql(TransactionQueries.CATEGORIES_WITH_TRANSACTIONS)
                .param(SqlParams.USER_ID, userId)
                .query(categoryRowMapper)
                .list();
    }

    /**
     * @param userId the owning user's id
     * @return every merchant the user has at least one transaction assigned to, alphabetical
     */
    public List<Merchant> getMerchantsWithTransactions(Long userId) {
        return jdbcClient.sql(TransactionQueries.MERCHANTS_WITH_TRANSACTIONS)
                .param(SqlParams.USER_ID, userId)
                .query(merchantRowMapper)
                .list();
    }

    /**
     * @param userId    the owning user's id
     * @param startDate the inclusive start of the window
     * @return fully hydrated transactions since {@code startDate}, excluding all three transfer
     *         types, most recent first
     */
    public List<Transaction> findRecentNonTransferTransactions(Long userId, LocalDate startDate) {
        return jdbcClient.sql(TransactionQueries.FIND_RECENT_NON_TRANSFER)
                .param(SqlParams.USER_ID, userId)
                .param(PARAM_START_DATE, startDate.atStartOfDay(UTC_ZONE).toOffsetDateTime())
                .query(rowMapper)
                .list();
    }

    /**
     * PF-848: every {@code TRANSFER_IN} transaction on one of the user's credit-card accounts --
     * the exact set the old mis-typing heuristic (fixed by PF-829) could have produced, used to
     * find backfill candidates. Never matches a genuine {@code markAsTransfer()}-confirmed
     * transfer on a non-credit-card account.
     *
     * @param userId the owning user's id
     * @return the user's {@code TRANSFER_IN} transactions on credit-card accounts
     */
    public List<Transaction> findTransferInOnCreditCardAccounts(Long userId) {
        return jdbcClient.sql(TransactionQueries.FIND_TRANSFER_IN_ON_CREDIT_CARD_ACCOUNTS)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .list();
    }

    /**
     * Batch, ownership-scoped, fully hydrated lookup by id.
     *
     * @param userId the requesting user's id
     * @param ids    the transaction ids to fetch; {@code null} or empty returns an empty list
     * @return the matching transactions owned by {@code userId} (silently skips any id that
     *         doesn't exist or isn't owned by the user)
     */
    public List<Transaction> findAllById(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jdbcClient.sql(TransactionQueries.FIND_ALL_BY_IDS_WITH_DETAILS)
                .param("ids", ids)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .list();
    }

    /**
     * Plain per-row loop over {@link #deleteById(Long, Long)} -- transactions with a {@code null}
     * id (not yet persisted) are silently skipped rather than erroring.
     *
     * @param userId       the requesting user's id
     * @param transactions the transactions to delete
     */
    public void deleteAll(Long userId, List<Transaction> transactions) {
        transactions.forEach(t -> {
            if (t.getId() != null) deleteById(t.getId(), userId);
        });
    }

    /**
     * Net effect on an account's balance from every non-deleted transaction strictly after
     * {@code date}, computed directly in SQL. Note this trusts each transaction's raw stored
     * {@code amount} sign for income/expense (adds income, subtracts expense) rather than forcing
     * it via {@code abs()} the way {@link Transaction#getNetChange()} does in Java -- the two are
     * only guaranteed to agree if amounts are always stored consistent with their type, which
     * nothing at the persistence layer currently enforces.
     *
     * @param accountId the account to sum
     * @param date      the exclusive lower bound
     * @return the net flow since {@code date}, or zero if there are no matching transactions
     */
    public BigDecimal getNetFlowAfterDate(Long accountId, LocalDate date) {
        return jdbcClient.sql(TransactionQueries.GET_NET_FLOW_AFTER_DATE)
                .param(PARAM_ACCOUNT_ID, accountId)
                .param(PARAM_DATE, date.atStartOfDay(UTC_ZONE).toOffsetDateTime())
                .query(BigDecimal.class)
                .optional()
                .orElse(BigDecimal.ZERO);
    }

    /**
     * @param userId    the owning user's id
     * @param startDate the inclusive start of the window
     * @return fully hydrated expense transactions since {@code startDate}, most recent first
     */
    public List<Transaction> findExpensesSince(Long userId, LocalDate startDate) {
        return jdbcClient.sql(TransactionQueries.FIND_EXPENSES_SINCE)
                .param(SqlParams.USER_ID, userId)
                .param(PARAM_START_DATE, startDate.atStartOfDay(UTC_ZONE).toOffsetDateTime())
                .query(rowMapper)
                .list();
    }

    /**
     * The main transaction-list query: filtered (via {@code filter}, built by
     * {@link TransactionSpecification}), sorted (whitelisted column mapping below -- an
     * unrecognized {@code pageable} sort property silently falls back to date-descending rather
     * than erroring), and paginated. Tags are fetched separately per page and attached afterward
     * (see {@link #findTagsByTransactionIds}), not joined into the main query.
     *
     * @param filter   the WHERE clause and bind parameters to apply
     * @param pageable the requested page, size, and sort
     * @return the requested page of matching transactions, each with its tags attached
     */
    public Page<Transaction> findAll(TransactionSpecification.FilterResult filter, Pageable pageable) {
        String baseFrom = "from transactions " +
                TransactionQueries.ENRICHED_JOINS + " " +
                "where " + filter.whereClause();

        long total = jdbcClient.sql("select count(*) " + baseFrom)
                .params(filter.parameters())
                .query(Long.class)
                .single();

        String sortClause = resolveSortClause(pageable);

        String pageSql = "select " + TransactionQueries.ENRICHED_COLUMNS + " " + baseFrom + sortClause +
                " limit :limit offset :offset";

        Map<String, Object> params = new HashMap<>(filter.parameters());
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Transaction> content = jdbcClient.sql(pageSql)
                .params(params)
                .query(rowMapper)
                .list();

        return new PageImpl<>(attachTags(content), pageable, total);
    }

    /**
     * Resolves the requested sort into a real SQL {@code order by} clause, falling back to
     * most-recent-first when nothing's requested (PF-809: extracted from {@link #findAll}).
     */
    private String resolveSortClause(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            return " order by transactions.date desc";
        }

        Sort.Order order = pageable.getSort().iterator().next();
        String col = resolveSortColumn(order.getProperty());
        String direction = order.getDirection().isAscending() ? "asc" : "desc";
        return " order by " + col + " " + direction;
    }

    /**
     * Maps a page-request's public sort property name to its real, qualified SQL column
     * (PF-809: extracted from {@link #resolveSortClause}).
     */
    private String resolveSortColumn(String property) {
        return switch (property) {
            case "date" -> "transactions.date";
            case "description" -> "transactions.description";
            case "merchant.name" -> "merchants.name";
            case "category.name" -> "categories.name";
            case "amount" -> "transactions.amount";
            case "type" -> "transactions.type";
            case "account.name" -> "accounts.name";
            default -> "transactions.date";
        };
    }

    /**
     * Attaches each transaction's tags, fetched in one batched query rather than one per row
     * (PF-809: extracted from {@link #findAll}; see {@link #findTagsByTransactionIds}).
     */
    private List<Transaction> attachTags(List<Transaction> content) {
        List<Long> transactionIds = content.stream().map(Transaction::getId).toList();
        Map<Long, List<Tag>> tagsByTransactionId = findTagsByTransactionIds(transactionIds);
        // plain loop, not stream().map() -- Transaction's @SuperBuilder toBuilder() return type
        // doesn't unify cleanly through a method-reference/lambda type witness in a stream here
        List<Transaction> withTags = new java.util.ArrayList<>(content.size());
        for (Transaction t : content) {
            withTags.add(t.toBuilder().tags(tagsByTransactionId.getOrDefault(t.getId(), List.of())).build());
        }
        return withTags;
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
}
