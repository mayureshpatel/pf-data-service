package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.MerchantDescriptionLink;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import({MerchantDescriptionLinkRepository.class, MerchantRepository.class})
@DisplayName("MerchantDescriptionLinkRepository Integration Tests (PostgreSQL)")
class MerchantDescriptionLinkRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MerchantDescriptionLinkRepository repository;

    @Autowired
    private MerchantRepository merchantRepository;

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final Long MERCHANT_CAFE = 4L; // baseline's "My Favorite Cafe", owned by user 1

    @Nested
    @DisplayName("findMerchantIdsByNormalizedDescriptions")
    class FindMerchantIdsByNormalizedDescriptionsTests {

        @Test
        @DisplayName("should return the linked merchant id for a matching normalized description")
        void shouldReturnMatch() {
            // arrange
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE", "local cafe");

            // act
            Map<String, Long> result = repository.findMerchantIdsByNormalizedDescriptions(USER_1, List.of("local cafe"));

            // assert & verify
            assertEquals(MERCHANT_CAFE, result.get("local cafe"));
        }

        @Test
        @DisplayName("PF-845: a description with no link is simply absent from the result, not an error")
        void shouldOmitUnmatchedDescriptions() {
            // act
            Map<String, Long> result = repository.findMerchantIdsByNormalizedDescriptions(USER_1, List.of("no such link"));

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not return another user's link")
        void shouldNotReturnAnotherUsersLink() {
            // arrange
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE", "local cafe");

            // act
            Map<String, Long> result = repository.findMerchantIdsByNormalizedDescriptions(USER_2, List.of("local cafe"));

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return an empty map and never query when given no descriptions")
        void shouldReturnEmptyForNoDescriptions() {
            // act
            Map<String, Long> result = repository.findMerchantIdsByNormalizedDescriptions(USER_1, List.of());

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("upsert")
    class UpsertTests {

        @Test
        @DisplayName("should create a new link")
        void shouldCreateNewLink() {
            // act
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE", "local cafe");

            // assert & verify
            List<MerchantDescriptionLink> links = repository.findByMerchantIdAndUserId(MERCHANT_CAFE, USER_1);
            assertEquals(1, links.size());
            assertEquals("LOCAL CAFE", links.get(0).getDescription());
        }

        @Test
        @DisplayName("PF-845: should overwrite (last-write-wins) rather than error when re-linking an "
                + "already-linked normalized description")
        void shouldOverwriteExistingLink() {
            // arrange
            Long otherMerchantId = merchantRepository.insert(
                    MerchantCreateRequest.builder().userId(USER_1).name("Other Cafe").build());
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE", "local cafe");

            // act -- same normalized key, different merchant and raw description
            repository.upsert(USER_1, otherMerchantId, "Local Cafe", "local cafe");

            // assert & verify -- one link, now pointing at the new merchant with the new description
            List<MerchantDescriptionLink> cafeLinks = repository.findByMerchantIdAndUserId(MERCHANT_CAFE, USER_1);
            List<MerchantDescriptionLink> otherLinks = repository.findByMerchantIdAndUserId(otherMerchantId, USER_1);
            assertTrue(cafeLinks.isEmpty());
            assertEquals(1, otherLinks.size());
            assertEquals("Local Cafe", otherLinks.get(0).getDescription());
        }
    }

    @Nested
    @DisplayName("findByIdAndUserId")
    class FindByIdAndUserIdTests {

        @Test
        @DisplayName("should find an owned link")
        void shouldFindOwnedLink() {
            // arrange
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE", "local cafe");
            Long linkId = repository.findByMerchantIdAndUserId(MERCHANT_CAFE, USER_1).get(0).getId();

            // act
            Optional<MerchantDescriptionLink> result = repository.findByIdAndUserId(linkId, USER_1);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals("LOCAL CAFE", result.get().getDescription());
        }

        @Test
        @DisplayName("should not find another user's link")
        void shouldNotFindAnotherUsersLink() {
            // arrange
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE", "local cafe");
            Long linkId = repository.findByMerchantIdAndUserId(MERCHANT_CAFE, USER_1).get(0).getId();

            // act
            Optional<MerchantDescriptionLink> result = repository.findByIdAndUserId(linkId, USER_2);

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("deleteByIdAndUserId")
    class DeleteByIdAndUserIdTests {

        @Test
        @DisplayName("should delete an owned link")
        void shouldDeleteOwnedLink() {
            // arrange
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE", "local cafe");
            Long linkId = repository.findByMerchantIdAndUserId(MERCHANT_CAFE, USER_1).get(0).getId();

            // act
            int rows = repository.deleteByIdAndUserId(linkId, USER_1);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(repository.findByIdAndUserId(linkId, USER_1).isEmpty());
        }

        @Test
        @DisplayName("should affect 0 rows, and not delete the record, when called with a userId that "
                + "doesn't own the link")
        void shouldNotDeleteAnotherUsersLink() {
            // arrange
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE", "local cafe");
            Long linkId = repository.findByMerchantIdAndUserId(MERCHANT_CAFE, USER_1).get(0).getId();

            // act
            int rows = repository.deleteByIdAndUserId(linkId, USER_2);

            // assert & verify
            assertEquals(0, rows);
            assertTrue(repository.findByIdAndUserId(linkId, USER_1).isPresent());
        }
    }

    @Nested
    @DisplayName("findByMerchantIdAndUserId")
    class FindByMerchantIdAndUserIdTests {

        @Test
        @DisplayName("should return every link for the merchant, oldest first")
        void shouldReturnLinksOldestFirst() {
            // arrange
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE", "local cafe");
            repository.upsert(USER_1, MERCHANT_CAFE, "LOCAL CAFE DOWNTOWN", "local cafe downtown");

            // act
            List<MerchantDescriptionLink> result = repository.findByMerchantIdAndUserId(MERCHANT_CAFE, USER_1);

            // assert & verify
            assertEquals(2, result.size());
            assertTrue(result.get(0).getId() < result.get(1).getId());
        }

        @Test
        @DisplayName("should return an empty list when the merchant has no links")
        void shouldReturnEmptyWhenNoLinks() {
            // act
            List<MerchantDescriptionLink> result = repository.findByMerchantIdAndUserId(MERCHANT_CAFE, USER_1);

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }
}
