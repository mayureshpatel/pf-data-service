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

    public List<Merchant> findAllByCleanName(String cleanName) {
        return jdbcClient.sql(MerchantQueries.FIND_ALL_BY_CLEAN_NAME)
                .param("cleanName", cleanName)
                .query(rowMapper)
                .list();
    }

    public List<Merchant> findAllByCleanNameLike(String cleanName) {
        return jdbcClient.sql(MerchantQueries.FIND_ALL_BY_CLEAN_NAME_LIKE)
                .param("cleanName", cleanName)
                .query(rowMapper)
                .list();
    }

    public Optional<Merchant> findByOriginalNameAndUserId(String originalName, Long userId) {
        return jdbcClient.sql(MerchantQueries.FIND_BY_ORIGINAL_NAME_AND_USER_ID)
                .param("originalName", originalName)
                .param("userId", userId)
                .query(rowMapper)
                .optional();
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

    public int update(MerchantUpdateRequest request, Long userId) {
        return jdbcClient.sql(MerchantQueries.UPDATE)
                .param("name", request.getCleanName())
                .param("id", request.getId())
                .update();
    }

    public int delete(Long id) {
        return jdbcClient.sql(MerchantQueries.DELETE)
                .param("id", id)
                .update();
    }
}
