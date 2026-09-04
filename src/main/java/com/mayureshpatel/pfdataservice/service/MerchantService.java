package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the raw description text on an imported transaction to a deduplicated
 * {@code Merchant} record, creating one on first sight. {@code cleanName} is generated at
 * creation time via {@link MerchantNameNormalizer}, so merchants get a readable display name
 * immediately instead of the raw, uncleaned bank description.
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
     * Finds the merchant matching a transaction description, creating one if none exists yet.
     *
     * @param userId      the user id
     * @param description the raw transaction description to resolve
     * @return the matched or newly created merchant's id
     */
    @Transactional
    public Long findOrCreateMerchant(Long userId, String description) {
        return merchantRepository.findByOriginalNameAndUserId(description, userId)
                .map(Merchant::getId)
                .orElseGet(() -> createMerchant(userId, description));
    }

    /**
     * Batch version of {@link #findOrCreateMerchant}: resolves a list of transaction
     * descriptions to merchant ids in one pass, creating any that don't already exist.
     *
     * @param userId       the user id
     * @param descriptions the raw transaction descriptions to resolve
     * @return a map from each distinct description to its merchant id
     */
    @Transactional
    public Map<String, Long> findOrCreateMerchants(Long userId, List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) {
            return Map.of();
        }

        List<String> distinctDescriptions = descriptions.stream().distinct().toList();

        List<Merchant> existingMerchants = merchantRepository.findAllByOriginalNamesAndUserId(distinctDescriptions, userId);

        Map<String, Long> merchantMap = existingMerchants.stream()
                .collect(Collectors.toMap(Merchant::getOriginalName, Merchant::getId));

        List<MerchantCreateRequest> missingMerchants = distinctDescriptions.stream()
                .filter(desc -> !merchantMap.containsKey(desc))
                .map(desc -> MerchantCreateRequest.builder()
                        .userId(userId)
                        .originalName(desc)
                        .cleanName(nameNormalizer.normalize(desc))
                        .build())
                .toList();

        merchantRepository.insertAllAndReturn(missingMerchants).forEach(m -> merchantMap.put(m.getOriginalName(), m.getId()));
        return merchantMap;
    }

    /**
     * Creates a new merchant for a transaction description, with a {@code cleanName} generated
     * by {@link MerchantNameNormalizer}.
     *
     * @param userId      the user id
     * @param description the raw transaction description to create a merchant for
     * @return the new merchant's generated id
     */
    private Long createMerchant(Long userId, String description) {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(userId)
                .originalName(description)
                .cleanName(nameNormalizer.normalize(description))
                .build();
        return merchantRepository.insert(request);
    }
}
