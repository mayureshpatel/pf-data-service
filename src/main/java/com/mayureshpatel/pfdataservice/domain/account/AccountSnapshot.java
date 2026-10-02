package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A point-in-time balance reading for an account, one per {@code accountId}/{@code snapshotDate}
 * pair. Exists specifically to back historical/trend reporting (e.g. net worth over time) without
 * having to replay the full transaction history from an account's origin on every request.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccountSnapshot {

    @EqualsAndHashCode.Include
    private Long id;
    private Long accountId;
    private Account account;
    private LocalDate snapshotDate;
    @ToString.Exclude
    private BigDecimal balance;

    @ToString.Exclude
    private TableAudit audit;
}
