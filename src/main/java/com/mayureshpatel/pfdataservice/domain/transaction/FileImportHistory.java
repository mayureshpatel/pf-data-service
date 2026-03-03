package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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
    private CreatedAtAudit audit;
}
