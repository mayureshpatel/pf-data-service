package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantTotalRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.query.MerchantQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MerchantRepository implements JdbcRepository<Merchant, Long> {

    private final JdbcClient jdbcClient;
    private final MerchantRowMapper rowMapper;
    private final MerchantTotalRowMapper merchantTotalRowMapper;

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

    public List<MerchantBreakdownDto> findMerchantTotals(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return jdbcClient.sql(MerchantQueries.FIND_MERCHANT_TOTALS)
                .param("userId", userId)
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query(merchantTotalRowMapper)
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
