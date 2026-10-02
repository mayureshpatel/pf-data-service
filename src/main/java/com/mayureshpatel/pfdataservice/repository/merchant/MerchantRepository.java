package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.report.MerchantReportDataDto;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantReportDataRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantTotalRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.query.MerchantQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC-backed persistence for {@link Merchant}, including the report-facing spend-breakdown
 * queries ({@link #findMerchantTotals}/{@link #findMerchantReportData}). Post-PF-845, merchants
 * are plain user-owned entities with no fragmentation/clustering concept -- see
 * {@code MerchantQueries}'s own doc for the column-naming history that predates that change.
 */
@Repository
@RequiredArgsConstructor
public class MerchantRepository implements JdbcRepository<Merchant, Long> {

    private final JdbcClient jdbcClient;
    private final MerchantRowMapper rowMapper;
    private final MerchantTotalRowMapper merchantTotalRowMapper;
    private final MerchantReportDataRowMapper merchantReportDataRowMapper;

    @Override
    public Optional<Merchant> findById(Long aLong) {
        return jdbcClient.sql(MerchantQueries.FIND_BY_ID)
                .param("id", aLong)
                .query(rowMapper)
                .optional();
    }

    /**
     * Unpaginated form of {@link #findAllByUserId(Long, String, Pageable)} -- returns every one
     * of the user's merchants at once, with no search filter.
     *
     * @param userId the owning user's id
     * @return every merchant the user has
     */
    public List<Merchant> findAllByUserId(Long userId) {
        return jdbcClient.sql(MerchantQueries.FIND_ALL_BY_USER_ID)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .list();
    }

    /**
     * User-scoped, paginated merchant search: matches {@code search} (case-insensitive,
     * substring) against name or city when provided, otherwise returns every merchant for the
     * page requested. Always sorted by name -- there's only one name concept now, unlike the
     * original/clean-name split this replaced. Mirrors
     * {@code TransactionRepository.findAll(FilterResult, Pageable)}'s
     * count-then-page-then-{@link PageImpl} shape.
     *
     * @param userId   the user id
     * @param search   an optional case-insensitive substring to match against name/city
     * @param pageable the requested page, size, and sort direction
     * @return the requested page of the user's merchants
     */
    public Page<Merchant> findAllByUserId(Long userId, String search, Pageable pageable) {
        boolean hasSearch = StringUtils.hasText(search);
        String searchParam = hasSearch ? "%" + search.trim() + "%" : null;

        long total = jdbcClient.sql(hasSearch ? MerchantQueries.COUNT_BY_USER_ID_AND_SEARCH : MerchantQueries.COUNT_BY_USER_ID)
                .param(SqlParams.USER_ID, userId)
                .param("search", searchParam)
                .query(Long.class)
                .single();

        String direction = pageable.getSort().isSorted() && pageable.getSort().iterator().next().getDirection().isDescending()
                ? "desc" : "asc";

        String baseSql = hasSearch ? MerchantQueries.FIND_PAGE_BY_USER_ID_AND_SEARCH : MerchantQueries.FIND_PAGE_BY_USER_ID;
        String orderClause = " order by name " + direction;

        Map<String, Object> params = new HashMap<>();
        params.put(SqlParams.USER_ID, userId);
        params.put("search", searchParam);

        // Pageable.unpaged() (used by test setup that genuinely wants "every merchant") throws
        // UnsupportedOperationException from getPageSize()/getOffset() -- there's no limit/offset
        // to apply in that case, only the ordering.
        String limitOffsetClause = pageable.isPaged() ? " limit :limit offset :offset" : "";
        if (pageable.isPaged()) {
            params.put("limit", pageable.getPageSize());
            params.put("offset", pageable.getOffset());
        }

        List<Merchant> content = jdbcClient.sql(baseSql + orderClause + limitOffsetClause)
                .params(params)
                .query(rowMapper)
                .list();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * @param id     the merchant id
     * @param userId the requesting user's id
     * @return the merchant if it exists and is owned by {@code userId}, otherwise empty
     */
    public Optional<Merchant> findByIdAndUserId(Long id, Long userId) {
        return jdbcClient.sql(MerchantQueries.FIND_BY_ID_AND_USER_ID)
                .param("id", id)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .optional();
    }

    /**
     * Dashboard-facing spend-by-merchant breakdown for the given date range (expense transactions
     * only). See {@link #findMerchantReportData} for the Reports feature's richer equivalent.
     *
     * @param userId    the owning user's id
     * @param startDate the inclusive range start
     * @param endDate   the exclusive range end
     * @return one row per merchant with any spend in the range, unordered
     */
    public List<MerchantBreakdownDto> findMerchantTotals(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return jdbcClient.sql(MerchantQueries.FIND_MERCHANT_TOTALS)
                .param(SqlParams.USER_ID, userId)
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query(merchantTotalRowMapper)
                .list();
    }

    /**
     * PF-823: Reports' Merchants tab data for the given range, aggregated fully server-side --
     * no row cap, unlike the client-side approach it replaces.
     */
    public List<MerchantReportDataDto> findMerchantReportData(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return jdbcClient.sql(MerchantQueries.FIND_MERCHANT_REPORT_DATA)
                .param(SqlParams.USER_ID, userId)
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query(merchantReportDataRowMapper)
                .list();
    }

    /**
     * @param request the merchant to create
     * @return the generated merchant id
     */
    public Long insert(MerchantCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(MerchantQueries.INSERT)
                .param(SqlParams.USER_ID, request.getUserId())
                .param("name", request.getName())
                .param("city", request.getCity())
                .param("state", request.getState())
                .param("postalCode", request.getPostalCode())
                .param("country", request.getCountry())
                .update(keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * @param request the merchant's new field values
     * @param userId  the requesting user's id
     * @return the number of rows updated (0 or 1)
     */
    public int update(MerchantUpdateRequest request, Long userId) {
        return jdbcClient.sql(MerchantQueries.UPDATE)
                .param("name", request.getName())
                .param("city", request.getCity())
                .param("state", request.getState())
                .param("postalCode", request.getPostalCode())
                .param("country", request.getCountry())
                .param("id", request.getId())
                .param(SqlParams.USER_ID, userId)
                .update();
    }

    /**
     * Deletes a merchant, scoped to its owner in the query itself. {@code transactions.merchant_id}
     * is {@code ON DELETE SET NULL} and {@code merchant_description_links.merchant_id} is
     * {@code ON DELETE CASCADE} -- the database handles dependent cleanup, nothing further needed
     * here.
     */
    public int delete(Long id, Long userId) {
        return jdbcClient.sql(MerchantQueries.DELETE)
                .param("id", id)
                .param(SqlParams.USER_ID, userId)
                .update();
    }
}
