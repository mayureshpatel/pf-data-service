package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code AccountDtoMapper}'s static {@code toDto} mapping (in {@link ToDtoMappingTests}), plus confirms the private constructor of this static-utility-only class is instantiable via reflection, solely to satisfy coverage tooling. */
@DisplayName("AccountDtoMapper Unit Tests")
class AccountDtoMapperTest {

    @Test
    @DisplayName("Private constructor should not be accessible but can be called for coverage")
    void testPrivateConstructor() throws Exception {
        // arrange
        Constructor<AccountDtoMapper> constructor = AccountDtoMapper.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);

        // act
        AccountDtoMapper instance = constructor.newInstance();

        // assert & verify
        assertNotNull(instance);
    }

    /**
     * {@code toDto} returns null for a null input, maps every field (nesting the user/type/
     * currency/bank into their own sub-DTOs) when fully populated, and leaves each nested DTO null
     * -- without throwing -- when its source field is null. A fixed bug: a blank (not null) bank
     * code is treated the same as absent rather than passed to {@code BankName.fromString("")},
     * which previously threw {@link IllegalArgumentException} and broke {@code GET /accounts} for
     * the whole user -- every account in the list failed to map, not just the one with a blank
     * bank, since the frontend's account-creation form sends {@code ""} rather than {@code null}
     * when no bank is selected.
     */
    @Nested
    @DisplayName("toDto mapping logic")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when account is null")
        void toDto_shouldReturnNullWhenAccountIsNull() {
            // arrange
            Account account = null;

            // act
            AccountDto result = AccountDtoMapper.toDto(account);

            // assert & verify
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when account is fully populated")
        void toDto_shouldMapAllFields() {
            // arrange
            Account account = Account.builder()
                    .id(1L)
                    .userId(100L)
                    .name("Checking")
                    .type(AccountType.builder().code("CHECKING").asset(true).build())
                    .currentBalance(new BigDecimal("1500.00"))
                    .currency(Currency.builder().code("USD").build())
                    .bankCode("CAPITAL_ONE")
                    .build();

            // act
            AccountDto dto = AccountDtoMapper.toDto(account);

            // assert & verify
            assertNotNull(dto);
            assertEquals(account.getId(), dto.id());
            assertEquals(account.getName(), dto.name());
            assertEquals(account.getCurrentBalance(), dto.currentBalance());

            assertNotNull(dto.user());
            assertEquals(account.getUserId(), dto.user().id());

            assertNotNull(dto.type());
            assertEquals(account.getType().getCode(), dto.type().code());

            assertNotNull(dto.currency());
            assertEquals(account.getCurrency().getCode(), dto.currency().code());

            assertNotNull(dto.bank());
            assertEquals("CAPITAL_ONE", dto.bank().name());
        }

        @Test
        @DisplayName("should handle null optional fields")
        void toDto_shouldHandleNullOptionals() {
            // arrange
            Account account = Account.builder()
                    .id(1L)
                    .name("Minimal Account")
                    .userId(null)
                    .type(null)
                    .currency(null)
                    .bankCode(null)
                    .build();

            // act
            AccountDto dto = AccountDtoMapper.toDto(account);

            // assert & verify
            assertNotNull(dto);
            assertEquals(account.getId(), dto.id());
            assertEquals(account.getName(), dto.name());
            assertNull(dto.user());
            assertNull(dto.type());
            assertNull(dto.currency());
            assertNull(dto.bank());
        }

        @Test
        @DisplayName("bug regression: should treat a blank bank code as absent, not throw -- "
                + "the frontend sends an empty string (not null) when no bank is selected on "
                + "create (account-form-drawer.component.ts's `rawValue.bankName ?? ''`), which "
                + "previously slipped past this mapper's null-only guard into BankName.fromString(''),"
                + " throwing IllegalArgumentException and breaking GET /accounts for the whole user "
                + "-- every account in the list fails to map, not just the one with a blank bank")
        void toDto_shouldTreatBlankBankCodeAsAbsent() {
            // arrange
            Account account = Account.builder()
                    .id(1L)
                    .name("No Bank Account")
                    .bankCode("")
                    .build();

            // act
            AccountDto dto = AccountDtoMapper.toDto(account);

            // assert & verify
            assertNotNull(dto);
            assertNull(dto.bank());
            assertNull(dto.bank());
        }

        @Test
        @DisplayName("should show a liability's balance as a negative number whichever sign it is stored with")
        void toDto_shouldShowLiabilityBalanceAsNegative() {
            Account loan = Account.builder().id(1L).name("Car loan")
                    .type(AccountType.builder().code("LOAN").asset(false).build())
                    .currentBalance(new BigDecimal("25358.00")).build();
            Account card = Account.builder().id(2L).name("Card")
                    .type(AccountType.builder().code("CREDIT_CARD").asset(false).build())
                    .currentBalance(new BigDecimal("-1750.76")).build();

            assertEquals(new BigDecimal("-25358.00"), AccountDtoMapper.toDto(loan).currentBalance());
            assertEquals(new BigDecimal("-1750.76"), AccountDtoMapper.toDto(card).currentBalance());
        }

        @Test
        @DisplayName("should leave an asset's balance as it is")
        void toDto_shouldKeepAssetBalance() {
            Account savings = Account.builder().id(1L).name("Savings")
                    .type(AccountType.builder().code("SAVINGS").asset(true).build())
                    .currentBalance(new BigDecimal("-5.00")).build();

            assertEquals(new BigDecimal("-5.00"), AccountDtoMapper.toDto(savings).currentBalance());
        }
    }
}
