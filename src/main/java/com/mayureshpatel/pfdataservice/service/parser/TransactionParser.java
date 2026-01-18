package com.mayureshpatel.pfdataservice.service.parser;

import com.mayureshpatel.pfdataservice.model.BankName;
import com.mayureshpatel.pfdataservice.model.Transaction;

import java.io.InputStream;
import java.util.List;

public interface TransactionParser {
    List<Transaction> parse(Long accountId, InputStream inputStream);

    BankName getBankName();
}