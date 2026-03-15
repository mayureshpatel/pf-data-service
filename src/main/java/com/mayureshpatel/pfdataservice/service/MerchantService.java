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

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public List<MerchantDto> getAllMerchants(Long userId) {
        return merchantRepository.findAllByUserId(userId)
                .stream()
                .map(MerchantDtoMapper::toDto)
                .toList();
    }

    @Transactional
    public Long findOrCreateMerchant(Long userId, String description) {
        return merchantRepository.findByOriginalNameAndUserId(description, userId)
                .map(Merchant::getId)
                .orElseGet(() -> createMerchant(userId, description));
    }

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
                        .cleanName("")
                        .build())
                .toList();

        merchantRepository.insertAllAndReturn(missingMerchants).forEach(m -> merchantMap.put(m.getOriginalName(), m.getId()));
        return merchantMap;
    }

    private Long createMerchant(Long userId, String description) {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(userId)
                .originalName(description)
                .cleanName("") // Non-nullable, so using empty string as requested
                .build();
        return merchantRepository.insert(request);
    }
}
