package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MerchantTotalRowMapper extends JdbcMapperUtils implements RowMapper<MerchantBreakdownDto> {

    @Override
    public MerchantBreakdownDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        MerchantDto merchantDto = new MerchantDto(
                rs.getLong("merchant_id"),
                null,
                rs.getString("merchant_original_name"),
                rs.getString("merchant_clean_name")
        );

        return new MerchantBreakdownDto(merchantDto, rs.getBigDecimal("total"));
    }

}
