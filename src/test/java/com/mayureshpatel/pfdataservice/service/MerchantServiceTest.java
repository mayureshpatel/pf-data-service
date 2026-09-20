package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantDescriptionLink;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDescriptionLinkDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantDescriptionLinkRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantService Unit Tests")
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantDescriptionLinkRepository descriptionLinkRepository;

    @InjectMocks
    private MerchantService merchantService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getAllMerchants")
    class GetAllMerchantsTests {

        @Test
        @DisplayName("should map every repository result to a MerchantDto")
        void shouldMapRepositoryResultsToDtos() {
            // arrange
            Merchant merchant = Merchant.builder().id(1L).userId(USER_ID).name("Target").build();
            Pageable pageable = PageRequest.of(0, 20);
            when(merchantRepository.findAllByUserId(USER_ID, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(merchant), pageable, 1));

            // act
            Page<MerchantDto> result = merchantService.getAllMerchants(USER_ID, null, pageable);

            // assert & verify
            assertEquals(1, result.getContent().size());
            assertEquals(1L, result.getContent().get(0).id());
            assertEquals("Target", result.getContent().get(0).name());
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
    @DisplayName("createMerchant")
    class CreateMerchantTests {

        @Test
        @DisplayName("should insert with the given name/location and return the new id")
        void shouldInsertAndReturnId() {
            // arrange
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(USER_ID).name("Trader Joe's").city("Atlanta").build();
            when(merchantRepository.insert(any(MerchantCreateRequest.class))).thenReturn(42L);

            // act
            Long result = merchantService.createMerchant(USER_ID, request);

            // assert & verify
            assertEquals(42L, result);
        }

        @Test
        @DisplayName("should always insert with userId from the authenticated caller, not whatever the request carries")
        void shouldOverrideUserIdFromCaller() {
            // arrange -- request claims a different user; the service must not trust that
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(999L).name("Trader Joe's").build();
            when(merchantRepository.insert(any(MerchantCreateRequest.class))).thenReturn(42L);

            // act
            merchantService.createMerchant(USER_ID, request);

            // assert & verify
            ArgumentCaptor<MerchantCreateRequest> captor = ArgumentCaptor.forClass(MerchantCreateRequest.class);
            verify(merchantRepository).insert(captor.capture());
            assertEquals(USER_ID, captor.getValue().getUserId());
            assertEquals("Trader Joe's", captor.getValue().getName());
        }
    }

    @Nested
    @DisplayName("updateMerchant")
    class UpdateMerchantTests {

        @Test
        @DisplayName("should update an owned merchant and return the affected row count")
        void shouldUpdateOwnedMerchant() {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(7L).name("Corrected Name").build();
            Merchant owned = Merchant.builder().id(7L).userId(USER_ID).name("Starbucks").build();
            when(merchantRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.of(owned));
            when(merchantRepository.update(request, USER_ID)).thenReturn(1);

            // act
            int result = merchantService.updateMerchant(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(merchantRepository).update(request, USER_ID);
        }

        @Test
        @DisplayName("should throw, and never call update(), for a merchant not owned by the requesting user "
                + "-- defense-in-depth even if @PreAuthorize is somehow bypassed")
        void shouldThrowForUnownedMerchant() {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder().id(7L).name("Corrected Name").build();
            when(merchantRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> merchantService.updateMerchant(USER_ID, request));
            verify(merchantRepository, never()).update(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteMerchant")
    class DeleteMerchantTests {

        @Test
        @DisplayName("should delete an owned merchant")
        void shouldDeleteOwnedMerchant() {
            // arrange
            Merchant owned = Merchant.builder().id(7L).userId(USER_ID).name("Starbucks").build();
            when(merchantRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.of(owned));

            // act
            merchantService.deleteMerchant(USER_ID, 7L);

            // assert & verify
            verify(merchantRepository).delete(7L, USER_ID);
        }

        @Test
        @DisplayName("should throw, and never call delete(), for a merchant not owned by the requesting user")
        void shouldThrowForUnownedMerchant() {
            // arrange
            when(merchantRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> merchantService.deleteMerchant(USER_ID, 7L));
            verify(merchantRepository, never()).delete(any(), any());
        }
    }

    @Nested
    @DisplayName("findMatchingMerchantId")
    class FindMatchingMerchantIdTests {

        @Test
        @DisplayName("should return the linked merchant's id when a link exists")
        void shouldReturnMatchWhenLinkExists() {
            // arrange
            when(descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(USER_ID, List.of("starbucks #1")))
                    .thenReturn(Map.of("starbucks #1", 99L));

            // act
            Optional<Long> result = merchantService.findMatchingMerchantId(USER_ID, "STARBUCKS #1");

            // assert & verify
            assertEquals(Optional.of(99L), result);
        }

        @Test
        @DisplayName("PF-845: should return empty, never create anything, when no link exists")
        void shouldReturnEmptyWhenNoLinkExists() {
            // arrange
            when(descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(USER_ID, List.of("unknown store")))
                    .thenReturn(Map.of());

            // act
            Optional<Long> result = merchantService.findMatchingMerchantId(USER_ID, "Unknown Store");

            // assert & verify
            assertTrue(result.isEmpty());
            verify(merchantRepository, never()).insert(any(MerchantCreateRequest.class));
        }
    }

    @Nested
    @DisplayName("findMatchingMerchantIds (batch)")
    class FindMatchingMerchantIdsBatchTests {

        @Test
        @DisplayName("should resolve descriptions that have a link and never create anything")
        void shouldResolveLinkedDescriptionsOnly() {
            // arrange
            List<String> descriptions = List.of("STARBUCKS #1", "TARGET #2");
            when(descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(USER_ID, List.of("starbucks #1", "target #2")))
                    .thenReturn(Map.of("starbucks #1", 1L, "target #2", 2L));

            // act
            Map<String, Long> result = merchantService.findMatchingMerchantIds(USER_ID, descriptions);

            // assert & verify
            assertEquals(Map.of("STARBUCKS #1", 1L, "TARGET #2", 2L), result);
            verify(merchantRepository, never()).insert(any(MerchantCreateRequest.class));
        }

        @Test
        @DisplayName("PF-845: a description with no link is simply absent from the result, not an error")
        void shouldOmitUnlinkedDescriptions() {
            // arrange
            List<String> descriptions = List.of("STARBUCKS #1", "UNKNOWN STORE");
            when(descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(USER_ID, List.of("starbucks #1", "unknown store")))
                    .thenReturn(Map.of("starbucks #1", 1L));

            // act
            Map<String, Long> result = merchantService.findMatchingMerchantIds(USER_ID, descriptions);

            // assert & verify
            assertEquals(Map.of("STARBUCKS #1", 1L), result);
            assertFalse(result.containsKey("UNKNOWN STORE"));
        }

        @Test
        @DisplayName("should return an empty map and never touch the repository when given no descriptions")
        void shouldReturnEmptyMapForNoDescriptions() {
            // arrange & act
            Map<String, Long> result = merchantService.findMatchingMerchantIds(USER_ID, List.of());

            // assert & verify
            assertTrue(result.isEmpty());
            verify(descriptionLinkRepository, never()).findMerchantIdsByNormalizedDescriptions(any(), any());
        }

        @Test
        @DisplayName("PF-845: two raw descriptions that differ only by case or whitespace match the same link")
        void shouldCollapseFormattingVariantsToOneMatch() {
            // arrange
            List<String> descriptions = List.of("STARBUCKS #100", "starbucks   #100");
            when(descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(USER_ID, List.of("starbucks #100")))
                    .thenReturn(Map.of("starbucks #100", 9L));

            // act
            Map<String, Long> result = merchantService.findMatchingMerchantIds(USER_ID, descriptions);

            // assert & verify
            assertEquals(9L, result.get("STARBUCKS #100"));
            assertEquals(9L, result.get("starbucks   #100"));
        }
    }

    @Nested
    @DisplayName("recordDescriptionLink")
    class RecordDescriptionLinkTests {

        @Test
        @DisplayName("should upsert with the light-normalized description as the lookup key")
        void shouldUpsertWithNormalizedKey() {
            // act
            merchantService.recordDescriptionLink(USER_ID, 7L, "  STARBUCKS   #1  ");

            // assert & verify
            verify(descriptionLinkRepository).upsert(USER_ID, 7L, "  STARBUCKS   #1  ", "starbucks #1");
        }
    }

    @Nested
    @DisplayName("getDescriptionLinks")
    class GetDescriptionLinksTests {

        @Test
        @DisplayName("should map every linked description to a DTO for an owned merchant")
        void shouldReturnLinksAsDtos() {
            // arrange
            Merchant owned = Merchant.builder().id(7L).userId(USER_ID).name("Starbucks").build();
            when(merchantRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.of(owned));
            MerchantDescriptionLink link = MerchantDescriptionLink.builder()
                    .id(1L).userId(USER_ID).merchantId(7L).description("STARBUCKS #1").build();
            when(descriptionLinkRepository.findByMerchantIdAndUserId(7L, USER_ID)).thenReturn(List.of(link));

            // act
            List<MerchantDescriptionLinkDto> result = merchantService.getDescriptionLinks(USER_ID, 7L);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("STARBUCKS #1", result.get(0).description());
        }

        @Test
        @DisplayName("should throw for a merchant not owned by the requesting user")
        void shouldThrowForUnownedMerchant() {
            // arrange
            when(merchantRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> merchantService.getDescriptionLinks(USER_ID, 7L));
        }
    }

    @Nested
    @DisplayName("deleteDescriptionLink")
    class DeleteDescriptionLinkTests {

        @Test
        @DisplayName("should delete an owned link")
        void shouldDeleteOwnedLink() {
            // arrange
            MerchantDescriptionLink link = MerchantDescriptionLink.builder()
                    .id(1L).userId(USER_ID).merchantId(7L).description("STARBUCKS #1").build();
            when(descriptionLinkRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(link));

            // act
            merchantService.deleteDescriptionLink(USER_ID, 1L);

            // assert & verify
            verify(descriptionLinkRepository).deleteByIdAndUserId(1L, USER_ID);
        }

        @Test
        @DisplayName("should throw, and never call delete(), for a link not owned by the requesting user")
        void shouldThrowForUnownedLink() {
            // arrange
            when(descriptionLinkRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> merchantService.deleteDescriptionLink(USER_ID, 1L));
            verify(descriptionLinkRepository, never()).deleteByIdAndUserId(any(), any());
        }
    }
}
