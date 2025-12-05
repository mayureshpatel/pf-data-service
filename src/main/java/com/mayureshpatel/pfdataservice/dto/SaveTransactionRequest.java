package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.model.Transaction;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveTransactionRequest {
    @NotEmpty(message = "Transaction list cannot be empty")
    private List<Transaction> transactions;
    private String fileName;
    private String fileHash;
}
