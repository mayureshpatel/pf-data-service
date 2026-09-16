package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantMergeRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantReviewClusterDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantService Unit Tests")
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    // @Spy, not @Mock: a real instance so its actual logic runs (it's a plain, fully-unit-tested
    // pure function -- MerchantNameNormalizerTest -- and mocking it here would hide whether
    // MerchantService actually calls it correctly), but still injectable via @InjectMocks. Only
    // getMerchantsNeedingReview (PF-842) calls this; findOrCreateMerchant(s) do not (PF-840).
    @Spy
    private final MerchantNameNormalizer nameNormalizer = new MerchantNameNormalizer();

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @InjectMocks
    private MerchantService merchantService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("findOrCreateMerchant")
    class FindOrCreateMerchantTests {

        @Test
        @DisplayName("should return the existing merchant's id without creating a new one")
        void shouldReturnExistingMerchantId() {
            // arrange
            String description = "STARBUCKS #12345";
            Merchant existing = Merchant.builder().id(99L).userId(USER_ID).originalName(description).cleanName("").build();
            when(merchantRepository.findAllByNormalizedOriginalNameAndUserId("starbucks #12345", USER_ID))
                    .thenReturn(List.of(existing));

            // act
            Long result = merchantService.findOrCreateMerchant(USER_ID, description);

            // assert & verify
            assertEquals(99L, result);
            verify(merchantRepository, never()).insert(any(MerchantCreateRequest.class));
        }

        @Test
        @DisplayName("PF-840: should create a new merchant with a blank clean name when none exists -- "
                + "cleanName is never auto-computed at creation")
        void shouldCreateNewMerchantWithBlankCleanName() {
            // arrange
            String description = "WHOLEFDS #12345";
            when(merchantRepository.findAllByNormalizedOriginalNameAndUserId("wholefds #12345", USER_ID))
                    .thenReturn(List.of());
            when(merchantRepository.insert(any(MerchantCreateRequest.class))).thenReturn(42L);

            // act
            Long result = merchantService.findOrCreateMerchant(USER_ID, description);

            // assert & verify
            assertEquals(42L, result);
            verify(merchantRepository).insert(argThat((MerchantCreateRequest request) ->
                    request.getOriginalName().equals(description)
                            && request.getCleanName().equals("")
            ));
        }

        @Test
        @DisplayName("PF-840: should match an existing merchant when the raw description differs only by "
                + "case or incidental whitespace")
        void shouldMatchExistingMerchantDespiteFormattingDifference() {
            // arrange -- same raw text, different case and extra whitespace than what's on file
            Merchant existing = Merchant.builder().id(7L).userId(USER_ID).originalName("Starbucks  #100").cleanName("").build();
            when(merchantRepository.findAllByNormalizedOriginalNameAndUserId("starbucks #100", USER_ID))
                    .thenReturn(List.of(existing));

            // act
            Long result = merchantService.findOrCreateMerchant(USER_ID, "STARBUCKS #100");

            // assert & verify
            assertEquals(7L, result);
            verify(merchantRepository, never()).insert(any(MerchantCreateRequest.class));
        }

        @Test
        @DisplayName("PF-840: should NOT match a different store number for the same real-world chain -- "
                + "matching no longer strips numbers, so two locations stay two separate merchants until a "
                + "human links them (PF-842)")
        void shouldNotMatchDifferentStoreNumberForSameChain() {
            // arrange -- a #999 statement must not silently resolve to an existing #100 record
            when(merchantRepository.findAllByNormalizedOriginalNameAndUserId("starbucks #999", USER_ID))
                    .thenReturn(List.of());
            when(merchantRepository.insert(any(MerchantCreateRequest.class))).thenReturn(55L);

            // act
            Long result = merchantService.findOrCreateMerchant(USER_ID, "STARBUCKS #999");

            // assert & verify
            assertEquals(55L, result);
            verify(merchantRepository).insert(argThat((MerchantCreateRequest request) ->
                    request.getOriginalName().equals("STARBUCKS #999") && request.getCleanName().equals("")
            ));
        }

        @Test
        @DisplayName("PF-840: should NOT match a genuinely different merchant")
        void shouldNotMatchGenuinelyDifferentMerchant() {
            // arrange
            when(merchantRepository.findAllByNormalizedOriginalNameAndUserId("shell oil wa", USER_ID))
                    .thenReturn(List.of());
            when(merchantRepository.insert(any(MerchantCreateRequest.class))).thenReturn(55L);

            // act
            Long result = merchantService.findOrCreateMerchant(USER_ID, "SHELL OIL WA");

            // assert & verify
            assertEquals(55L, result);
            verify(merchantRepository).insert(argThat((MerchantCreateRequest request) ->
                    request.getOriginalName().equals("SHELL OIL WA") && request.getCleanName().equals("")
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
            Merchant m1 = Merchant.builder().id(1L).userId(USER_ID).originalName("STARBUCKS #1").cleanName("").build();
            Merchant m2 = Merchant.builder().id(2L).userId(USER_ID).originalName("TARGET #2").cleanName("").build();
            when(merchantRepository.findAllByNormalizedOriginalNamesAndUserId(List.of("starbucks #1", "target #2"), USER_ID))
                    .thenReturn(List.of(m1, m2));

            // act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, descriptions);

            // assert & verify
            assertEquals(Map.of("STARBUCKS #1", 1L, "TARGET #2", 2L), result);
            verify(merchantRepository, never()).insertAllAndReturn(any());
        }

        @Test
        @DisplayName("PF-840: should create only the missing merchants, with blank clean names")
        void shouldCreateOnlyMissingMerchantsWithBlankCleanNames() {
            // arrange
            List<String> descriptions = List.of("STARBUCKS #1", "CHEVRON 00123 4567");
            Merchant existing = Merchant.builder().id(1L).userId(USER_ID).originalName("STARBUCKS #1").cleanName("").build();
            when(merchantRepository.findAllByNormalizedOriginalNamesAndUserId(List.of("starbucks #1", "chevron 00123 4567"), USER_ID))
                    .thenReturn(List.of(existing));

            Merchant inserted = Merchant.builder().id(2L).userId(USER_ID).originalName("CHEVRON 00123 4567").cleanName("").build();
            when(merchantRepository.insertAllAndReturn(any())).thenReturn(List.of(inserted));

            // act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, descriptions);

            // assert & verify
            assertEquals(Map.of("STARBUCKS #1", 1L, "CHEVRON 00123 4567", 2L), result);
            verify(merchantRepository).insertAllAndReturn(argThat(requests ->
                    requests.size() == 1
                            && requests.get(0).getOriginalName().equals("CHEVRON 00123 4567")
                            && requests.get(0).getCleanName().equals("")
            ));
        }

        @Test
        @DisplayName("should return an empty map and never touch the repository when given no descriptions")
        void shouldReturnEmptyMapForNoDescriptions() {
            // arrange & act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, List.of());

            // assert & verify
            assertTrue(result.isEmpty());
            verify(merchantRepository, never()).findAllByNormalizedOriginalNamesAndUserId(any(), any());
            verify(merchantRepository, never()).insertAllAndReturn(any());
        }

        @Test
        @DisplayName("PF-840: two raw descriptions that differ only by case or whitespace resolve to the "
                + "same merchant -- only one gets created, not two")
        void shouldCollapseFormattingVariantsToOneMerchant() {
            // arrange -- identical real merchant text, differing only by case and extra whitespace
            List<String> descriptions = List.of("STARBUCKS #100", "starbucks   #100");
            Merchant inserted = Merchant.builder().id(9L).userId(USER_ID).originalName("STARBUCKS #100").cleanName("").build();
            when(merchantRepository.findAllByNormalizedOriginalNamesAndUserId(List.of("starbucks #100"), USER_ID))
                    .thenReturn(List.of());
            when(merchantRepository.insertAllAndReturn(any())).thenReturn(List.of(inserted));

            // act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, descriptions);

            // assert & verify
            assertEquals(9L, result.get("STARBUCKS #100"));
            assertEquals(9L, result.get("starbucks   #100"));
            // exactly one merchant created for both, using the first-seen description as original_name
            verify(merchantRepository).insertAllAndReturn(argThat(requests ->
                    requests.size() == 1
                            && requests.get(0).getOriginalName().equals("STARBUCKS #100")
                            && requests.get(0).getCleanName().equals("")
            ));
        }

        @Test
        @DisplayName("PF-840: two different store numbers for the same real-world chain are NOT merged in "
                + "a batch either -- matching no longer strips numbers")
        void shouldNotMergeDifferentStoreNumbersInBatch() {
            // arrange -- same chain, two different, previously-unseen store numbers
            List<String> descriptions = List.of("STARBUCKS #100", "STARBUCKS #200");
            when(merchantRepository.findAllByNormalizedOriginalNamesAndUserId(List.of("starbucks #100", "starbucks #200"), USER_ID))
                    .thenReturn(List.of());

            Merchant m1 = Merchant.builder().id(9L).userId(USER_ID).originalName("STARBUCKS #100").cleanName("").build();
            Merchant m2 = Merchant.builder().id(10L).userId(USER_ID).originalName("STARBUCKS #200").cleanName("").build();
            when(merchantRepository.insertAllAndReturn(any())).thenReturn(List.of(m1, m2));

            // act
            Map<String, Long> result = merchantService.findOrCreateMerchants(USER_ID, descriptions);

            // assert & verify
            assertNotEquals(result.get("STARBUCKS #100"), result.get("STARBUCKS #200"));
            verify(merchantRepository).insertAllAndReturn(argThat(requests -> requests.size() == 2));
        }

        @Test
        @DisplayName("PF-840: genuinely different merchants in the same batch are not merged")
        void shouldNotMergeGenuinelyDifferentMerchantsInBatch() {
            // arrange
            List<String> descriptions = List.of("CHEVRON 00123 WA", "SHELL OIL WA");
            when(merchantRepository.findAllByNormalizedOriginalNamesAndUserId(List.of("chevron 00123 wa", "shell oil wa"), USER_ID))
                    .thenReturn(List.of());

            Merchant chevron = Merchant.builder().id(1L).userId(USER_ID).originalName("CHEVRON 00123 WA").cleanName("").build();
            Merchant shell = Merchant.builder().id(2L).userId(USER_ID).originalName("SHELL OIL WA").cleanName("").build();
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
            Pageable pageable = PageRequest.of(0, 20);
            when(merchantRepository.findAllByUserId(USER_ID, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(merchant), pageable, 1));

            // act
            Page<MerchantDto> result = merchantService.getAllMerchants(USER_ID, null, pageable);

            // assert & verify
            assertEquals(1, result.getContent().size());
            assertEquals(1L, result.getContent().get(0).id());
            assertEquals("Target", result.getContent().get(0).cleanName());
        }

        @Test
        @DisplayName("PF-320: should pass the search term through to the repository unchanged")
        void shouldPassSearchTermThrough() {
            // arrange
            Pageable pageable = PageRequest.of(0, 20);
            when(merchantRepository.findAllByUserId(USER_ID, "target", pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            // act
            merchantService.getAllMerchants(USER_ID, "target", pageable);

            // assert & verify
            verify(merchantRepository).findAllByUserId(USER_ID, "target", pageable);
        }
    }

    @Nested
    @DisplayName("getDistinctCleanNames")
    class GetDistinctCleanNamesTests {

        @Test
        @DisplayName("PF-842: should return the repository's page of distinct clean names unchanged")
        void shouldReturnDistinctCleanNames() {
            // arrange
            Pageable pageable = PageRequest.of(0, 20);
            Page<String> page = new PageImpl<>(List.of("Kroger", "Starbucks"), pageable, 2);
            when(merchantRepository.findDistinctCleanNames(USER_ID, null, pageable)).thenReturn(page);

            // act
            Page<String> result = merchantService.getDistinctCleanNames(USER_ID, null, pageable);

            // assert & verify
            assertEquals(page, result);
        }
    }

    @Nested
    @DisplayName("getMerchantsByCleanName")
    class GetMerchantsByCleanNameTests {

        @Test
        @DisplayName("PF-842: should map every merchant sharing the exact clean name to a MerchantDto")
        void shouldReturnGroupAsDtos() {
            // arrange
            Merchant a = Merchant.builder().id(1L).userId(USER_ID).originalName("KROGER #431 ROSWELL").cleanName("Kroger").build();
            Merchant b = Merchant.builder().id(2L).userId(USER_ID).originalName("KROGER #696 WARNER ROBINS").cleanName("Kroger").build();
            when(merchantRepository.findAllByCleanNameAndUserId("Kroger", USER_ID)).thenReturn(List.of(a, b));

            // act
            List<MerchantDto> result = merchantService.getMerchantsByCleanName(USER_ID, "Kroger");

            // assert & verify
            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).id());
            assertEquals(2L, result.get(1).id());
        }
    }

    @Nested
    @DisplayName("getMerchantsNeedingReview")
    class GetMerchantsNeedingReviewTests {

        @Test
        @DisplayName("PF-842: should cluster merchants with a blank clean name by the normalizer's suggestion")
        void shouldClusterBlankCleanNameMerchants() {
            // arrange -- two different store numbers, same city (the merged PF-832 fix strips the
            // number but deliberately preserves the city -- same city means same suggestion here)
            Merchant a = Merchant.builder().id(1L).userId(USER_ID).originalName("KROGER #431 ROSWELL GA").cleanName("").build();
            Merchant b = Merchant.builder().id(2L).userId(USER_ID).originalName("KROGER #999 ROSWELL GA").cleanName("").build();
            when(merchantRepository.findAllByUserId(USER_ID)).thenReturn(List.of(a, b));

            // act
            List<MerchantReviewClusterDto> result = merchantService.getMerchantsNeedingReview(USER_ID);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("Kroger Roswell", result.get(0).suggestedCleanName());
            assertEquals(2, result.get(0).merchants().size());
        }

        @Test
        @DisplayName("PF-842/PF-833: should also flag a merchant whose current clean name differs from a "
                + "fresh suggestion -- the same condition resolves both brand-new blank rows and merchants "
                + "mislabeled before the normalizer existed at all, with no separate backfill")
        void shouldFlagMismatchedCleanNameMerchant() {
            // arrange -- clean name doesn't match what a fresh normalize() would produce right now
            Merchant mislabeled = Merchant.builder().id(3L).userId(USER_ID)
                    .originalName("KROGER #431 ROSWELL GA").cleanName("Kroger").build();
            when(merchantRepository.findAllByUserId(USER_ID)).thenReturn(List.of(mislabeled));

            // act
            List<MerchantReviewClusterDto> result = merchantService.getMerchantsNeedingReview(USER_ID);

            // assert & verify -- real normalizer strips the number, keeps the city
            assertEquals(1, result.size());
            assertEquals("Kroger Roswell", result.get(0).suggestedCleanName());
        }

        @Test
        @DisplayName("PF-842: should NOT flag a merchant whose clean name already matches a fresh suggestion")
        void shouldNotFlagAlreadyReviewedMerchant() {
            // arrange -- already correctly reviewed: cleanName matches what normalize() produces now
            Merchant reviewed = Merchant.builder().id(4L).userId(USER_ID)
                    .originalName("KROGER #431 ROSWELL GA").cleanName("Kroger Roswell").build();
            when(merchantRepository.findAllByUserId(USER_ID)).thenReturn(List.of(reviewed));

            // act
            List<MerchantReviewClusterDto> result = merchantService.getMerchantsNeedingReview(USER_ID);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("PF-842: known tradeoff -- a manually-customized clean name the normalizer would never "
                + "produce keeps reappearing (a dismissible suggestion, not data loss)")
        void shouldFlagCustomizedCleanNameAsAKnownTradeoff() {
            // arrange -- deliberately renamed to something no normalizer run would ever independently produce
            Merchant customized = Merchant.builder().id(5L).userId(USER_ID)
                    .originalName("KROGER #431 ROSWELL GA").cleanName("My Local Grocery Store").build();
            when(merchantRepository.findAllByUserId(USER_ID)).thenReturn(List.of(customized));

            // act
            List<MerchantReviewClusterDto> result = merchantService.getMerchantsNeedingReview(USER_ID);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(5L, result.get(0).merchants().get(0).id());
        }

        @Test
        @DisplayName("PF-842: should return clusters largest-first")
        void shouldReturnClustersLargestFirst() {
            // arrange -- one 2-member Kroger/Roswell cluster, one 1-member Amazon cluster
            Merchant krogerA = Merchant.builder().id(1L).userId(USER_ID).originalName("KROGER #431 ROSWELL GA").cleanName("").build();
            Merchant krogerB = Merchant.builder().id(2L).userId(USER_ID).originalName("KROGER #999 ROSWELL GA").cleanName("").build();
            Merchant amazon = Merchant.builder().id(3L).userId(USER_ID).originalName("AMAZON").cleanName("").build();
            when(merchantRepository.findAllByUserId(USER_ID)).thenReturn(List.of(amazon, krogerA, krogerB));

            // act
            List<MerchantReviewClusterDto> result = merchantService.getMerchantsNeedingReview(USER_ID);

            // assert & verify
            assertEquals(2, result.size());
            assertEquals("Kroger Roswell", result.get(0).suggestedCleanName());
            assertEquals(2, result.get(0).merchants().size());
            assertEquals("Amazon", result.get(1).suggestedCleanName());
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

    @Nested
    @DisplayName("updateMerchantsBulk")
    class UpdateMerchantsBulkTests {

        @Test
        @DisplayName("PF-842: should update every merchant in the request and sum the affected row counts")
        void shouldUpdateEveryMerchantInBatch() {
            // arrange -- confirming a whole review cluster in one action
            MerchantUpdateRequest r1 = MerchantUpdateRequest.builder().id(1L).cleanName("Kroger").build();
            MerchantUpdateRequest r2 = MerchantUpdateRequest.builder().id(2L).cleanName("Kroger").build();
            Merchant owned1 = Merchant.builder().id(1L).userId(USER_ID).build();
            Merchant owned2 = Merchant.builder().id(2L).userId(USER_ID).build();
            when(merchantRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(owned1));
            when(merchantRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(owned2));
            when(merchantRepository.update(r1, USER_ID)).thenReturn(1);
            when(merchantRepository.update(r2, USER_ID)).thenReturn(1);

            // act
            Integer result = merchantService.updateMerchantsBulk(USER_ID, List.of(r1, r2));

            // assert & verify
            assertEquals(2, result);
            verify(merchantRepository).update(r1, USER_ID);
            verify(merchantRepository).update(r2, USER_ID);
        }

        @Test
        @DisplayName("should return 0 and never touch the repository when given no requests")
        void shouldReturnZeroForNoRequests() {
            // arrange & act
            Integer result = merchantService.updateMerchantsBulk(USER_ID, List.of());

            // assert & verify
            assertEquals(0, result);
            verify(merchantRepository, never()).update(any(), any());
        }

        @Test
        @DisplayName("PF-842: should throw, and roll back the whole batch, when one item isn't owned by the "
                + "requesting user -- matches updateMerchant's own per-item ownership check")
        void shouldThrowWhenOneItemNotOwned() {
            // arrange
            MerchantUpdateRequest r1 = MerchantUpdateRequest.builder().id(1L).cleanName("Kroger").build();
            MerchantUpdateRequest r2 = MerchantUpdateRequest.builder().id(2L).cleanName("Kroger").build();
            Merchant owned1 = Merchant.builder().id(1L).userId(USER_ID).build();
            when(merchantRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(owned1));
            when(merchantRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> merchantService.updateMerchantsBulk(USER_ID, List.of(r1, r2)));
        }
    }

    @Nested
    @DisplayName("mergeMerchants")
    class MergeMerchantsTests {

        private static final Long SURVIVING_ID = 7L;
        private static final Long MERGED_AWAY_ID = 8L;

        @Test
        @DisplayName("PF-222: should reassign transactions, reassign recurring transactions, then delete the "
                + "merged-away merchant, in that order -- reassignment must happen before delete or the "
                + "recurring_transactions NOT NULL constraint would be violated")
        void shouldReassignThenDelete() {
            // arrange
            Merchant surviving = Merchant.builder().id(SURVIVING_ID).userId(USER_ID).cleanName("Starbucks").build();
            Merchant mergedAway = Merchant.builder().id(MERGED_AWAY_ID).userId(USER_ID).cleanName("Starbucks Coffee").build();
            when(merchantRepository.findByIdAndUserId(SURVIVING_ID, USER_ID)).thenReturn(Optional.of(surviving));
            when(merchantRepository.findByIdAndUserId(MERGED_AWAY_ID, USER_ID)).thenReturn(Optional.of(mergedAway));
            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(SURVIVING_ID)
                    .mergedAwayMerchantId(MERGED_AWAY_ID)
                    .build();

            // act
            merchantService.mergeMerchants(USER_ID, request);

            // assert & verify
            verify(transactionRepository).reassignMerchant(MERGED_AWAY_ID, SURVIVING_ID, USER_ID);
            verify(recurringTransactionRepository).reassignMerchant(MERGED_AWAY_ID, SURVIVING_ID, USER_ID);
            verify(merchantRepository).delete(MERGED_AWAY_ID, USER_ID);

            InOrder order = inOrder(transactionRepository, recurringTransactionRepository, merchantRepository);
            order.verify(transactionRepository).reassignMerchant(eq(MERGED_AWAY_ID), eq(SURVIVING_ID), eq(USER_ID));
            order.verify(recurringTransactionRepository).reassignMerchant(eq(MERGED_AWAY_ID), eq(SURVIVING_ID), eq(USER_ID));
            order.verify(merchantRepository).delete(eq(MERGED_AWAY_ID), eq(USER_ID));
        }

        @Test
        @DisplayName("PF-222: should throw and touch nothing else when the surviving merchant isn't owned")
        void shouldThrowWhenSurvivingMerchantNotOwned() {
            // arrange
            when(merchantRepository.findByIdAndUserId(SURVIVING_ID, USER_ID)).thenReturn(Optional.empty());
            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(SURVIVING_ID)
                    .mergedAwayMerchantId(MERGED_AWAY_ID)
                    .build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> merchantService.mergeMerchants(USER_ID, request));
            verify(transactionRepository, never()).reassignMerchant(any(), any(), any());
            verify(recurringTransactionRepository, never()).reassignMerchant(any(), any(), any());
            verify(merchantRepository, never()).delete(any(), any());
        }

        @Test
        @DisplayName("PF-222: should throw and touch nothing else when the merged-away merchant isn't owned")
        void shouldThrowWhenMergedAwayMerchantNotOwned() {
            // arrange
            Merchant surviving = Merchant.builder().id(SURVIVING_ID).userId(USER_ID).cleanName("Starbucks").build();
            when(merchantRepository.findByIdAndUserId(SURVIVING_ID, USER_ID)).thenReturn(Optional.of(surviving));
            when(merchantRepository.findByIdAndUserId(MERGED_AWAY_ID, USER_ID)).thenReturn(Optional.empty());
            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(SURVIVING_ID)
                    .mergedAwayMerchantId(MERGED_AWAY_ID)
                    .build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> merchantService.mergeMerchants(USER_ID, request));
            verify(transactionRepository, never()).reassignMerchant(any(), any(), any());
            verify(recurringTransactionRepository, never()).reassignMerchant(any(), any(), any());
            verify(merchantRepository, never()).delete(any(), any());
        }

        @Test
        @DisplayName("PF-222: should throw for a self-merge without even checking ownership")
        void shouldThrowForSelfMerge() {
            // arrange
            MerchantMergeRequest request = MerchantMergeRequest.builder()
                    .survivingMerchantId(SURVIVING_ID)
                    .mergedAwayMerchantId(SURVIVING_ID)
                    .build();

            // act & assert & verify
            assertThrows(IllegalArgumentException.class, () -> merchantService.mergeMerchants(USER_ID, request));
            verify(merchantRepository, never()).findByIdAndUserId(any(), any());
        }
    }
}
