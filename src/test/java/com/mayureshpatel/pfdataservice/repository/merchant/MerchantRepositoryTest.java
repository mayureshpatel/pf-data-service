package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(MerchantRepository.class)
@DisplayName("MerchantRepository Integration Tests (PostgreSQL)")
class MerchantRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MerchantRepository repository;

    private static final Long USER_1 = 1L;
    private static final Long MERCHANT_WHOLEFOODS = 1L; // Global

    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should find global merchant by ID")
        void shouldFindById() {
            // Act
            Optional<Merchant> result = repository.findById(MERCHANT_WHOLEFOODS);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("Whole Foods", result.get().getCleanName());
            assertNull(result.get().getUserId());
        }

        @Test
        @DisplayName("should find all merchants for user (including null/global ones if supported by query logic)")
        void shouldFindAllByUserId() {
            // Note: The query only filters by user_id = :userId
            // Act
            List<Merchant> result = repository.findAllByUserId(USER_1);

            // Assert
            // Baseline has 1 custom merchant for USER_1
            assertEquals(1, result.size());
            assertEquals("LOCAL CAFE", result.get(0).getOriginalName());
        }

        @Test
        @DisplayName("should find merchants by exact clean name")
        void shouldFindByCleanName() {
            // Act
            List<Merchant> result = repository.findAllByCleanName("Whole Foods");

            // Assert
            assertFalse(result.isEmpty());
            assertEquals(MERCHANT_WHOLEFOODS, result.get(0).getId());
        }

        @Test
        @DisplayName("should find merchants by name pattern")
        void shouldFindByCleanNameLike() {
            // Act
            List<Merchant> result = repository.findAllByCleanNameLike("%Whole%");

            // Assert
            assertFalse(result.isEmpty());
            assertTrue(result.stream().anyMatch(m -> m.getCleanName().contains("Whole")));
        }
    }

    @Nested
    @DisplayName("Aggregations")
    class AggregationTests {
        @Test
        @DisplayName("should calculate merchant totals from baseline transactions")
        void shouldFindMerchantTotals() {
            // Arrange
            // From baseline: USER_1 has Grocery Run transactions at Whole Foods (ID 1)
            OffsetDateTime start = LocalDate.of(2025, 9, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = LocalDate.of(2026, 3, 31).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            // Act
            List<MerchantBreakdownDto> result = repository.findMerchantTotals(USER_1, start, end);

            // Assert
            assertFalse(result.isEmpty());
            MerchantBreakdownDto breakdown = result.stream()
                    .filter(b -> b.merchant().cleanName().equals("Whole Foods"))
                    .findFirst()
                    .orElseThrow();
            
            assertTrue(breakdown.total().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new user-specific merchant")
        void shouldInsert() {
            // Arrange
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(USER_1)
                    .originalName("NEW SHOP 999")
                    .cleanName("New Shop")
                    .build();

            // Act
            Long id = repository.insert(request);

            // Assert
            assertNotNull(id);
            assertTrue(id > 0);
            List<Merchant> all = repository.findAllByUserId(USER_1);
            assertTrue(all.stream().anyMatch(m -> m.getCleanName().equals("New Shop")));
        }

        @Test
        @DisplayName("should update an existing merchant's clean name")
        void shouldUpdate() {
            // Arrange
            Merchant custom = repository.findAllByUserId(USER_1).get(0);
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(custom.getId())
                    .cleanName("Updated Cafe")
                    .build();

            // Act
            int rows = repository.update(request, USER_1);

            // Assert
            assertEquals(1, rows);
            Merchant updated = repository.findById(custom.getId()).orElseThrow();
            assertEquals("Updated Cafe", updated.getCleanName());
            assertEquals("LOCAL CAFE", updated.getOriginalName()); // Should remain unchanged
        }

        @Test
        @DisplayName("should hard delete a merchant")
        void shouldDelete() {
            // Arrange
            Merchant custom = repository.findAllByUserId(USER_1).get(0);

            // Act
            int rows = repository.delete(custom.getId());

            // Assert
            assertEquals(1, rows);
            assertTrue(repository.findById(custom.getId()).isEmpty());
        }
    }
}
