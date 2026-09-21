package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantDescriptionLink;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MerchantDescriptionLinkRowMapper extends JdbcMapperUtils implements RowMapper<MerchantDescriptionLink> {

    @Override
    public MerchantDescriptionLink mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return MerchantDescriptionLink.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .merchantId(rs.getLong("merchant_id"))
                .description(rs.getString("description"))
                .normalizedDescription(rs.getString("normalized_description"))
                .audit(getAuditColumns(rs, "", getAvailableColumns(rs)))
                .build();
    }
}
