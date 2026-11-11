package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.BalanceMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies how {@link AccountUpdateRequest#toDomain()} carries the optional balance mode. */
@DisplayName("AccountUpdateRequest")
class AccountUpdateRequestTest {

    private AccountUpdateRequest.AccountUpdateRequestBuilder base() {
        return AccountUpdateRequest.builder().id(1L).name("A").type("CHECKING").currencyCode("USD").version(1L);
    }

    @Test
    @DisplayName("toDomain keeps the builder default (never null) when no balance mode is given")
    void toDomain_shouldKeepDefaultWhenModeAbsent() {
        // act
        Account account = base().build().toDomain();

        // assert & verify
        assertEquals(BalanceMode.DERIVED, account.getBalanceMode());
    }

    @Test
    @DisplayName("toDomain maps an explicit balance mode")
    void toDomain_shouldMapExplicitMode() {
        // act
        Account account = base().balanceMode("MANUAL").build().toDomain();

        // assert & verify
        assertEquals(BalanceMode.MANUAL, account.getBalanceMode());
    }
}
