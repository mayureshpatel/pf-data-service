package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            when(merchantRepository.findAllByCleanNameAndUserId("Starbucks", USER_ID)).thenReturn(List.of(existing));

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
            when(merchantRepository.findAllByCleanNameAndUserId("Wholefds", USER_ID)).thenReturn(List.of());
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

        @Test
        @DisplayName("PF-219: should match an existing merchant even when the raw description differs, "
                + "as long as it normalizes to the same clean name")
        void shouldMatchExistingMerchantDespiteRawDescriptionDifference() {
            // arrange -- same real-world merchant, different store number than what's on file
            Merchant existing = Merchant.builder().id(7L).userId(USER_ID).originalName("STARBUCKS #100").cleanName("Starbucks").build();
            when(merchantRepository.findAllByCleanNameAndUserId("Starbucks", USER_ID)).thenReturn(List.of(existing));

            // act
            Long result = merchantService.findOrCreateMerchant(USER_ID, "STARBUCKS #999");

            // assert & verify
            assertEquals(7L, result);
            verify(merchantRepository, never()).insert(any(MerchantCreateRequest.class));
        }

        @Test
        @DisplayName("PF-219: should NOT match a genuinely different merchant that happens to share no relation "
                + "to the one on file -- the matching change must not over-merge")
        void shouldNotMatchGenuinelyDifferentMerchant() {
            // arrange -- an existing "Chevron" on file must not catch an unrelated "Shell Oil" lookup
            when(merchantRepository.findAllByCleanNameAndUserId("Shell Oil", USER_ID)).thenReturn(List.of());
            when(merchantRepository.insert(any(MerchantCreateRequest.class))).thenReturn(55L);

            // act
            Long result = merchantService.findOrCreateMerchant(USER_ID, "SHELL OIL WA");

            // assert & verify
            assertEquals(55L, result);
            verify(merchantRepository).insert(argThat((MerchantCreateRequest request) ->
                    request.getCleanName().equals("Shell Oil")
            ));
            // never looked up under the unrelated existing merchant's clean name
            verify(merchantRepository, never()).findAllByCleanNameAndUserId("Chevron", USER_ID);
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
            when(merchantRepository.findAllByCleanNamesAndUserId(List.of("Starbucks", "Target"), USER_ID))
                    .thenReturn(List.of(m1, m2));

            // act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, descriptions);

            // assert & verify
            assertEquals(Map.of("STARBUCKS #1", 1L, "TARGET #2", 2L), result);
            verify(merchantRepository, never()).insertAllAndReturn(any());
        }

        @Test
        @DisplayName("should create only the missing merchants, with normalized clean names")
        void shouldCreateOnlyMissingMerchantsWithNormalizedNames() {
            // arrange
            List<String> descriptions = List.of("STARBUCKS #1", "CHEVRON 00123 4567");
            Merchant existing = Merchant.builder().id(1L).userId(USER_ID).originalName("STARBUCKS #1").cleanName("Starbucks").build();
            when(merchantRepository.findAllByCleanNamesAndUserId(List.of("Starbucks", "Chevron"), USER_ID))
                    .thenReturn(List.of(existing));

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
        @DisplayName("should return an empty map and never touch the repository when given no descriptions")
        void shouldReturnEmptyMapForNoDescriptions() {
            // arrange & act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, List.of());

            // assert & verify
            assertTrue(result.isEmpty());
            verify(merchantRepository, never()).findAllByCleanNamesAndUserId(any(), any());
            verify(merchantRepository, never()).insertAllAndReturn(any());
        }

        @Test
        @DisplayName("PF-219: two raw descriptions that normalize to the same clean name resolve to the "
                + "same merchant -- only one gets created, not two")
        void shouldCollapseMultipleRawDescriptionsToOneMerchant() {
            // arrange -- same real-world Starbucks, two different store numbers, neither on file yet
            List<String> descriptions = List.of("STARBUCKS #100", "STARBUCKS #200");
            Merchant inserted = Merchant.builder().id(9L).userId(USER_ID).originalName("STARBUCKS #100").cleanName("Starbucks").build();
            when(merchantRepository.findAllByCleanNamesAndUserId(List.of("Starbucks"), USER_ID)).thenReturn(List.of());
            when(merchantRepository.insertAllAndReturn(any())).thenReturn(List.of(inserted));

            // act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, descriptions);

            // assert & verify
            assertEquals(9L, result.get("STARBUCKS #100"));
            assertEquals(9L, result.get("STARBUCKS #200"));
            // exactly one merchant created for both, using the first-seen description as original_name
            verify(merchantRepository).insertAllAndReturn(argThat(requests ->
                    requests.size() == 1
                            && requests.get(0).getOriginalName().equals("STARBUCKS #100")
                            && requests.get(0).getCleanName().equals("Starbucks")
            ));
        }

        @Test
        @DisplayName("PF-219: genuinely different merchants in the same batch are not merged")
        void shouldNotMergeGenuinelyDifferentMerchantsInBatch() {
            // arrange
            List<String> descriptions = List.of("CHEVRON 00123 WA", "SHELL OIL WA");
            when(merchantRepository.findAllByCleanNamesAndUserId(List.of("Chevron", "Shell Oil"), USER_ID)).thenReturn(List.of());

            Merchant chevron = Merchant.builder().id(1L).userId(USER_ID).originalName("CHEVRON 00123 WA").cleanName("Chevron").build();
            Merchant shell = Merchant.builder().id(2L).userId(USER_ID).originalName("SHELL OIL WA").cleanName("Shell Oil").build();
            when(merchantRepository.insertAllAndReturn(any())).thenReturn(List.of(chevron, shell));

            // act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, descriptions);

            // assert & verify
            assertNotEquals(result.get("CHEVRON 00123 WA"), result.get("SHELL OIL WA"));
            verify(merchantRepository).insertAllAndReturn(argThat(requests -> requests.size() == 2));
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

    @Nested
    @DisplayName("updateMerchant")
    class UpdateMerchantTests {

        @Test
        @DisplayName("should update an owned merchant and return the affected row count")
        void shouldUpdateOwnedMerchant() {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(7L).cleanName("Corrected Name").build();
            Merchant owned = Merchant.builder().id(7L).userId(USER_ID).originalName("STARBUCKS #1").cleanName("Starbucks").build();
            when(merchantRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.of(owned));
            when(merchantRepository.update(request, USER_ID)).thenReturn(1);

            // act
            int result = merchantService.updateMerchant(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(merchantRepository).update(request, USER_ID);
        }

        @Test
        @DisplayName("PF-220: should throw ResourceNotFoundException, and never call update(), for a merchant "
                + "not owned by the requesting user -- defense-in-depth even if @PreAuthorize is somehow bypassed")
        void shouldThrowForUnownedMerchant() {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(7L).cleanName("Corrected Name").build();
            when(merchantRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> merchantService.updateMerchant(USER_ID, request));
            verify(merchantRepository, never()).update(any(), any());
        }
    }
}
