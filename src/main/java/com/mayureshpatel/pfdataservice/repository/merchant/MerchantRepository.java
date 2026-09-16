package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.report.MerchantReportDataDto;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantReportDataRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantTotalRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.query.MerchantQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Repository
@RequiredArgsConstructor
public class MerchantRepository implements JdbcRepository<Merchant, Long> {

    private static final String SORT_PROPERTY_ORIGINAL_NAME = "originalName";

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

    public List<Merchant> findAllByUserId(Long userId) {
        return jdbcClient.sql(MerchantQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    /**
     * User-scoped, paginated merchant search: matches {@code search} (case-insensitive,
     * substring) against either name column when provided, otherwise returns every merchant for
     * the page requested. Mirrors {@code TransactionRepository.findAll(FilterResult, Pageable)}'s
     * count-then-page-then-{@link PageImpl} shape.
     *
     * @param userId   the user id
     * @param search   an optional case-insensitive substring to match against clean/original name
     * @param pageable the requested page, size, and sort
     * @return the requested page of the user's merchants
     */
    public Page<Merchant> findAllByUserId(Long userId, String search, Pageable pageable) {
        boolean hasSearch = StringUtils.hasText(search);
        String searchParam = hasSearch ? "%" + search.trim() + "%" : null;

        long total = jdbcClient.sql(hasSearch ? MerchantQueries.COUNT_BY_USER_ID_AND_SEARCH : MerchantQueries.COUNT_BY_USER_ID)
                .param("userId", userId)
                .param("search", searchParam)
                .query(Long.class)
                .single();

        String sortColumn = "clean_name";
        String direction = "asc";
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            if (SORT_PROPERTY_ORIGINAL_NAME.equals(order.getProperty())) {
                sortColumn = "original_name";
            }
            direction = order.getDirection().isDescending() ? "desc" : "asc";
        }

        String baseSql = hasSearch ? MerchantQueries.FIND_PAGE_BY_USER_ID_AND_SEARCH : MerchantQueries.FIND_PAGE_BY_USER_ID;
        String orderClause = " order by " + sortColumn + " " + direction;

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
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
     * User-scoped, paginated distinct clean names (PF-842) -- backs the two-level clean-name
     * picker and the grouped Merchants view's outer rows. Mirrors
     * {@link #findAllByUserId(Long, String, Pageable)}'s count-then-page-then-{@link PageImpl}
     * shape, minus the sort-column branch (there's only one column to sort here).
     *
     * @param userId   the user id
     * @param search   an optional case-insensitive substring to match against clean name
     * @param pageable the requested page and size (sort is always by clean name, ascending)
     * @return the requested page of the user's distinct, non-blank clean names
     */
    public Page<String> findDistinctCleanNames(Long userId, String search, Pageable pageable) {
        boolean hasSearch = StringUtils.hasText(search);
        String searchParam = hasSearch ? "%" + search.trim() + "%" : null;

        long total = jdbcClient.sql(hasSearch
                        ? MerchantQueries.COUNT_DISTINCT_CLEAN_NAMES_BY_USER_ID_AND_SEARCH
                        : MerchantQueries.COUNT_DISTINCT_CLEAN_NAMES_BY_USER_ID)
                .param("userId", userId)
                .param("search", searchParam)
                .query(Long.class)
                .single();

        String baseSql = hasSearch
                ? MerchantQueries.FIND_DISTINCT_CLEAN_NAMES_BY_USER_ID_AND_SEARCH
                : MerchantQueries.FIND_DISTINCT_CLEAN_NAMES_BY_USER_ID;

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("search", searchParam);

        String limitOffsetClause = pageable.isPaged() ? " limit :limit offset :offset" : "";
        if (pageable.isPaged()) {
            params.put("limit", pageable.getPageSize());
            params.put("offset", pageable.getOffset());
        }

        List<String> content = jdbcClient.sql(baseSql + limitOffsetClause)
                .params(params)
                .query(String.class)
                .list();

        return new PageImpl<>(content, pageable, total);
    }

    public Optional<Merchant> findByIdAndUserId(Long id, Long userId) {
        return jdbcClient.sql(MerchantQueries.FIND_BY_ID_AND_USER_ID)
                .param("id", id)
                .param("userId", userId)
                .query(rowMapper)
                .optional();
    }

    public List<Merchant> findAllByCleanName(String cleanName) {
        return jdbcClient.sql(MerchantQueries.FIND_ALL_BY_CLEAN_NAME)
                .param("cleanName", cleanName)
                .query(rowMapper)
                .list();
    }

    /**
     * User-scoped clean-name lookup. Ordered by {@code id} so that if a user somehow already has
     * more than one merchant sharing a clean name (no DB uniqueness constraint on
     * {@code (user_id, clean_name)} -- only on {@code (user_id, original_name)}), callers picking
     * the first result get a stable, deterministic choice rather than whatever order Postgres
     * happens to return.
     */
    public List<Merchant> findAllByCleanNameAndUserId(String cleanName, Long userId) {
        return jdbcClient.sql(MerchantQueries.FIND_ALL_BY_CLEAN_NAME_AND_USER_ID)
                .param("cleanName", cleanName)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public List<Merchant> findAllByCleanNamesAndUserId(List<String> cleanNames, Long userId) {
        if (cleanNames == null || cleanNames.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql(MerchantQueries.FIND_ALL_BY_CLEAN_NAMES_AND_USER_ID)
                .param("cleanNames", cleanNames)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    /**
     * User-scoped, light-normalized {@code original_name} lookup -- the matching key
     * {@code MerchantService.findOrCreateMerchant} uses to decide whether a raw description
     * resolves to an existing merchant. Ordered by {@code id} for the same reason as
     * {@link #findAllByCleanNameAndUserId}: no uniqueness constraint on the normalized form
     * either (the DB's real unique index is on the raw, un-normalized {@code original_name}), so
     * callers picking the first result need a stable, deterministic choice.
     *
     * @param normalizedOriginalName the case-folded, whitespace-collapsed original name to match
     * @param userId                 the user id
     * @return every merchant whose {@code original_name} normalizes to the same value, oldest first
     */
    public List<Merchant> findAllByNormalizedOriginalNameAndUserId(String normalizedOriginalName, Long userId) {
        return jdbcClient.sql(MerchantQueries.FIND_ALL_BY_NORMALIZED_ORIGINAL_NAME_AND_USER_ID)
                .param("normalizedOriginalName", normalizedOriginalName)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    /**
     * Batch form of {@link #findAllByNormalizedOriginalNameAndUserId}, mirroring
     * {@link #findAllByCleanNamesAndUserId}'s empty-input guard.
     *
     * @param normalizedOriginalNames the case-folded, whitespace-collapsed original names to match
     * @param userId                  the user id
     * @return every merchant whose {@code original_name} normalizes to one of the given values
     */
    public List<Merchant> findAllByNormalizedOriginalNamesAndUserId(List<String> normalizedOriginalNames, Long userId) {
        if (normalizedOriginalNames == null || normalizedOriginalNames.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql(MerchantQueries.FIND_ALL_BY_NORMALIZED_ORIGINAL_NAMES_AND_USER_ID)
                .param("normalizedOriginalNames", normalizedOriginalNames)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public List<MerchantBreakdownDto> findMerchantTotals(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return jdbcClient.sql(MerchantQueries.FIND_MERCHANT_TOTALS)
                .param("userId", userId)
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
                .param("userId", userId)
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query(merchantReportDataRowMapper)
                .list();
    }

    public Long insert(MerchantCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(MerchantQueries.INSERT)
                .param("userId", request.getUserId())
                .param("originalName", request.getOriginalName())
                .param("name", request.getCleanName())
                .update(keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<Merchant> insertAllAndReturn(List<MerchantCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        return requests.stream().map(req -> {
            Long id = insert(req);
            return Merchant.builder()
                .id(id)
                .userId(req.getUserId())
                .originalName(req.getOriginalName())
                .cleanName(req.getCleanName())
                .build();
        }).toList();
    }

    public int update(MerchantUpdateRequest request, Long userId) {
        return jdbcClient.sql(MerchantQueries.UPDATE)
                .param("name", request.getCleanName())
                .param("id", request.getId())
                .param("userId", userId)
                .update();
    }

    /**
     * Deletes a merchant. User-scoped: unlike {@link #update}, which had a real, exposed IDOR
     * before PF-220 fixed it, nothing called this method at all until PF-222 (merchant merge)
     * became its first real caller -- scoping the SQL here from the start, rather than exposing
     * the same class of gap PF-220 had to fix after the fact.
     */
    public int delete(Long id, Long userId) {
        return jdbcClient.sql(MerchantQueries.DELETE)
                .param("id", id)
                .param("userId", userId)
                .update();
    }
}
