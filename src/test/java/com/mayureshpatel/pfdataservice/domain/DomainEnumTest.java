package com.mayureshpatel.pfdataservice.domain;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Groups the small, otherwise-homeless behavioral tests for several unrelated domain enums/support types under one file rather than one file each. */
@DisplayName("Domain Enum and Support Object Tests")
class DomainEnumTest {

    /** {@link TransactionType}'s {@code isIncome}/{@code isExpense}/{@code isTransfer}/{@code isAdjustment} classification methods. */
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

    /** {@link Frequency#fromCode(String)}'s case-insensitive lookup and its failure on an unknown code. */
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

    /** {@link CategoryType#fromValue(String)}'s case-insensitive lookup and its failure on an unknown value. */
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

    /** {@link BankName#fromString(String)}'s lookup by both its enum name and its display name, plus {@link BankName#getDisplayName()}. */
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

    /** {@link Iconography}'s getters/setters and its no-arg constructor. */
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
