package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkChangeDto;
import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.DescriptionLinkUndoResultDto;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantDescriptionLinkRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link DescriptionLinkService}: linking descriptions records which links were created or
 * moved and fills only merchantless transactions, and the stateless undo reverses only what has not
 * changed since.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DescriptionLinkService Unit Tests")
class DescriptionLinkServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long MERCHANT_ID = 7L;
    private static final Long OTHER_MERCHANT = 9L;

    @Mock
    private MerchantDescriptionLinkRepository descriptionLinkRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DescriptionLinkService service;

    /** Links are upserted once per folded key, changes are recorded against the previous link, and the past is filled. */
    @Nested
    @DisplayName("linkDescriptions")
    class LinkDescriptionsTests {

        @Test
        @DisplayName("should upsert each key once, record new and moved links, and fill the past")
        void shouldLinkAndFill() {
            // arrange
            List<String> descriptions = List.of("KROGER #1", "kroger  #1", "KROGER #2", "KROGER #3");
            List<String> keys = List.of("kroger #1", "kroger #2", "kroger #3");
            when(descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(USER_ID, keys))
                    .thenReturn(Map.of("kroger #2", OTHER_MERCHANT, "kroger #3", MERCHANT_ID));
            when(transactionRepository.fillMerchantByNormalizedDescriptions(USER_ID, MERCHANT_ID, keys))
                    .thenReturn(List.of(40L, 41L));

            // act
            DescriptionLinkResultDto result = service.linkDescriptions(USER_ID, MERCHANT_ID, descriptions);

            // assert & verify
            assertThat(result.merchantId()).isEqualTo(MERCHANT_ID);
            assertThat(result.linkedCount()).isEqualTo(3);
            assertThat(result.movedCount()).isEqualTo(1);
            assertThat(result.transactionsFilled()).isEqualTo(2);
            assertThat(result.linkChanges()).containsExactly(
                    new DescriptionLinkChangeDto("kroger #1", null),
                    new DescriptionLinkChangeDto("kroger #2", OTHER_MERCHANT));
            assertThat(result.filledTransactionIds()).containsExactly(40L, 41L);
            verify(descriptionLinkRepository).upsert(USER_ID, MERCHANT_ID, "KROGER #1", "kroger #1");
            verify(descriptionLinkRepository, times(3)).upsert(anyLong(), anyLong(), anyString(), anyString());
        }
        @Test
        @DisplayName("should reject a link that would fill more transactions than one undo can take back")
        void shouldRejectOversizedFill() {
            // arrange
            List<Long> tooMany = LongStream.rangeClosed(1, DescriptionLinkResultDto.MAX_FILLED + 1L).boxed().toList();
            when(descriptionLinkRepository.findMerchantIdsByNormalizedDescriptions(USER_ID, List.of("kroger #1"))).thenReturn(Map.of());
            when(transactionRepository.fillMerchantByNormalizedDescriptions(USER_ID, MERCHANT_ID, List.of("kroger #1"))).thenReturn(tooMany);

            // act & assert & verify
            assertThatThrownBy(() -> service.linkDescriptions(USER_ID, MERCHANT_ID, List.of("KROGER #1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Link fewer descriptions");
        }
    }

    /** Undo repoints moved links, deletes created ones, falls back to delete when the old merchant is gone, and reports skips. */
    @Nested
    @DisplayName("undoLink")
    class UndoLinkTests {

        @Test
        @DisplayName("should restore unchanged links and transactions and report the rest as skipped")
        void shouldUndoWhatIsUnchanged() {
            // arrange
            DescriptionLinkResultDto result = new DescriptionLinkResultDto(MERCHANT_ID, 3, 2, 3,
                    List.of(new DescriptionLinkChangeDto("created", null),
                            new DescriptionLinkChangeDto("moved", OTHER_MERCHANT),
                            new DescriptionLinkChangeDto("changed since", OTHER_MERCHANT)),
                    List.of(40L, 41L, 41L, 42L));
            when(descriptionLinkRepository.deleteIfCurrent(USER_ID, "created", MERCHANT_ID)).thenReturn(1);
            when(descriptionLinkRepository.repointIfCurrent(USER_ID, "moved", MERCHANT_ID, OTHER_MERCHANT)).thenReturn(1);
            when(descriptionLinkRepository.repointIfCurrent(USER_ID, "changed since", MERCHANT_ID, OTHER_MERCHANT)).thenReturn(0);
            when(descriptionLinkRepository.deleteIfCurrent(USER_ID, "changed since", MERCHANT_ID)).thenReturn(0);
            when(transactionRepository.clearMerchantIfCurrent(USER_ID, MERCHANT_ID, List.of(40L, 41L, 42L)))
                    .thenReturn(List.of(40L, 42L));

            // act
            DescriptionLinkUndoResultDto undo = service.undoLink(USER_ID, result);

            // assert & verify
            assertThat(undo.linksRestored()).isEqualTo(2);
            assertThat(undo.linksSkipped()).isEqualTo(1);
            assertThat(undo.transactionsRestored()).isEqualTo(2);
            assertThat(undo.skippedTransactionIds()).containsExactly(41L);
            verify(descriptionLinkRepository, never()).deleteIfCurrent(USER_ID, "moved", MERCHANT_ID);
        }

        @Test
        @DisplayName("should remove a moved link when its previous merchant can no longer be restored")
        void shouldDeleteWhenPreviousGone() {
            // arrange
            DescriptionLinkResultDto result = new DescriptionLinkResultDto(MERCHANT_ID, 1, 1, 0,
                    List.of(new DescriptionLinkChangeDto("moved", OTHER_MERCHANT)), List.of());
            when(descriptionLinkRepository.repointIfCurrent(USER_ID, "moved", MERCHANT_ID, OTHER_MERCHANT)).thenReturn(0);
            when(descriptionLinkRepository.deleteIfCurrent(USER_ID, "moved", MERCHANT_ID)).thenReturn(1);
            when(transactionRepository.clearMerchantIfCurrent(USER_ID, MERCHANT_ID, List.of())).thenReturn(List.of());

            // act
            DescriptionLinkUndoResultDto undo = service.undoLink(USER_ID, result);

            // assert & verify
            assertThat(undo.linksRestored()).isEqualTo(1);
            assertThat(undo.linksSkipped()).isZero();
            assertThat(undo.skippedTransactionIds()).isEmpty();
        }
    }
}
