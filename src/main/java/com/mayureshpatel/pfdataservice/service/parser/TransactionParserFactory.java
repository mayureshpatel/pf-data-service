package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.model.BankName;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TransactionParserFactory {
    private final Map<BankName, TransactionParser> transactionParsers;

    public TransactionParserFactory(List<TransactionParser> parsers) {
        this.transactionParsers = parsers.stream()
                .collect(Collectors.toMap(TransactionParser::getBankName, Function.identity()));
    }

    public TransactionParser getTransactionParser(String bankName) {
        BankName bank = BankName.fromString(bankName);
        TransactionParser transactionParser = transactionParsers.get(bank);

        if (transactionParser == null) {
            throw new IllegalArgumentException("No parser found for bank: " + bankName);
        }
        return transactionParser;
    }
}
