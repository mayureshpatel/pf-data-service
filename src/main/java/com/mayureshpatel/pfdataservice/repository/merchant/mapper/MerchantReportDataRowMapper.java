package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.dto.report.MerchantReportDataDto;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Maps {@code MerchantQueries.FIND_MERCHANT_REPORT_DATA}'s rows directly to columns rather than
 * delegating to {@link MerchantRowMapper}/{@code MerchantDtoMapper} -- {@link MerchantReportDataDto}
 * is a flat, purpose-built shape (see its own Javadoc for why its field names look like a
 * PF-841-era grouped row even though PF-845's merchants can no longer fragment).
 */
@Component
public class MerchantReportDataRowMapper extends JdbcMapperUtils implements RowMapper<MerchantReportDataDto> {

    @Override
    public MerchantReportDataDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MerchantReportDataDto(
                rs.getLong("representative_merchant_id"),
                rs.getString("display_name"),
                rs.getBigDecimal("total"),
                rs.getLong("txn_count"),
                toCategoryNames(rs.getArray("category_names"))
        );
    }

    /**
     * array_remove(array_agg(...), null) in the backing query already strips any null entry, so
     * this only needs to guard against the driver itself returning a null Array reference.
     */
    private List<String> toCategoryNames(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }
        return Arrays.asList((String[]) sqlArray.getArray());
    }
}
