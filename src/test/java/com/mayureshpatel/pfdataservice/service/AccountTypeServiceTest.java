package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountTypeService Unit Tests")
class AccountTypeServiceTest {

    @Mock
    private AccountTypeRepository accountTypeRepository;

    @InjectMocks
    private AccountTypeService accountTypeService;

    private static final String TYPE_CODE = "CHECKING";

    @Nested
    @DisplayName("getAllActiveAccountTypes")
    class GetAllActiveAccountTypesTests {
        @Test
        @DisplayName("should map every repository result to an AccountTypeDto")
        void shouldMapRepositoryResultsToDtos() {
            // arrange
            AccountType type = AccountType.builder()
                    .code(TYPE_CODE).label("Checking").active(true).sortOrder(1).asset(true).build();
            when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(type));

            // act
            List<AccountTypeDto> result = accountTypeService.getAllActiveAccountTypes();

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(TYPE_CODE, result.get(0).code());
            assertEquals("Checking", result.get(0).label());
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {
        @Test
        @DisplayName("should delegate to the repository and return the inserted row count")
        void shouldDelegateToRepository() {
            // arrange
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .code("SAVINGS").label("Savings").isAsset(true).sortOrder(2).isActive(true).build();
            when(accountTypeRepository.insert(request)).thenReturn(1);

            // act
            int result = accountTypeService.create(request);

            // assert & verify
            assertEquals(1, result);
            verify(accountTypeRepository).insert(request);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {
        @Test
        @DisplayName("should delegate to the repository and return the deleted row count")
        void shouldDelegateToRepository() {
            // arrange
            when(accountTypeRepository.deleteByCode(TYPE_CODE)).thenReturn(1);

            // act
            int result = accountTypeService.delete(TYPE_CODE);

            // assert & verify
            assertEquals(1, result);
            verify(accountTypeRepository).deleteByCode(TYPE_CODE);
        }
    }
}
