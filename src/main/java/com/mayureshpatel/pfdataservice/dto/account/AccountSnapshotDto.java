package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountSnapshotDto(
        Long id,
        AccountDto account,
        OffsetDateTime snapshotDate,
        BigDecimal balance
) {

    /**
     * Maps an {@link AccountSnapshot} domain object to its corresponding DTO representation.
     *
     * @param accountSnapshot The AccountSnapshot domain object to be mapped.
     * @return The {@link AccountSnapshotDto} representation of the provided AccountSnapshot.
     */
    public static AccountSnapshotDto mapToDto(AccountSnapshot accountSnapshot) {
        return new AccountSnapshotDto(
                accountSnapshot.getId(),
                AccountDto.fromDomain(accountSnapshot.getAccount()),
                accountSnapshot.getSnapshotDate(),
                accountSnapshot.getBalance()
        );
    }
}
