package com.mayureshpatel.pfdataservice;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Authorization Integration Tests")
class AuthorizationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory factory;

    @Test
    @DisplayName("DELETE /api/v1/transactions/{id} should return 204 when user owns transaction")
    void deleteTransaction_shouldAllowWhenOwner() throws Exception {
        // Arrange
        User user = factory.createUser("owner");
        
        Account account = factory.createAccount(user, "Owner Account");
        Merchant merchant = factory.createMerchant(user, "Merchant");
        Transaction tx = factory.createTransaction(account, merchant, null, new BigDecimal("10.00"), OffsetDateTime.now(ZoneOffset.UTC), TransactionType.EXPENSE);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/transactions/" + tx.getId())
                        .with(com.mayureshpatel.pfdataservice.security.WithCustomMockUserSecurityContextFactory.customMockUser(user)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithCustomMockUser(id = 999L, username = "stranger")
    @DisplayName("DELETE /api/v1/transactions/{id} should return 403 when user does not own transaction")
    void deleteTransaction_shouldDenyWhenNotOwner() throws Exception {
        // Arrange
        User owner = factory.createUser("real_owner");
        Account account = factory.createAccount(owner, "Owner Account");
        Merchant merchant = factory.createMerchant(owner, "Merchant");
        Transaction tx = factory.createTransaction(account, merchant, null, new BigDecimal("10.00"), OffsetDateTime.now(ZoneOffset.UTC), TransactionType.EXPENSE);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/transactions/" + tx.getId()))
                .andExpect(status().isForbidden());
    }
}
