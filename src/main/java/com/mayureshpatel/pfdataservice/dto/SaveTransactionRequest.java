package com.mayureshpatel.pfdataservice.dto;

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
    private List<TransactionDto> transactions;
    private String fileName;
    private String fileHash;
}