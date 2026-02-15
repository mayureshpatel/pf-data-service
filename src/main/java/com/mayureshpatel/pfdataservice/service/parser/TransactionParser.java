package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.repository.account.model.BankName;
import com.mayureshpatel.pfdataservice.repository.transaction.model.Transaction;

import java.io.InputStream;
import java.util.stream.Stream;

public interface TransactionParser {
    Stream<Transaction> parse(Long accountId, InputStream inputStream);

    BankName getBankName();
}