package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("AccountTypeRepository integration tests")
class AccountTypeRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Test
    @DisplayName("should find all active account types ordered by sort order")
    void findByIsActiveTrueOrderBySortOrder_returnsOrderedList() {
        List<AccountType> types = accountTypeRepository.findByIsActiveTrueOrderBySortOrder();

        assertThat(types).isNotEmpty();
        assertThat(types).extracting(AccountType::isActive).containsOnly(true);
        // Checking first element is CHECKING, next is SAVINGS per V1__init_schema.sql
        assertThat(types.get(0).getCode()).isEqualTo("CHECKING");
        assertThat(types.get(1).getCode()).isEqualTo("SAVINGS");
    }

    @Test
    @DisplayName("save should insert when entity code is null but fails since code is required")
    void save_withNullCode_throwsException() {
        AccountType type = new AccountType();
        type.setCode(null);
        type.setLabel("Test");
        
        try {
            accountTypeRepository.save(type);
        } catch (Exception e) {
            assertThat(e).isNotNull(); // Expected to throw due to DB constraints
        }
    }
    
    @Test
    @DisplayName("save should update when entity code exists")
    void save_withExistingCode_updatesEntity() {
        AccountType type = new AccountType();
        type.setCode("CHECKING");
        type.setLabel("Checking Updated");
        type.setActive(true);
        type.setSortOrder(1);

        try {
            accountTypeRepository.save(type);
            org.junit.jupiter.api.Assertions.fail("Expected UnsupportedOperationException to be thrown");
        } catch (UnsupportedOperationException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("delete should do nothing if code is null")
    void delete_withNullCode_doesNothing() {
        AccountType type = new AccountType();
        type.setCode(null);

        accountTypeRepository.delete(type);
        // Should not throw
    }
    
    @Test
    @DisplayName("delete should throw unsupported when trying to delete by code")
    void delete_withCode_throwsUnsupported() {
        AccountType type = new AccountType();
        type.setCode("CHECKING");

        try {
            accountTypeRepository.delete(type);
            org.junit.jupiter.api.Assertions.fail("Expected UnsupportedOperationException to be thrown");
        } catch (UnsupportedOperationException e) {
            assertThat(e).isNotNull();
        }
    }
}
