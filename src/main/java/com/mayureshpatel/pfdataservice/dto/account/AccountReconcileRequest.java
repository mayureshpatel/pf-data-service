package com.mayureshpatel.pfdataservice.dto.account;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountReconcileRequest {
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    @NotNull(message = "New balance is required")
    private BigDecimal newBalance;

    @NotNull(message = "Version is required")
    private Long version;
}
