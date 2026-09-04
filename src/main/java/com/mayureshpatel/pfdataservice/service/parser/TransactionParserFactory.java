package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Looks up the right {@link TransactionParser} implementation for a given bank name. Each
 * concrete parser declares its own {@link TransactionParser#getBankName()}; this factory indexes
 * all of them by that name at startup, via constructor injection of every
 * {@code TransactionParser} bean Spring finds.
 */
@Component
public class TransactionParserFactory {
    private final Map<BankName, TransactionParser> transactionParsers;

    /**
     * Creates a factory for transaction parsers.
     *
     * @param parsers list of transaction parsers
     */
    public TransactionParserFactory(List<TransactionParser> parsers) {
        this.transactionParsers = parsers.stream()
                .collect(Collectors.toMap(TransactionParser::getBankName, Function.identity()));
    }

    /**
     * Retrieves a transaction parser for the specified bank name.
     *
     * @param bankName the name of the bank
     * @return the transaction parser for the bank
     * @throws IllegalArgumentException if no parser is found for the bank
     */
    public TransactionParser getTransactionParser(String bankName) {
        BankName bank = BankName.fromString(bankName);
        TransactionParser transactionParser = this.transactionParsers.get(bank);

        if (transactionParser == null) {
            throw new IllegalArgumentException("No parser found for bank: " + bankName);
        }
        return transactionParser;
    }
}
