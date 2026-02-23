package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TransactionParserFactory unit tests")
class TransactionParserFactoryTest {

    private TransactionParser parserFor(BankName bankName) {
        TransactionParser p = mock(TransactionParser.class);
        when(p.getBankName()).thenReturn(bankName);
        return p;
    }

    @Nested
    @DisplayName("getTransactionParser() — known banks")
    class KnownBankTests {

        @ParameterizedTest(name = "bankName=''{0}'' should resolve to {1}")
        @CsvSource({
                "CAPITAL_ONE, CAPITAL_ONE",
                "Capital One, CAPITAL_ONE",
                "capital one, CAPITAL_ONE",
                "DISCOVER, DISCOVER",
                "Discover, DISCOVER",
                "discover, DISCOVER",
                "SYNOVUS, SYNOVUS",
                "Synovus, SYNOVUS",
                "STANDARD, STANDARD",
                "Standard CSV, STANDARD",
                "UNIVERSAL, UNIVERSAL",
                "Universal CSV, UNIVERSAL"
        })
        @DisplayName("should return the correct parser for both enum name and display name")
        void getTransactionParser_knownBankName_returnsCorrectParser(String bankName, BankName expectedBank) {
            TransactionParser parser = parserFor(expectedBank);
            TransactionParserFactory factory = new TransactionParserFactory(List.of(parser));

            TransactionParser result = factory.getTransactionParser(bankName);

            assertThat(result).isSameAs(parser);
        }

        @Test
        @DisplayName("should correctly resolve all five registered parsers")
        void getTransactionParser_allParsersRegistered_allResolvable() {
            TransactionParser capitalOne = parserFor(BankName.CAPITAL_ONE);
            TransactionParser discover = parserFor(BankName.DISCOVER);
            TransactionParser synovus = parserFor(BankName.SYNOVUS);
            TransactionParser standard = parserFor(BankName.STANDARD);
            TransactionParser universal = parserFor(BankName.UNIVERSAL);

            TransactionParserFactory factory = new TransactionParserFactory(
                    List.of(capitalOne, discover, synovus, standard, universal));

            assertThat(factory.getTransactionParser("CAPITAL_ONE")).isSameAs(capitalOne);
            assertThat(factory.getTransactionParser("DISCOVER")).isSameAs(discover);
            assertThat(factory.getTransactionParser("SYNOVUS")).isSameAs(synovus);
            assertThat(factory.getTransactionParser("STANDARD")).isSameAs(standard);
            assertThat(factory.getTransactionParser("UNIVERSAL")).isSameAs(universal);
        }
    }

    @Nested
    @DisplayName("getTransactionParser() — unknown banks")
    class UnknownBankTests {

        @Test
        @DisplayName("should throw IllegalArgumentException for an unrecognized bank name")
        void getTransactionParser_unknownBankName_throwsIllegalArgumentException() {
            TransactionParserFactory factory = new TransactionParserFactory(List.of());

            assertThatThrownBy(() -> factory.getTransactionParser("UnknownBank"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw exception when a recognized bank has no registered parser")
        void getTransactionParser_recognizedBankButNoParser_throwsIllegalArgumentException() {
            // Only register Discover parser; requesting Capital One should fail
            TransactionParser discoverOnly = parserFor(BankName.DISCOVER);
            TransactionParserFactory factory = new TransactionParserFactory(List.of(discoverOnly));

            assertThatThrownBy(() -> factory.getTransactionParser("CAPITAL_ONE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CAPITAL_ONE");
        }
    }
}
