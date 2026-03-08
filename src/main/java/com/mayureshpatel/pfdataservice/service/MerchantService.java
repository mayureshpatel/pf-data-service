package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    @Transactional
    public Long findOrCreateMerchant(Long userId, String description) {
        return merchantRepository.findByOriginalNameAndUserId(description, userId)
                .map(Merchant::getId)
                .orElseGet(() -> createMerchant(userId, description));
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
