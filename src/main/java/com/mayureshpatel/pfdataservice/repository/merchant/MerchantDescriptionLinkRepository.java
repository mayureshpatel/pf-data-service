package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantDescriptionLink;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantDescriptionLinkRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.query.MerchantDescriptionLinkQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Remembers which raw transaction descriptions a user has linked to which merchant (PF-845) --
 * captured automatically whenever a human assigns a merchant to a transaction
 * ({@code TransactionService}/{@code TransactionImportService} never call {@link #upsert}
 * directly during import; only assignment does), and manageable explicitly via the merchant's own
 * description-links screen.
 */
@Repository
@RequiredArgsConstructor
public class MerchantDescriptionLinkRepository {

    private final JdbcClient jdbcClient;
    private final MerchantDescriptionLinkRowMapper rowMapper;

    /**
     * Batch-matches normalized descriptions against a user's existing links. A description with
     * no link simply isn't a key in the returned map -- absence means "no match," never an error.
     *
     * @param userId                 the user id
     * @param normalizedDescriptions the light-normalized descriptions to match
     * @return matched normalized description to merchant id; missing entries mean no match
     */
    public Map<String, Long> findMerchantIdsByNormalizedDescriptions(Long userId, List<String> normalizedDescriptions) {
        if (normalizedDescriptions == null || normalizedDescriptions.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> result = new HashMap<>();
        jdbcClient.sql(MerchantDescriptionLinkQueries.FIND_MERCHANT_IDS_BY_NORMALIZED_DESCRIPTIONS)
                .param("userId", userId)
                .param("normalizedDescriptions", normalizedDescriptions)
                .query((rs, rowNum) -> result.put(rs.getString("normalized_description"), rs.getLong("merchant_id")))
                .list();
        return result;
    }

    public Optional<MerchantDescriptionLink> findByIdAndUserId(Long id, Long userId) {
        return jdbcClient.sql(MerchantDescriptionLinkQueries.FIND_BY_ID_AND_USER_ID)
                .param("id", id)
                .param("userId", userId)
                .query(rowMapper)
                .optional();
    }

    /**
     * Creates or overwrites the link for a normalized description (last-write-wins) -- one method
     * serves both the auto-capture path and the explicit "add a link" endpoint.
     *
     * @param userId                the user id
     * @param merchantId             the merchant to link the description to
     * @param description            the raw description, as linked
     * @param normalizedDescription  the light-normalized lookup key
     */
    public void upsert(Long userId, Long merchantId, String description, String normalizedDescription) {
        jdbcClient.sql(MerchantDescriptionLinkQueries.UPSERT)
                .param("userId", userId)
                .param("merchantId", merchantId)
                .param("description", description)
                .param("normalizedDescription", normalizedDescription)
                .update();
    }

    /**
     * Returns every description linked to one merchant, oldest first -- backs the merchant's own
     * description-links management screen.
     *
     * @param merchantId the merchant id
     * @param userId     the user id
     * @return the merchant's linked descriptions
     */
    public List<MerchantDescriptionLink> findByMerchantIdAndUserId(Long merchantId, Long userId) {
        return jdbcClient.sql(MerchantDescriptionLinkQueries.FIND_BY_MERCHANT_ID_AND_USER_ID)
                .param("merchantId", merchantId)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    /**
     * Deletes a single description link, scoped to its owner in the query itself -- same pattern
     * {@code MerchantRepository.delete(id, userId)} already uses. Never touches transactions
     * already assigned that merchant; only affects future matching.
     *
     * @param id     the link id
     * @param userId the user id
     * @return the number of rows deleted (0 or 1)
     */
    public int deleteByIdAndUserId(Long id, Long userId) {
        return jdbcClient.sql(MerchantDescriptionLinkQueries.DELETE_BY_ID_AND_USER_ID)
                .param("id", id)
                .param("userId", userId)
                .update();
    }
}
