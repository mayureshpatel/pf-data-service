package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.model.BankName;
import com.mayureshpatel.pfdataservice.model.Transaction;

import java.io.InputStream;
import java.util.stream.Stream;

public interface TransactionParser {
    Stream<Transaction> parse(Long accountId, InputStream inputStream);

    BankName getBankName();
}