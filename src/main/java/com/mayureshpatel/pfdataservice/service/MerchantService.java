package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the raw description text on an imported transaction to a deduplicated
 * {@code Merchant} record, creating one on first sight. {@code cleanName} is generated at
 * creation time via {@link MerchantNameNormalizer}, so merchants get a readable display name
 * immediately instead of the raw, uncleaned bank description.
 * <p>
 * Resolution matches on the normalized clean name, not raw {@code original_name} equality (PF-219)
 * -- two raw descriptions that differ only by case, a trailing reference number, or a trailing
 * state code resolve to the same merchant instead of each creating their own.
 */
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantNameNormalizer nameNormalizer;

    /**
     * Returns all merchants for a user.
     *
     * @param userId the user id
     * @return the user's merchants
     */
    public List<MerchantDto> getAllMerchants(Long userId) {
        return merchantRepository.findAllByUserId(userId)
                .stream()
                .map(MerchantDtoMapper::toDto)
                .toList();
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
     * Matching is by normalized clean name (PF-219), not raw {@code original_name} equality.
     *
     * @param userId      the user id
     * @param description the raw transaction description to resolve
     * @return the matched or newly created merchant's id
     */
    @Transactional
    public Long findOrCreateMerchant(Long userId, String description) {
        String cleanName = nameNormalizer.normalize(description);
        List<Merchant> matches = merchantRepository.findAllByCleanNameAndUserId(cleanName, userId);
        if (!matches.isEmpty()) {
            return matches.get(0).getId();
        }
        return createMerchant(userId, description, cleanName);
    }

    /**
     * Batch version of {@link #findOrCreateMerchant}: resolves a list of transaction
     * descriptions to merchant ids in one pass, creating any that don't already exist. Several
     * raw descriptions can normalize to the same clean name -- each still gets its own entry in
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

        // Normalized once per distinct raw description -- reused below both to look up existing
        // merchants and, for anything missing, as the clean_name of the merchant that gets created.
        // LinkedHashMap (not the default Collectors.toMap HashMap) so distinctCleanNames below comes
        // out in a deterministic, insertion-derived order rather than hash-bucket order -- this has
        // no bearing on correctness, but it keeps the repository call's argument predictable.
        Map<String, String> descriptionToCleanName = distinctDescriptions.stream()
                .collect(Collectors.toMap(desc -> desc, nameNormalizer::normalize, (a, b) -> a, LinkedHashMap::new));
        List<String> distinctCleanNames = descriptionToCleanName.values().stream().distinct().toList();

        List<Merchant> existingMerchants = merchantRepository.findAllByCleanNamesAndUserId(distinctCleanNames, userId);
        Map<String, Long> cleanNameToMerchantId = new LinkedHashMap<>();
        existingMerchants.forEach(m -> cleanNameToMerchantId.putIfAbsent(m.getCleanName(), m.getId()));

        List<String> missingCleanNames = distinctCleanNames.stream()
                .filter(cleanName -> !cleanNameToMerchantId.containsKey(cleanName))
                .toList();

        if (!missingCleanNames.isEmpty()) {
            // One new merchant per distinct missing clean name, not per raw description. Whichever
            // distinct description reaches a given clean name first (stream order over
            // distinctDescriptions) becomes that merchant's original_name -- just the first-seen
            // raw text, not otherwise meaningful once normalized.
            Map<String, String> cleanNameToFirstDescription = new LinkedHashMap<>();
            distinctDescriptions.forEach(desc ->
                    cleanNameToFirstDescription.putIfAbsent(descriptionToCleanName.get(desc), desc));

            List<MerchantCreateRequest> newMerchants = missingCleanNames.stream()
                    .map(cleanName -> MerchantCreateRequest.builder()
                            .userId(userId)
                            .originalName(cleanNameToFirstDescription.get(cleanName))
                            .cleanName(cleanName)
                            .build())
                    .toList();

            merchantRepository.insertAllAndReturn(newMerchants)
                    .forEach(m -> cleanNameToMerchantId.put(m.getCleanName(), m.getId()));
        }

        return distinctDescriptions.stream()
                .collect(Collectors.toMap(
                        desc -> desc,
                        desc -> cleanNameToMerchantId.get(descriptionToCleanName.get(desc))));
    }

    /**
     * Creates a new merchant for a transaction description with a precomputed clean name (the
     * caller has always already normalized it while checking for an existing match, so this
     * doesn't normalize a second time).
     *
     * @param userId      the user id
     * @param description the raw transaction description to create a merchant for
     * @param cleanName   {@code description}, already normalized by {@link MerchantNameNormalizer}
     * @return the new merchant's generated id
     */
    private Long createMerchant(Long userId, String description, String cleanName) {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(userId)
                .originalName(description)
                .cleanName(cleanName)
                .build();
        return merchantRepository.insert(request);
    }
}
