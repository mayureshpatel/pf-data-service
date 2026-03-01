package com.mayureshpatel.pfdataservice;

import com.mayureshpatel.pfdataservice.config.TestContainersConfig;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@DisplayName("Account Functional Tests")
class AccountFunctionalTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestDataFactory factory;

    @Test
    @WithCustomMockUser(id = 1001L, username = "functional_user")
    @DisplayName("GET /api/v1/accounts should return accounts via real HTTP stack")
    void getAccounts_shouldReturnAccounts() {
        // Arrange
        User user = factory.createUser("functional_user");
        Account account = factory.createAccount(user, "Savings");
        
        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/accounts")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(AccountDto.class)
                .consumeWith(response -> {
                    var accounts = response.getResponseBody();
                    assertThat(accounts).isNotNull().isNotEmpty();
                    assertThat(accounts.get(0).name()).isEqualTo("Savings");
                });
    }

    @Test
    @DisplayName("GET /api/v1/accounts without auth should return 401/403")
    void getAccounts_unauthenticated_shouldFail() {
        webTestClient.get()
                .uri("/api/v1/accounts")
                .exchange()
                .expectStatus().isForbidden(); // Depending on SecurityConfig, might be 401 or 403
    }
}
