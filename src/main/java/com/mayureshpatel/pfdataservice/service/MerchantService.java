package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantMergeRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the raw description text on an imported transaction to a deduplicated
 * {@code Merchant} record, creating one on first sight. {@code cleanName} is left blank ({@code
 * ""}) at creation (PF-840) -- it's a deliberate, user-managed display label, set later by a
 * human or a reviewed suggestion (PF-842), never auto-computed here.
 * <p>
 * Resolution matches on light normalization of {@code original_name} (case-fold + whitespace-
 * collapse only, see {@link #lightNormalize}) -- this supersedes PF-219's original approach of
 * matching on normalized clean-name equality. Two raw descriptions that differ only by case or
 * incidental whitespace resolve to the same merchant; nothing beyond that is ever assumed to be
 * the same merchant automatically -- see {@link MerchantNameNormalizer}, which still fixes
 * PF-832's chain-stripping bug but is now only a suggestion generator for the review flow
 * (PF-842), never an ingest-time matcher.
 */
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    /**
     * Case-folds and collapses whitespace in a raw merchant name -- the matching key
     * {@link #findOrCreateMerchant}/{@link #findOrCreateMerchants} use to decide whether a raw
     * description resolves to an existing merchant. Deliberately does not strip numbers, cities,
     * or reference codes (unlike {@link MerchantNameNormalizer}) -- matching only ever collapses
     * pure formatting noise, never approximates two genuinely different descriptions into one
     * merchant. Has a SQL mirror in {@code MerchantQueries.FIND_ALL_BY_NORMALIZED_ORIGINAL_NAME*}
     * that must stay in exact lockstep, or matching silently diverges between newly-created rows
     * (matched here, in Java) and existing rows (matched there, in SQL).
     *
     * @param raw the raw merchant name; may be null
     * @return the case-folded, whitespace-collapsed form, or {@code ""} for a null input
     */
    private static String lightNormalize(String raw) {
        return raw == null ? "" : raw.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a page of a user's merchants (PF-320), optionally narrowed by a case-insensitive
     * search term matched against either name column -- replaces the previous unbounded
     * {@code List<MerchantDto>} return, which grew linearly with a user's transaction history
     * (PF-319).
     *
     * @param userId   the user id
     * @param search   an optional case-insensitive substring to match against clean/original name
     * @param pageable the requested page, size, and sort
     * @return the requested page of the user's merchants
     */
    public Page<MerchantDto> getAllMerchants(Long userId, String search, Pageable pageable) {
        return merchantRepository.findAllByUserId(userId, search, pageable)
                .map(MerchantDtoMapper::toDto);
    }

    /**
     * Manually corrects a merchant's display name (PF-220) -- for fixing names automatic
     * normalization gets wrong, or merchants that predate it. Ownership is checked here in
     * addition to the Controller's {@code @PreAuthorize}, matching this project's established
     * defense-in-depth pattern for owned-resource updates (see {@code AccountService.updateAccount}):
     * the {@code @PreAuthorize} gate keeps a non-owner's request from ever reaching this method,
     * this lookup keeps the method itself safe to call from anywhere else in the codebase without
     * relying on that gate, and the repository's own {@code UPDATE ... WHERE id = ? AND user_id = ?}
     * keeps the write itself scoped even if both of those were somehow bypassed.
     *
     * @param userId  the authenticated user id
     * @param request the correction: the merchant id and its new clean name
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
     * Finds the merchant matching a transaction description, creating one if none exists yet.
     * Matching is by light normalization of {@code original_name} (PF-840), not clean-name
     * equality (PF-219's original approach) and not raw equality either.
     *
     * @param userId      the user id
     * @param description the raw transaction description to resolve
     * @return the matched or newly created merchant's id
     */
    @Transactional
    public Long findOrCreateMerchant(Long userId, String description) {
        List<Merchant> matches = merchantRepository.findAllByNormalizedOriginalNameAndUserId(
                lightNormalize(description), userId);
        if (!matches.isEmpty()) {
            return matches.get(0).getId();
        }
        return createMerchant(userId, description);
    }

    /**
     * Batch version of {@link #findOrCreateMerchant}: resolves a list of transaction
     * descriptions to merchant ids in one pass, creating any that don't already exist. Several
     * raw descriptions can light-normalize to the same key -- each still gets its own entry in
     * the returned map, but they resolve to the same merchant id rather than one each.
     *
     * @param userId       the user id
     * @param descriptions the raw transaction descriptions to resolve
     * @return a map from each distinct raw description to its resolved merchant id
     */
    @Transactional
    public Map<String, Long> findOrCreateMerchants(Long userId, List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) {
            return Map.of();
        }

        List<String> distinctDescriptions = descriptions.stream().distinct().toList();

        // normalized once per distinct raw description -- reused below both to look up existing
        // merchants and, for anything missing, as the matching key of the merchant that gets
        // created. linkedhashmap (not the default collectors.toMap hashmap) so
        // distinctNormalizedKeys below comes out in a deterministic, insertion-derived order
        // rather than hash-bucket order -- this has no bearing on correctness, but it keeps the
        // repository call's argument predictable.
        Map<String, String> descriptionToNormalizedKey = distinctDescriptions.stream()
                .collect(Collectors.toMap(desc -> desc, MerchantService::lightNormalize, (a, b) -> a, LinkedHashMap::new));
        List<String> distinctNormalizedKeys = descriptionToNormalizedKey.values().stream().distinct().toList();

        List<Merchant> existingMerchants = merchantRepository.findAllByNormalizedOriginalNamesAndUserId(distinctNormalizedKeys, userId);
        Map<String, Long> normalizedKeyToMerchantId = new LinkedHashMap<>();
        existingMerchants.forEach(m -> normalizedKeyToMerchantId.putIfAbsent(lightNormalize(m.getOriginalName()), m.getId()));

        List<String> missingNormalizedKeys = distinctNormalizedKeys.stream()
                .filter(key -> !normalizedKeyToMerchantId.containsKey(key))
                .toList();

        if (!missingNormalizedKeys.isEmpty()) {
            // one new merchant per distinct missing key, not per raw description. whichever
            // distinct description reaches a given key first (stream order over
            // distinctDescriptions) becomes that merchant's original_name -- just the first-seen
            // raw text, not otherwise meaningful once normalized. cleanName is always "" -- never
            // auto-computed (PF-840).
            Map<String, String> normalizedKeyToFirstDescription = new LinkedHashMap<>();
            distinctDescriptions.forEach(desc ->
                    normalizedKeyToFirstDescription.putIfAbsent(descriptionToNormalizedKey.get(desc), desc));

            List<MerchantCreateRequest> newMerchants = missingNormalizedKeys.stream()
                    .map(key -> MerchantCreateRequest.builder()
                            .userId(userId)
                            .originalName(normalizedKeyToFirstDescription.get(key))
                            .cleanName("")
                            .build())
                    .toList();

            merchantRepository.insertAllAndReturn(newMerchants)
                    .forEach(m -> normalizedKeyToMerchantId.put(lightNormalize(m.getOriginalName()), m.getId()));
        }

        return distinctDescriptions.stream()
                .collect(Collectors.toMap(
                        desc -> desc,
                        desc -> normalizedKeyToMerchantId.get(descriptionToNormalizedKey.get(desc))));
    }

    /**
     * Merges {@code mergedAwayMerchantId} into {@code survivingMerchantId} (PF-222): reassigns
     * every transaction and recurring transaction pointing at the merged-away merchant to the
     * survivor, then deletes the merged-away record. All in one transaction -- a partial failure
     * must never leave a transaction or recurring rule pointing at a merchant that's been deleted.
     * <p>
     * Order matters: reassignment happens before deletion, not after. If the delete ran first and
     * relied on {@code recurring_transactions_merchant_id_fkey}'s {@code ON DELETE SET NULL} to
     * clean up, it would immediately violate that column's {@code NOT NULL} constraint and roll
     * the whole operation back -- the exact edge case this story exists to actually exercise for
     * the first time (nothing has ever deleted a merchant before this).
     *
     * @param userId                the authenticated user id
     * @param request               which merchant survives and which gets merged away
     * @throws ResourceNotFoundException if either merchant doesn't exist or isn't owned by {@code userId}
     * @throws IllegalArgumentException  if both ids are the same merchant
     */
    @Transactional
    public void mergeMerchants(Long userId, MerchantMergeRequest request) {
        Long survivingId = request.getSurvivingMerchantId();
        Long mergedAwayId = request.getMergedAwayMerchantId();

        if (survivingId.equals(mergedAwayId)) {
            throw new IllegalArgumentException("Cannot merge a merchant into itself.");
        }

        merchantRepository.findByIdAndUserId(survivingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Surviving merchant not found."));
        merchantRepository.findByIdAndUserId(mergedAwayId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merged-away merchant not found."));

        transactionRepository.reassignMerchant(mergedAwayId, survivingId, userId);
        recurringTransactionRepository.reassignMerchant(mergedAwayId, survivingId, userId);
        merchantRepository.delete(mergedAwayId, userId);
    }

    /**
     * Creates a new merchant for a transaction description. {@code cleanName} is always left
     * blank ({@code ""}) -- it's set later, deliberately, by a human or a reviewed suggestion
     * (PF-842), never auto-computed at creation (PF-840).
     *
     * @param userId      the user id
     * @param description the raw transaction description to create a merchant for
     * @return the new merchant's generated id
     */
    private Long createMerchant(Long userId, String description) {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(userId)
                .originalName(description)
                .cleanName("")
                .build();
        return merchantRepository.insert(request);
    }
}
