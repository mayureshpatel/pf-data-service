package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies {@code MerchantRuleService}'s CRUD, its defaults for a null match type and priority, and that a foreign or missing merchant or rule is rejected before anything is written. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantRuleService Unit Tests")
class MerchantRuleServiceTest {

    @Mock
    private MerchantRuleRepository merchantRuleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MerchantService merchantService;

    @InjectMocks
    private MerchantRuleService service;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long RULE_ID = 100L;
    private static final Long MERCHANT_ID = 50L;
    private static final String KEYWORD = "KROGER";

    private MerchantRule savedRule(Long ownerId) {
        return MerchantRule.builder()
                .id(RULE_ID)
                .user(User.builder().id(ownerId).build())
                .keywords(List.of("OLD"))
                .matchType(MatchType.AND)
                .priority(3)
                .merchant(Merchant.builder().id(MERCHANT_ID).build())
                .build();
    }

    /** {@code getRules} maps every repository row for the user to a {@link MerchantRuleDto}. */
    @Nested
    @DisplayName("getRules")
    class GetRulesTests {
        @Test
        @DisplayName("should return list of rule DTOs")
        void shouldReturnRules() {
            // arrange
            when(merchantRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(savedRule(USER_ID)));

            // act
            List<MerchantRuleDto> result = service.getRules(USER_ID);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(List.of("OLD"), result.get(0).keywords());
            assertEquals(USER_ID, result.get(0).userId());
        }
    }

    /** {@code createRule} verifies the merchant is the user's, defaults a null match type to OR and a null priority to 0, and takes the owner from the user id argument. */
    @Nested
    @DisplayName("createRule")
    class CreateRuleTests {
        @Test
        @DisplayName("should create the rule with the request's fields and the session user as owner")
        void shouldCreate() {
            // arrange
            User user = User.builder().id(USER_ID).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(merchantRuleRepository.insertAndReturnId(any())).thenReturn(42L);
            MerchantRuleCreateRequest request = MerchantRuleCreateRequest.builder()
                    .merchantId(MERCHANT_ID).keywords(List.of(KEYWORD)).matchType(MatchType.AND).priority(7)
                    .minAmount(new BigDecimal("5.00")).maxAmount(new BigDecimal("20.00")).build();

            // act
            Long result = service.createRule(USER_ID, request);

            // assert & verify
            assertEquals(42L, result);
            verify(merchantService).requireOwnedMerchants(USER_ID, List.of(MERCHANT_ID));
            ArgumentCaptor<MerchantRule> captor = ArgumentCaptor.forClass(MerchantRule.class);
            verify(merchantRuleRepository).insertAndReturnId(captor.capture());
            MerchantRule saved = captor.getValue();
            assertEquals(USER_ID, saved.getUser().getId());
            assertEquals(MERCHANT_ID, saved.getMerchant().getId());
            assertEquals(List.of(KEYWORD), saved.getKeywords());
            assertEquals(MatchType.AND, saved.getMatchType());
            assertEquals(7, saved.getPriority());
            assertEquals(new BigDecimal("5.00"), saved.getMinAmount());
            assertEquals(new BigDecimal("20.00"), saved.getMaxAmount());
        }

        @Test
        @DisplayName("should default a null match type to OR and a null priority to 0")
        void shouldApplyDefaults() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(merchantRuleRepository.insertAndReturnId(any())).thenReturn(42L);
            MerchantRuleCreateRequest request = MerchantRuleCreateRequest.builder()
                    .merchantId(MERCHANT_ID).keywords(List.of(KEYWORD)).build();

            // act
            service.createRule(USER_ID, request);

            // assert & verify
            ArgumentCaptor<MerchantRule> captor = ArgumentCaptor.forClass(MerchantRule.class);
            verify(merchantRuleRepository).insertAndReturnId(captor.capture());
            assertEquals(MatchType.OR, captor.getValue().getMatchType());
            assertEquals(0, captor.getValue().getPriority());
        }

        @Test
        @DisplayName("should throw and insert nothing when the merchant is not the user's")
        void shouldRejectForeignMerchant() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            doThrow(new ResourceNotFoundException("Merchant not found.")).when(merchantService)
                    .requireOwnedMerchants(USER_ID, List.of(MERCHANT_ID));
            MerchantRuleCreateRequest request = MerchantRuleCreateRequest.builder()
                    .merchantId(MERCHANT_ID).keywords(List.of(KEYWORD)).build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> service.createRule(USER_ID, request));
            verify(merchantRuleRepository, never()).insertAndReturnId(any());
        }

        @Test
        @DisplayName("should throw when the user does not exist")
        void shouldRejectMissingUser() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            MerchantRuleCreateRequest request = MerchantRuleCreateRequest.builder()
                    .merchantId(MERCHANT_ID).keywords(List.of(KEYWORD)).build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> service.createRule(USER_ID, request));
            verify(merchantRuleRepository, never()).insertAndReturnId(any());
        }
    }

    /** {@code updateRule} requires the user's own rule and the user's own merchant, replaces the editable fields, and applies the same defaults as create. */
    @Nested
    @DisplayName("updateRule")
    class UpdateRuleTests {
        private MerchantRuleUpdateRequest.MerchantRuleUpdateRequestBuilder request() {
            return MerchantRuleUpdateRequest.builder().id(RULE_ID).merchantId(60L).keywords(List.of(KEYWORD));
        }

        @Test
        @DisplayName("should update the rule's fields and point it at the new merchant")
        void shouldUpdate() {
            // arrange
            when(merchantRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(savedRule(USER_ID)));
            when(merchantRuleRepository.update(any())).thenReturn(1);

            // act
            int rows = service.updateRule(USER_ID, request().matchType(MatchType.OR).priority(9).build());

            // assert & verify
            assertEquals(1, rows);
            verify(merchantService).requireOwnedMerchants(USER_ID, List.of(60L));
            ArgumentCaptor<MerchantRule> captor = ArgumentCaptor.forClass(MerchantRule.class);
            verify(merchantRuleRepository).update(captor.capture());
            assertEquals(RULE_ID, captor.getValue().getId());
            assertEquals(60L, captor.getValue().getMerchant().getId());
            assertEquals(List.of(KEYWORD), captor.getValue().getKeywords());
            assertEquals(9, captor.getValue().getPriority());
        }

        @Test
        @DisplayName("should default a null match type to OR and a null priority to 0")
        void shouldApplyDefaults() {
            // arrange
            when(merchantRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(savedRule(USER_ID)));

            // act
            service.updateRule(USER_ID, request().build());

            // assert & verify
            ArgumentCaptor<MerchantRule> captor = ArgumentCaptor.forClass(MerchantRule.class);
            verify(merchantRuleRepository).update(captor.capture());
            assertEquals(MatchType.OR, captor.getValue().getMatchType());
            assertEquals(0, captor.getValue().getPriority());
        }

        @Test
        @DisplayName("should throw when the rule does not exist")
        void shouldRejectMissingRule() {
            // arrange
            when(merchantRuleRepository.findById(RULE_ID)).thenReturn(Optional.empty());
            MerchantRuleUpdateRequest request = request().build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> service.updateRule(USER_ID, request));
            verify(merchantRuleRepository, never()).update(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when the rule belongs to another user")
        void shouldRejectForeignRule() {
            // arrange
            when(merchantRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(savedRule(OTHER_USER_ID)));
            MerchantRuleUpdateRequest request = request().build();

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> service.updateRule(USER_ID, request));
            verify(merchantRuleRepository, never()).update(any());
        }

        @Test
        @DisplayName("should throw and update nothing when the merchant is not the user's")
        void shouldRejectForeignMerchant() {
            // arrange
            when(merchantRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(savedRule(USER_ID)));
            doThrow(new ResourceNotFoundException("Merchant not found.")).when(merchantService)
                    .requireOwnedMerchants(USER_ID, List.of(60L));
            MerchantRuleUpdateRequest request = request().build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> service.updateRule(USER_ID, request));
            verify(merchantRuleRepository, never()).update(any());
        }
    }

    /** {@code deleteRule} deletes only the caller's own rule, scoped by user id. */
    @Nested
    @DisplayName("deleteRule")
    class DeleteRuleTests {
        @Test
        @DisplayName("should delete the user's rule")
        void shouldDelete() {
            // arrange
            when(merchantRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(savedRule(USER_ID)));

            // act
            service.deleteRule(USER_ID, RULE_ID);

            // assert & verify
            verify(merchantRuleRepository).deleteById(RULE_ID, USER_ID);
        }

        @Test
        @DisplayName("should throw when the rule does not exist")
        void shouldRejectMissingRule() {
            // arrange
            when(merchantRuleRepository.findById(RULE_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> service.deleteRule(USER_ID, RULE_ID));
            verify(merchantRuleRepository, never()).deleteById(any(), any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when the rule belongs to another user")
        void shouldRejectForeignRule() {
            // arrange
            when(merchantRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(savedRule(OTHER_USER_ID)));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> service.deleteRule(USER_ID, RULE_ID));
            verify(merchantRuleRepository, never()).deleteById(any(), any());
        }
    }
}
