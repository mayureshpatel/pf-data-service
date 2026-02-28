package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.util.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionDtoMapper unit tests")
class TransactionDtoMapperTest {

    @Test
    @DisplayName("should return null when transaction is null")
    void toDto_nullTransaction_returnsNull() {
        assertThat(TransactionDtoMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("should map all fields correctly including nested DTOs")
    void toDto_fullTransaction_mapsAllFields() {
        Transaction tx = TestFixtures.aTransaction();

        TransactionDto dto = TransactionDtoMapper.toDto(tx);

        assertThat(dto.id()).isEqualTo(tx.getId());
        assertThat(dto.date()).isEqualTo(tx.getTransactionDate());
        assertThat(dto.postDate()).isEqualTo(tx.getPostDate());
        assertThat(dto.description()).isEqualTo(tx.getDescription());
        assertThat(dto.amount()).isEqualByComparingTo(tx.getAmount());
        assertThat(dto.type()).isEqualTo(tx.getType());

        // Nested DTOs
        assertThat(dto.merchant()).isNotNull();
        assertThat(dto.merchant().id()).isEqualTo(tx.getMerchant().getId());
        assertThat(dto.category()).isNotNull();
        assertThat(dto.category().id()).isEqualTo(tx.getCategory().getId());
        assertThat(dto.account()).isNotNull();
        assertThat(dto.account().id()).isEqualTo(tx.getAccount().getId());
    }

    @Test
    @DisplayName("should handle null merchant, category, and account")
    void toDto_nullNestedObjects_mapsNestedAsNull() {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setDescription("Bare transaction");

        TransactionDto dto = TransactionDtoMapper.toDto(tx);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.description()).isEqualTo("Bare transaction");
        assertThat(dto.merchant()).isNull();
        assertThat(dto.category()).isNull();
        assertThat(dto.account()).isNull();
    }
}
