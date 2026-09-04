package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantService Unit Tests")
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private MerchantService merchantService;

    // @Spy, not @Mock: a real instance so its actual logic runs (it's a plain, fully-unit-tested
    // pure function -- MerchantNameNormalizerTest -- and mocking it here would hide whether
    // MerchantService actually calls it correctly), but still injectable via @InjectMocks.
    @Spy
    private final MerchantNameNormalizer nameNormalizer = new MerchantNameNormalizer();

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("findOrCreateMerchant")
    class FindOrCreateMerchantTests {

        @Test
        @DisplayName("should return the existing merchant's id without creating a new one")
        void shouldReturnExistingMerchantId() {
            // arrange
            String description = "STARBUCKS #12345";
            Merchant existing = Merchant.builder().id(99L).userId(USER_ID).originalName(description).cleanName("Starbucks").build();
            when(merchantRepository.findByOriginalNameAndUserId(description, USER_ID)).thenReturn(Optional.of(existing));

            // act
            Long result = merchantService.findOrCreateMerchant(USER_ID, description);

            // assert & verify
            assertEquals(99L, result);
            verify(merchantRepository, never()).insert(any(MerchantCreateRequest.class));
        }

        @Test
        @DisplayName("should create a new merchant with a normalized clean name when none exists")
        void shouldCreateNewMerchantWithNormalizedName() {
            // arrange
            String description = "WHOLEFDS #12345";
            when(merchantRepository.findByOriginalNameAndUserId(description, USER_ID)).thenReturn(Optional.empty());
            when(merchantRepository.insert(any(MerchantCreateRequest.class))).thenReturn(42L);

            // act
            Long result = merchantService.findOrCreateMerchant(USER_ID, description);

            // assert & verify
            assertEquals(42L, result);
            verify(merchantRepository).insert(argThat((MerchantCreateRequest request) ->
                    request.getOriginalName().equals(description)
                            && request.getCleanName().equals("Wholefds")
                            && !request.getCleanName().isBlank()
            ));
        }
    }

    @Nested
    @DisplayName("findOrCreateMerchants (batch)")
    class FindOrCreateMerchantsBatchTests {

        @Test
        @DisplayName("should resolve already-existing merchants without inserting a new one for any of them")
        void shouldResolveExistingMerchantsOnly() {
            // arrange
            List<String> descriptions = List.of("STARBUCKS #1", "TARGET #2");
            Merchant m1 = Merchant.builder().id(1L).userId(USER_ID).originalName("STARBUCKS #1").cleanName("Starbucks").build();
            Merchant m2 = Merchant.builder().id(2L).userId(USER_ID).originalName("TARGET #2").cleanName("Target").build();
            when(merchantRepository.findAllByOriginalNamesAndUserId(descriptions, USER_ID)).thenReturn(List.of(m1, m2));
            when(merchantRepository.insertAllAndReturn(any())).thenReturn(List.of());

            // act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, descriptions);

            // assert & verify
            assertEquals(Map.of("STARBUCKS #1", 1L, "TARGET #2", 2L), result);
            // insertAllAndReturn is still called (with an empty list) -- it's the repository method's
            // own job to short-circuit that cheaply, not the service's job to guard against calling it.
            verify(merchantRepository).insertAllAndReturn(List.of());
        }

        @Test
        @DisplayName("should create only the missing merchants, with normalized clean names")
        void shouldCreateOnlyMissingMerchantsWithNormalizedNames() {
            // arrange
            List<String> descriptions = List.of("STARBUCKS #1", "CHEVRON 00123 4567");
            Merchant existing = Merchant.builder().id(1L).userId(USER_ID).originalName("STARBUCKS #1").cleanName("Starbucks").build();
            when(merchantRepository.findAllByOriginalNamesAndUserId(descriptions, USER_ID)).thenReturn(List.of(existing));

            Merchant inserted = Merchant.builder().id(2L).userId(USER_ID).originalName("CHEVRON 00123 4567").cleanName("Chevron").build();
            when(merchantRepository.insertAllAndReturn(any())).thenReturn(List.of(inserted));

            // act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, descriptions);

            // assert & verify
            assertEquals(Map.of("STARBUCKS #1", 1L, "CHEVRON 00123 4567", 2L), result);
            verify(merchantRepository).insertAllAndReturn(argThat(requests ->
                    requests.size() == 1
                            && requests.get(0).getOriginalName().equals("CHEVRON 00123 4567")
                            && requests.get(0).getCleanName().equals("Chevron")
            ));
        }

        @Test
        @DisplayName("should return an empty map and touch the repository only for the lookup when given no descriptions")
        void shouldReturnEmptyMapForNoDescriptions() {
            // arrange & act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, List.of());

            // assert & verify
            assertTrue(result.isEmpty());
            verify(merchantRepository, never()).findAllByOriginalNamesAndUserId(any(), any());
            verify(merchantRepository, never()).insertAllAndReturn(any());
        }
    }

    @Nested
    @DisplayName("getAllMerchants")
    class GetAllMerchantsTests {

        @Test
        @DisplayName("should map every repository result to a MerchantDto")
        void shouldMapRepositoryResultsToDtos() {
            // arrange
            Merchant merchant = Merchant.builder().id(1L).userId(USER_ID).originalName("TARGET #2").cleanName("Target").build();
            when(merchantRepository.findAllByUserId(USER_ID)).thenReturn(List.of(merchant));

            // act
            List<MerchantDto> result = merchantService.getAllMerchants(USER_ID);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).id());
            assertEquals("Target", result.get(0).cleanName());
        }
    }
}
