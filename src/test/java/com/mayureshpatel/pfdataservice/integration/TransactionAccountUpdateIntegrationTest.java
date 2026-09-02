package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PF-194: editing a transaction's account must actually move it, and correctly adjust both the
 * old and new account's balances -- not silently discard the change. Exercises the real
 * controller, service, and repositories against a real Postgres, since the bug and its fix live
 * in exactly the layer (Jackson deserialization + a two-account balance reconciliation inside one
 * {@code @Transactional} method) that a mocked unit test can't observe end-to-end.
 */
@AutoConfigureMockMvc
class TransactionAccountUpdateIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("PUT /transactions should move a transaction to a different account and correct both balances (PF-194)")
    @WithCustomMockUser(id = 200L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (200, 'pf194user', 'pf194@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (200, 200, 'Old Account', 'CHECKING', 1000.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (201, 200, 'New Account', 'CHECKING', 500.00, 'STANDARD') ON CONFLICT DO NOTHING;",
            "INSERT INTO transactions (id, account_id, amount, date, description, type) VALUES (200, 200, 10.00, NOW(), 'Move me', 'EXPENSE') ON CONFLICT DO NOTHING;"
    })
    void shouldMoveTransactionAndCorrectBothBalances() throws Exception {
        // Arrange -- move a $10 EXPENSE from the old account (id 200) to the new one (id 201)
        TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                .id(200L)
                .accountId(201L)
                .amount(new BigDecimal("10.00"))
                .transactionDate(OffsetDateTime.now())
                .description("Move me")
                .type("EXPENSE")
                .build();

        // Act
        mockMvc.perform(put("/api/v1/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Assert -- read persisted state back through the real repositories, not mocks
        Transaction moved = transactionRepository.findById(200L, 200L).orElseThrow();
        assertEquals(201L, moved.getAccount().getId());
        // PF-215: the enriched query's embedded account must also correctly hydrate its type,
        // not just its id -- a second instance of the same column-alias mismatch class
        assertEquals("CHECKING", moved.getAccount().getType().getCode());

        Account oldAccount = accountRepository.findById(200L).orElseThrow();
        Account newAccount = accountRepository.findById(201L).orElseThrow();
        // old account: undoing the $10 EXPENSE raises its balance (1000 -> 1010)
        assertEquals(0, new BigDecimal("1010.00").compareTo(oldAccount.getCurrentBalance()));
        // new account: applying the $10 EXPENSE lowers its balance (500 -> 490)
        assertEquals(0, new BigDecimal("490.00").compareTo(newAccount.getCurrentBalance()));
    }
}
