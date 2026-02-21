package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantTotalRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.query.MerchantQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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

    public List<MerchantBreakdownDto> findMerchantTotals(Long userId, int month, int year) {
        return jdbcClient.sql(MerchantQueries.FIND_MERCHANT_TOTALS)
                .param("userId", userId)
                .param("month", month)
                .param("year", year)
                .query(merchantTotalRowMapper)
                .list();
    }
}
