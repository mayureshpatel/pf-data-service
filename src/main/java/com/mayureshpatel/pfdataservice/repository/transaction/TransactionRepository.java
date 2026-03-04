package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.transaction.CategoryTransactionsDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.CategoryBreakdownRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.CategoryTransactionsRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.TransactionDetailRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.TransactionRowMapper;
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
import java.util.List;
import java.util.Optional;

@Repository("jdbcTransactionRepository")
@RequiredArgsConstructor
public class TransactionRepository implements JdbcRepository<Transaction, Long>, SoftDeleteSupport {

    private final JdbcClient jdbcClient;
    private final TransactionRowMapper rowMapper;
    private final TransactionDetailRowMapper detailRowMapper;
    private final CategoryBreakdownRowMapper categoryBreakdownRowMapper;
    private final CategoryTransactionsRowMapper categoryTransactionsDtoMapper;
    private final CategoryRowMapper categoryRowMapper;
    private final MerchantRowMapper merchantRowMapper;

    @Override
    public Optional<Transaction> findById(Long id) {
        return jdbcClient.sql(TransactionQueries.FIND_BY_ID_WITH_DETAILS)
                .param("id", id)
                .query(detailRowMapper)
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

    public boolean existsByAccountIdAndDateAndAmountAndDescriptionAndType(
            Long accountId,
            OffsetDateTime transactionDate,
            BigDecimal amount,
            String description,
            TransactionType type
    ) {
        return jdbcClient.sql(TransactionQueries.FIND_BY_ACCOUNT_ID_AND_DATE_AND_AMOUNT_AND_DESCRIPTION_AND_TYPE)
                .param("accountId", accountId)
                .param("transactionDate", transactionDate)
                .param("amount", amount)
                .param("description", description)
                .param("type", type.name())
                .query(rowMapper)
                .optional().isPresent();
    }

    public int insert(TransactionCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        return jdbcClient.sql(TransactionQueries.INSERT)
                .param("accountId", request.getAccountId())
                .param("categoryId", request.getCategoryId())
                .param("amount", request.getAmount())
                .param("date", request.getTransactionDate())
                .param("postDate", request.getPostDate())
                .param("description", request.getDescription())
                .param("type", request.getType())
                .param("merchantId", request.getMerchantId())
                .update(keyHolder);
    }

    public int update(TransactionUpdateRequest request) {
        return jdbcClient.sql(TransactionQueries.UPDATE)
                .param("id", request.getId())
                .param("categoryId", request.getCategoryId())
                .param("amount", request.getAmount())
                .param("date", request.getTransactionDate())
                .param("postDate", request.getPostDate())
                .param("description", request.getDescription())
                .param("type", request.getType())
                .param("merchantId", request.getMerchantId())
                .update();
    }

    public Integer insertAll(List<TransactionCreateRequest> requestList) {
        return requestList.stream()
                .map(this::insert)
                .mapToInt(Integer::intValue).sum();
    }

    public Integer updateAll(List<TransactionUpdateRequest> requestList) {
        return requestList.stream()
                .map(this::update)
                .mapToInt(Integer::intValue).sum();
    }

    public Integer updateAllT(List<Transaction> requestList) {
        return requestList.stream()
                .map(this::update)
                .mapToInt(Integer::intValue).sum();
    }

    @Override
    public int deleteById(Long id) {
        return jdbcClient.sql(TransactionQueries.DELETE_BY_ID)
                .param("id", id)
                .update();
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
                .param("startDate", startDate)
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

    public List<Transaction> findRecentNonTransferTransactions(Long userId, LocalDate startDate) {
        return jdbcClient.sql(TransactionQueries.FIND_RECENT_NON_TRANSFER)
                .param("userId", userId)
                .param("startDate", startDate)
                .query(rowMapper)
                .list();
    }

    public List<Transaction> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jdbcClient.sql(TransactionQueries.FIND_ALL_BY_IDS_WITH_DETAILS)
                .param("ids", ids)
                .query(detailRowMapper)
                .list();
    }

    public List<Transaction> findAllByIdWithAccountAndUser(List<Long> ids) {
        return findAllById(ids);
    }

    public void deleteAll(List<Transaction> transactions) {
        transactions.forEach(t -> {
            if (t.getId() != null) deleteById(t.getId());
        });
    }

    public long countByIdInAndAccount_User_Id(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) return 0;
        return findAllById(ids).stream()
                .filter(t -> t.getAccount() != null
                        && t.getAccount().getUserId() != null
                        && userId.equals(t.getAccount().getUserId()))
                .count();
    }

    public BigDecimal getNetFlowAfterDate(Long accountId, LocalDate date) {
        return jdbcClient.sql(TransactionQueries.GET_NET_FLOW_AFTER_DATE)
                .param("accountId", accountId)
                .param("date", date)
                .query(BigDecimal.class)
                .optional()
                .orElse(BigDecimal.ZERO);
    }

    public List<Transaction> findExpensesSince(Long userId, LocalDate startDate) {
        return jdbcClient.sql(TransactionQueries.FIND_EXPENSES_SINCE)
                .param("userId", userId)
                .param("startDate", startDate)
                .query(rowMapper)
                .list();
    }

    public Page<Transaction> findAll(TransactionSpecification.FilterResult filter, Pageable pageable) {
        String baseFrom = "FROM transactions t " +
                TransactionQueries.ENRICHED_JOINS + " " +
                "WHERE " + filter.whereClause();

        long total = jdbcClient.sql("SELECT COUNT(*) " + baseFrom)
                .params(filter.parameters())
                .query(Long.class)
                .single();

        String sortClause = " ORDER BY t.date DESC";
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String col = switch (order.getProperty()) {
                case "date" -> "t.date";
                case "description" -> "t.description";
                case "merchant.cleanName" -> "m.clean_name";
                case "category.name" -> "c.name";
                case "amount" -> "t.amount";
                case "type" -> "t.type";
                default -> "t.date";
            };
            sortClause = " ORDER BY " + col + " " + order.getDirection();
        }

        String pageSql = "SELECT " + TransactionQueries.ENRICHED_COLUMNS + " " + baseFrom + sortClause +
                " LIMIT " + pageable.getPageSize() + " OFFSET " + pageable.getOffset();

        List<Transaction> content = jdbcClient.sql(pageSql)
                .params(filter.parameters())
                .query(detailRowMapper)
                .list();

        return new PageImpl<>(content, pageable, total);
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
