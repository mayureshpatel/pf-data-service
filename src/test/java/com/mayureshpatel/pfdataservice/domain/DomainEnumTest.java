package com.mayureshpatel.pfdataservice.domain;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Enum and Support Object Tests")
class DomainEnumTest {

    @Nested
    @DisplayName("TransactionType logic")
    class TransactionTypeTests {
        @Test
        void shouldIdentifyIncome() {
            assertTrue(TransactionType.INCOME.isIncome());
            assertTrue(TransactionType.TRANSFER_IN.isIncome());
            assertFalse(TransactionType.EXPENSE.isIncome());
        }

        @Test
        void shouldIdentifyExpense() {
            assertTrue(TransactionType.EXPENSE.isExpense());
            assertTrue(TransactionType.TRANSFER_OUT.isExpense());
            assertFalse(TransactionType.INCOME.isExpense());
        }

        @Test
        void shouldIdentifyTransfer() {
            assertTrue(TransactionType.TRANSFER.isTransfer());
            assertFalse(TransactionType.TRANSFER_IN.isTransfer());
        }

        @Test
        void shouldIdentifyAdjustment() {
            assertTrue(TransactionType.ADJUSTMENT.isAdjustment());
            assertFalse(TransactionType.INCOME.isAdjustment());
        }
    }

    @Nested
    @DisplayName("Frequency logic")
    class FrequencyTests {
        @Test
        void shouldCreateFromCode() {
            assertEquals(Frequency.MONTHLY, Frequency.fromCode("monthly"));
            assertEquals(Frequency.WEEKLY, Frequency.fromCode("WEEKLY"));
        }

        @Test
        void shouldThrowOnInvalidFrequency() {
            assertThrows(IllegalArgumentException.class, () -> Frequency.fromCode("invalid"));
        }
    }

    @Nested
    @DisplayName("CategoryType logic")
    class CategoryTypeTests {
        @Test
        void shouldCreateFromValue() {
            assertEquals(CategoryType.EXPENSE, CategoryType.fromValue("expense"));
            assertEquals(CategoryType.INCOME, CategoryType.fromValue("INCOME"));
        }

        @Test
        void shouldThrowOnInvalidValue() {
            assertThrows(IllegalArgumentException.class, () -> CategoryType.fromValue("invalid"));
        }
    }

    @Nested
    @DisplayName("BankName logic")
    class BankNameTests {
        @Test
        void shouldCreateFromString() {
            assertEquals(BankName.CAPITAL_ONE, BankName.fromString("CAPITAL_ONE"));
            assertEquals(BankName.CAPITAL_ONE, BankName.fromString("Capital One"));
        }

        @Test
        void shouldThrowOnInvalidString() {
            assertThrows(IllegalArgumentException.class, () -> BankName.fromString("invalid"));
        }

        @Test
        void shouldGetDisplayName() {
            assertEquals("Capital One", BankName.CAPITAL_ONE.getDisplayName());
        }
    }

    @Nested
    @DisplayName("Iconography logic")
    class IconographyTests {
        @Test
        void shouldStoreData() {
            Iconography icon = new Iconography("pi-wallet", "blue");
            assertEquals("pi-wallet", icon.getIcon());
            assertEquals("blue", icon.getColor());

            icon.setIcon("pi-card");
            icon.setColor("red");
            assertEquals("pi-card", icon.getIcon());
            assertEquals("red", icon.getColor());
        }

        @Test
        void shouldHaveNoArgConstructor() {
            Iconography icon = new Iconography();
            assertNull(icon.getIcon());
        }
    }
}
