package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkChangeDto;
import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkUndoResultDto;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantDescriptionLinkRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Links raw descriptions to a merchant on the user's explicit request and, unlike the quiet
 * auto-capture in {@link MerchantService#recordDescriptionLink}, also fills the merchant in on
 * existing transactions that have none (PF-980). A merchant already set on a transaction is never
 * overwritten.
 * <p>
 * Undo is stateless, like {@link RuleApplicationUndoService}: the link result lists every link it
 * created or moved and every transaction it filled, the client sends that result back, and the undo
 * only reverses what has not changed since. A link that now points at another merchant, or a
 * transaction whose merchant was changed by hand, is left alone and reported as skipped. The
 * previous merchant of a filled transaction is always "none", so the body is not trusted for it. A
 * link's previous merchant does come from the body, but it is only restored when it still belongs
 * to the user. Because the undo trusts only current state, a hand-made body can at most remove or
 * repoint the caller's own links that point at this merchant and clear it from the caller's own
 * transactions, which the user could already do directly; it can never reach another user's data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DescriptionLinkService {

    private final MerchantDescriptionLinkRepository descriptionLinkRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Links every description to the merchant (last-write-wins, so a description linked elsewhere
     * moves), then gives the merchant to the user's live transactions that have no merchant and use
     * one of those descriptions. All in one transaction.
     *
     * @param userId       the user id
     * @param merchantId   the merchant to link to; its ownership is checked by the caller
     * @param descriptions the raw descriptions to link
     * @return what changed, in the shape the undo endpoint takes back
     * @throws IllegalArgumentException when the link would fill more than
     *                                  {@link DescriptionLinkResultDto#MAX_FILLED} transactions; the
     *                                  whole request is rolled back so it stays undoable
     */
    @Transactional
    public DescriptionLinkResultDto linkDescriptions(Long userId, Long merchantId, List<String> descriptions) {
        // keep the first raw spelling of each key, in request order
        Map<String, String> rawByKey = new LinkedHashMap<>();
        for (String description : descriptions) {
            rawByKey.putIfAbsent(DescriptionKeys.fold(description), description);
        }
        List<String> keys = new ArrayList<>(rawByKey.keySet());
        Map<String, Long> current = descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(userId, keys);

        List<DescriptionLinkChangeDto> changes = new ArrayList<>();
        for (Map.Entry<String, String> entry : rawByKey.entrySet()) {
            String key = entry.getKey();
            Long previous = current.get(key);
            descriptionLinkRepository.upsert(userId, merchantId, entry.getValue(), key);
            if (!Objects.equals(previous, merchantId)) {
                changes.add(new DescriptionLinkChangeDto(key, previous));
            }
        }
        int moved = (int) changes.stream().filter(change -> change.previousMerchantId() != null).count();

        List<Long> filled = transactionRepository.fillMerchantByNormalizedDescriptions(userId, merchantId, keys);
        if (filled.size() > DescriptionLinkResultDto.MAX_FILLED) {
            // too big to undo in one call: throwing rolls back the links and the fill
            throw new IllegalArgumentException("Linking these descriptions would fill more than "
                    + DescriptionLinkResultDto.MAX_FILLED + " transactions. Link fewer descriptions at a time.");
        }
        log.info("linked {} descriptions to merchant {} for user {}: {} moved, {} transactions filled",
                keys.size(), merchantId, userId, moved, filled.size());
        return new DescriptionLinkResultDto(merchantId, keys.size(), moved, filled.size(), changes, filled);
    }

    /**
     * Reverses a link request from its own result: each link it created is removed and each link it
     * moved goes back to its previous merchant, and each transaction it filled has its merchant
     * cleared again, but only where nothing has changed since. All in one transaction.
     *
     * @param userId the user id
     * @param result the link result, as returned by {@link #linkDescriptions}; its merchant's
     *               ownership is checked by the caller
     * @return how much was put back and what was left because it changed since
     */
    @Transactional
    public DescriptionLinkUndoResultDto undoLink(Long userId, DescriptionLinkResultDto result) {
        Long merchantId = result.merchantId();
        int linksRestored = 0;
        for (DescriptionLinkChangeDto change : result.linkChanges()) {
            String key = change.normalizedDescription();
            int updated = 0;
            if (change.previousMerchantId() != null) {
                updated = descriptionLinkRepository.repointIfCurrent(userId, key, merchantId, change.previousMerchantId());
            }
            if (updated == 0) {
                // created by the request, or its old merchant is gone: remove it if still ours
                updated = descriptionLinkRepository.deleteIfCurrent(userId, key, merchantId);
            }
            linksRestored += updated;
        }
        int linksSkipped = result.linkChanges().size() - linksRestored;

        List<Long> ids = result.filledTransactionIds().stream().distinct().toList();
        Set<Long> cleared = new HashSet<>(transactionRepository.clearMerchantIfCurrent(userId, merchantId, ids));
        List<Long> skipped = ids.stream().filter(id -> !cleared.contains(id)).toList();

        log.info("undid description link to merchant {} for user {}: {} links restored, {} skipped, {} transactions cleared, {} skipped",
                merchantId, userId, linksRestored, linksSkipped, cleared.size(), skipped.size());
        return new DescriptionLinkUndoResultDto(linksRestored, linksSkipped, cleared.size(), skipped);
    }
}
