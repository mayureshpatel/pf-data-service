package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeCreateRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(AccountTypeRepository.class)
@DisplayName("AccountTypeRepository Integration Tests (PostgreSQL)")
class AccountTypeRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AccountTypeRepository repository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {
        @Test
        @DisplayName("should insert and find active account types ordered by sort order")
        void shouldInsertAndFind() {
            // Arrange
            repository.insert(AccountTypeCreateRequest.builder()
                    .code("TST1")
                    .label("Test 1")
                    .icon("icon1")
                    .color("#111")
                    .isAsset(true)
                    .sortOrder(10)
                    .isActive(true)
                    .build());

            repository.insert(AccountTypeCreateRequest.builder()
                    .code("TST2")
                    .label("Test 2")
                    .icon("icon2")
                    .color("#222")
                    .isAsset(false)
                    .sortOrder(5) // Should appear before TST1
                    .isActive(true)
                    .build());

            // Act
            List<AccountType> result = repository.findByIsActiveTrueOrderBySortOrder();

            // Assert
            assertTrue(result.size() >= 2);
            // Verify order based on sortOrder
            assertTrue(result.indexOf(result.stream().filter(a -> a.getCode().equals("TST2")).findFirst().get()) <
                       result.indexOf(result.stream().filter(a -> a.getCode().equals("TST1")).findFirst().get()));
        }

        @Test
        @DisplayName("should delete by code")
        void shouldDeleteByCode() {
            // Arrange
            repository.insert(AccountTypeCreateRequest.builder()
                    .code("DEL1")
                    .label("Delete Me")
                    .sortOrder(1)
                    .isActive(true)
                    .build());

            // Act
            int rows = repository.deleteByCode("DEL1");

            // Assert
            assertEquals(1, rows);
            List<AccountType> types = repository.findByIsActiveTrueOrderBySortOrder();
            assertTrue(types.stream().noneMatch(a -> a.getCode().equals("DEL1")));
        }

        @Test
        @DisplayName("should delete by entity")
        void shouldDeleteEntity() {
            // Arrange
            repository.insert(AccountTypeCreateRequest.builder()
                    .code("DEL2")
                    .label("Delete Me 2")
                    .sortOrder(1)
                    .isActive(true)
                    .build());
            AccountType entity = AccountType.builder().code("DEL2").build();

            // Act
            int rows = repository.delete(entity);

            // Assert
            assertEquals(1, rows);
        }

        @Test
        @DisplayName("should return 0 when deleting entity with null code")
        void shouldHandleNullCodeDelete() {
            assertEquals(0, repository.delete(AccountType.builder().build()));
        }
    }
}
