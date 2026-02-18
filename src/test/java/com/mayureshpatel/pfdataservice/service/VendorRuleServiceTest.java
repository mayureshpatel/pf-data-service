package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.vendor.VendorRule;
import com.mayureshpatel.pfdataservice.dto.vendor.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.vendor.UnmatchedVendorDto;
import com.mayureshpatel.pfdataservice.dto.vendor.VendorRuleDto;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.repository.vendor.VendorRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorRuleServiceTest {

    @Mock
    private VendorRuleRepository vendorRuleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private VendorCleaner vendorCleaner;

    @InjectMocks
    private VendorRuleService vendorRuleService;

    private User user;
    private VendorRule rule;
    private VendorRuleDto ruleDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        rule = new VendorRule();
        rule.setId(10L);
        rule.setUser(user);
        rule.setKeyword("STARBUCKS");
        rule.setVendorName("Starbucks");
        rule.setPriority(1);

        ruleDto = VendorRuleDto.builder()
                .keyword("STARBUCKS")
                .vendorName("Starbucks")
                .priority(1)
                .build();
    }

    @Test
    void getRules_ShouldReturnListOfDtos() {
        // Given
        when(vendorRuleRepository.findByUserOrGlobal(1L)).thenReturn(List.of(rule));

        // When
        List<VendorRuleDto> result = vendorRuleService.getRules(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).keyword()).isEqualTo("STARBUCKS");
        verify(vendorRuleRepository).findByUserOrGlobal(1L);
    }

    @Test
    void createRule_ShouldSaveAndReturnDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(vendorRuleRepository.save(any(VendorRule.class))).thenReturn(rule);

        // When
        VendorRuleDto result = vendorRuleService.createRule(1L, ruleDto);

        // Then
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.keyword()).isEqualTo("STARBUCKS");
        verify(userRepository).findById(1L);
        verify(vendorRuleRepository).save(any(VendorRule.class));
    }

    @Test
    void deleteRule_ShouldDeleteIfOwner() {
        // Given
        when(vendorRuleRepository.findById(10L)).thenReturn(Optional.of(rule));

        // When
        vendorRuleService.deleteRule(1L, 10L);

        // Then
        verify(vendorRuleRepository).delete(rule);
    }

    @Test
    void deleteRule_ShouldThrowExceptionIfNotOwner() {
        // Given
        when(vendorRuleRepository.findById(10L)).thenReturn(Optional.of(rule));

        // When & Then
        assertThatThrownBy(() -> vendorRuleService.deleteRule(99L, 10L))
                .isInstanceOf(AccessDeniedException.class);
        verify(vendorRuleRepository, never()).delete(any());
    }

    @Test
    void deleteRule_ShouldThrowExceptionIfGlobal() {
        // Given
        rule.setUser(null);
        when(vendorRuleRepository.findById(10L)).thenReturn(Optional.of(rule));

        // When & Then
        assertThatThrownBy(() -> vendorRuleService.deleteRule(1L, 10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Cannot delete global rules");
        verify(vendorRuleRepository, never()).delete(any());
    }

    @Test
    void applyRules_ShouldUpdateTransactions() {
        // Given
        Transaction t = new Transaction();
        t.setOriginalVendorName("STARBUCKS COFFEE 123");
        t.setVendorName("Old Vendor");

        when(vendorCleaner.loadRulesForUser(1L)).thenReturn(List.of(rule));
        when(transactionRepository.findByAccount_User_Id(1L)).thenReturn(List.of(t));
        when(vendorCleaner.cleanVendorName(eq("STARBUCKS COFFEE 123"), anyList())).thenReturn("Starbucks");

        // When
        vendorRuleService.applyRules(1L);

        // Then
        assertThat(t.getVendorName()).isEqualTo("Starbucks");
        verify(transactionRepository).saveAll(anyList());
    }

    @Test
    void previewApply_ShouldReturnList() {
        // Given
        Transaction t = new Transaction();
        t.setOriginalVendorName("STARBUCKS COFFEE 123");
        t.setVendorName("Old Vendor");
        t.setDescription("STARBUCKS COFFEE 123");

        when(vendorCleaner.loadRulesForUser(1L)).thenReturn(List.of(rule));
        when(transactionRepository.findByAccount_User_Id(1L)).thenReturn(List.of(t));
        when(vendorCleaner.cleanVendorName(eq("STARBUCKS COFFEE 123"), anyList())).thenReturn("Starbucks");

        // When
        List<RuleChangePreviewDto> previews = vendorRuleService.previewApply(1L);

        // Then
        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).newCategory()).isEqualTo("Starbucks");
    }

    @Test
    void getUnmatchedVendors_ShouldReturnList() {
        // Given
        Transaction t = new Transaction();
        t.setOriginalVendorName("Unknown Store");
        t.setDescription("Unknown Store");

        when(vendorCleaner.loadRulesForUser(1L)).thenReturn(Collections.emptyList());
        when(transactionRepository.findByAccount_User_Id(1L)).thenReturn(List.of(t));
        when(vendorCleaner.cleanVendorName(eq("Unknown Store"), anyList())).thenReturn(null);

        // When
        List<UnmatchedVendorDto> unmatched = vendorRuleService.getUnmatchedVendors(1L);

        // Then
        assertThat(unmatched).hasSize(1);
        assertThat(unmatched.get(0).originalName()).isEqualTo("Unknown Store");
        assertThat(unmatched.get(0).count()).isEqualTo(1);
    }
}
