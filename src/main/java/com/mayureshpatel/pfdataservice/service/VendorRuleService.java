package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.VendorRuleDto;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.model.VendorRule;
import com.mayureshpatel.pfdataservice.repository.UserRepository;
import com.mayureshpatel.pfdataservice.repository.VendorRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorRuleService {

    private final VendorRuleRepository vendorRuleRepository;
    private final UserRepository userRepository;

    public List<VendorRuleDto> getRules(Long userId) {
        return vendorRuleRepository.findByUserOrGlobal(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public VendorRuleDto createRule(Long userId, VendorRuleDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        VendorRule rule = new VendorRule();
        rule.setUser(user);
        rule.setKeyword(dto.keyword());
        rule.setVendorName(dto.vendorName());
        rule.setPriority(dto.priority() != null ? dto.priority() : 0);

        return mapToDto(vendorRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        VendorRule rule = vendorRuleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found"));

        if (rule.getUser() != null && !rule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this rule");
        }
        
        // Prevent deleting global rules via this API (unless admin, but for now simple check)
        if (rule.getUser() == null) {
             throw new AccessDeniedException("Cannot delete global rules");
        }

        vendorRuleRepository.delete(rule);
    }

    private VendorRuleDto mapToDto(VendorRule rule) {
        return VendorRuleDto.builder()
                .id(rule.getId())
                .keyword(rule.getKeyword())
                .vendorName(rule.getVendorName())
                .priority(rule.getPriority())
                .build();
    }
}
