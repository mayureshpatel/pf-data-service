package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.MerchantDescriptionLinkDtoMapper;
import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantDescriptionLinkRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CRUD for a user's deliberately-created merchants (PF-845), plus matching/auto-capture against
 * {@code merchant_description_links}: a separate table remembering which raw transaction
 * descriptions map to which merchant. Unlike the model this replaces (PF-EPIC-047, superseded),
 * nothing here ever auto-creates a merchant from transaction text -- {@link #findMatchingMerchantId}/
 * {@link #findMatchingMerchantIds} are lookup-only, and a description with no existing link simply
 * resolves to nothing. A link itself is created two ways: automatically, as a side effect whenever
 * a human assigns a merchant to a transaction ({@code TransactionService}), or explicitly via
 * {@link #recordDescriptionLink}'s other caller, the description-links management endpoint.
 */
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantDescriptionLinkRepository descriptionLinkRepository;

    /**
     * Case-folds and collapses whitespace in a raw description -- the lookup key
     * {@link #findMatchingMerchantId}/{@link #findMatchingMerchantIds}/{@link #recordDescriptionLink}
     * use against {@code merchant_description_links.normalized_description}. Exact-match-after-
     * light-normalization only: no fuzzy matching, no number/state stripping -- a user can look at
     * a description and know whether it will match.
     *
     * @param raw the raw description; may be null
     * @return the case-folded, whitespace-collapsed form, or {@code ""} for a null input
     */
    private static String lightNormalize(String raw) {
        return raw == null ? "" : raw.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a page of a user's merchants (PF-320), optionally narrowed by a case-insensitive
     * search term matched against name or city.
     *
     * @param userId   the user id
     * @param search   an optional case-insensitive substring to match against name/city
     * @param pageable the requested page, size, and sort
     * @return the requested page of the user's merchants
     */
    public Page<MerchantDto> getAllMerchants(Long userId, String search, Pageable pageable) {
        return merchantRepository.findAllByUserId(userId, search, pageable)
                .map(MerchantDtoMapper::toDto);
    }

    /**
     * Creates a new merchant the user deliberately named. The only way a merchant gets created --
     * nothing in this app auto-creates one from a transaction description.
     *
     * @param userId  the authenticated user id
     * @param request the merchant to create
     * @return the new merchant's generated id
     */
    @Transactional
    public Long createMerchant(Long userId, MerchantCreateRequest request) {
        return merchantRepository.insert(request.toBuilder().userId(userId).build());
    }

    /**
     * Updates a merchant's name and location. Ownership is checked here in addition to the
     * Controller's {@code @PreAuthorize}, matching this project's established defense-in-depth
     * pattern for owned-resource updates (see {@code AccountService.updateAccount}).
     *
     * @param userId  the authenticated user id
     * @param request the merchant's new name and location fields, including its id
     * @return the number of rows updated (0 or 1)
     * @throws ResourceNotFoundException if the merchant doesn't exist or isn't owned by {@code userId}
     */
    @Transactional
    public int updateMerchant(Long userId, MerchantUpdateRequest request) {
        merchantRepository.findByIdAndUserId(request.getId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found."));
        return merchantRepository.update(request, userId);
    }

    /**
     * Deletes a merchant the user owns. No manual cleanup of dependents is needed here --
     * {@code transactions.merchant_id} is {@code ON DELETE SET NULL} and
     * {@code merchant_description_links.merchant_id} is {@code ON DELETE CASCADE}, so the database
     * itself leaves dependent transactions with a blank merchant rather than erroring, and removes
     * the now-meaningless links.
     *
     * @param userId     the authenticated user id
     * @param merchantId the merchant id to delete
     * @throws ResourceNotFoundException if the merchant doesn't exist or isn't owned by {@code userId}
     */
    @Transactional
    public void deleteMerchant(Long userId, Long merchantId) {
        merchantRepository.findByIdAndUserId(merchantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found."));
        merchantRepository.delete(merchantId, userId);
    }

    /**
     * Looks up the merchant an existing description link points to. Never creates one -- a
     * description with no link simply has no match.
     *
     * @param userId      the user id
     * @param description the raw transaction description to match
     * @return the linked merchant's id, or empty if no link exists for this description
     */
    public Optional<Long> findMatchingMerchantId(Long userId, String description) {
        String normalized = lightNormalize(description);
        return Optional.ofNullable(
                descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(userId, List.of(normalized)).get(normalized));
    }

    /**
     * Batch form of {@link #findMatchingMerchantId}: resolves a list of transaction descriptions
     * against existing description links in one pass. A description with no link is simply absent
     * from the returned map -- callers must treat "absent" as "leave the merchant blank," not as
     * an error.
     *
     * @param userId       the user id
     * @param descriptions the raw transaction descriptions to match
     * @return each distinct raw description that has an existing link, mapped to its merchant id
     */
    public Map<String, Long> findMatchingMerchantIds(Long userId, List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) {
            return Map.of();
        }

        List<String> distinctDescriptions = descriptions.stream().distinct().toList();
        // linkedhashmap (not the default Collectors.toMap hashmap) so the normalized-key list
        // below comes out in a deterministic, insertion-derived order rather than hash-bucket
        // order -- no bearing on correctness, but keeps the repository call's argument predictable.
        Map<String, String> descriptionToNormalizedKey = distinctDescriptions.stream()
                .collect(Collectors.toMap(desc -> desc, MerchantService::lightNormalize, (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> normalizedKeyToMerchantId = descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(
                userId, descriptionToNormalizedKey.values().stream().distinct().toList());

        return distinctDescriptions.stream()
                .filter(desc -> normalizedKeyToMerchantId.containsKey(descriptionToNormalizedKey.get(desc)))
                .collect(Collectors.toMap(desc -> desc, desc -> normalizedKeyToMerchantId.get(descriptionToNormalizedKey.get(desc))));
    }

    /**
     * Creates or overwrites the link from a description to a merchant (last-write-wins) -- called
     * both as a side effect whenever a human assigns a merchant to a transaction
     * ({@code TransactionService}) and directly from the explicit add-link endpoint. Import never
     * calls this: it consumes existing links, it never creates new ones.
     *
     * @param userId      the user id
     * @param merchantId  the merchant to link the description to
     * @param description the raw description to link
     */
    @Transactional
    public void recordDescriptionLink(Long userId, Long merchantId, String description) {
        descriptionLinkRepository.upsert(userId, merchantId, description, lightNormalize(description));
    }

    /**
     * Returns every description linked to a merchant the user owns -- backs the merchant's own
     * description-links management screen.
     *
     * @param userId     the authenticated user id
     * @param merchantId the merchant id
     * @return the merchant's linked descriptions
     * @throws ResourceNotFoundException if the merchant doesn't exist or isn't owned by {@code userId}
     */
    public List<MerchantDescriptionLinkDto> getDescriptionLinks(Long userId, Long merchantId) {
        merchantRepository.findByIdAndUserId(merchantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found."));
        return descriptionLinkRepository.findByMerchantIdAndUserId(merchantId, userId).stream()
                .map(MerchantDescriptionLinkDtoMapper::toDto)
                .toList();
    }

    /**
     * Deletes a single description link the user owns. Never touches transactions already
     * assigned that merchant -- only affects future matching.
     *
     * @param userId the authenticated user id
     * @param linkId the link id to delete
     * @throws ResourceNotFoundException if the link doesn't exist or isn't owned by {@code userId}
     */
    @Transactional
    public void deleteDescriptionLink(Long userId, Long linkId) {
        descriptionLinkRepository.findByIdAndUserId(linkId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Description link not found."));
        descriptionLinkRepository.deleteByIdAndUserId(linkId, userId);
    }
}
