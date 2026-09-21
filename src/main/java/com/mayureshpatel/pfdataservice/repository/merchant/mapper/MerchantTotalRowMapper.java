package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps {@code MerchantQueries.FIND_MERCHANT_TOTALS}'s rows directly to columns rather than
 * delegating to {@link MerchantRowMapper}/{@code MerchantDtoMapper} -- {@link MerchantBreakdownDto}
 * is a flat, purpose-built shape (see its own Javadoc for why its field names look like a
 * PF-841-era grouped row even though PF-845's merchants can no longer fragment).
 */
@Component
public class MerchantTotalRowMapper extends JdbcMapperUtils implements RowMapper<MerchantBreakdownDto> {

    @Override
    public MerchantBreakdownDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MerchantBreakdownDto(
                rs.getLong("representative_merchant_id"),
                rs.getString("display_name"),
                rs.getBigDecimal("total")
        );
    }

}
