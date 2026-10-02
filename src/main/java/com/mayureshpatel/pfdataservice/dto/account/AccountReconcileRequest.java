package com.mayureshpatel.pfdataservice.dto.account;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * The request payload for reconciling an account to a known-correct {@code newBalance} (e.g.
 * after checking a real bank statement). {@code version} is the optimistic-locking token the
 * caller last read -- required so a reconcile against a stale balance fails loudly instead of
 * silently overwriting a concurrent change.
 */
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
