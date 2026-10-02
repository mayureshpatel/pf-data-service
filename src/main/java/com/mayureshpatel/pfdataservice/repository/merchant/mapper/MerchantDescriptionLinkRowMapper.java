package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantDescriptionLink;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a JDBC {@link ResultSet} row directly to a {@link MerchantDescriptionLink}. Unlike most of
 * this codebase's row mappers, this one has no prefixed/nested-embedding overload -- it's only
 * ever used to map this table's own rows directly, never composed into a richer joined result.
 */
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
