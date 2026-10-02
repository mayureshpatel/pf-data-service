package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A record of one previously-imported CSV statement file, keyed by {@code fileHash} so the same
 * file can't be imported twice for the same account -- {@code transactionCount} is the number of
 * transactions that import actually created, kept here rather than recomputed for a quick history
 * display.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FileImportHistory {

    @EqualsAndHashCode.Include
    private Long id;
    private Account account;
    private String fileName;
    private String fileHash;
    private int transactionCount;

    @ToString.Exclude
    private TableAudit audit;
}
