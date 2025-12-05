package com.mayureshpatel.pfdataservice.service.parser;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TransactionParserFactory {
    private final Map<String, TransactionParser> transactionParsers;

    public TransactionParserFactory(List<TransactionParser> parsers) {
        this.transactionParsers = parsers.stream()
                .collect(Collectors.toMap(TransactionParser::getBankName, Function.identity()));
    }

    public TransactionParser getTransactionParser(String bankName) {
        TransactionParser transactionParser = transactionParsers.get(bankName.toUpperCase());

        if (transactionParser == null) {
            throw new IllegalArgumentException("No parser found for bank: " + bankName);
        }
        return transactionParser;
    }
}
