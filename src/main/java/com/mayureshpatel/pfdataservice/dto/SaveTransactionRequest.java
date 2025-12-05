package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveTransactionRequest {
    private List<Transaction> transactions;
    private String fileName;
    private String fileHash;
}
