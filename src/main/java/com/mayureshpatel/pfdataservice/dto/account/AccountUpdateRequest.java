package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class AccountUpdateRequest {

    private final Long id;
    private final String name;
    private final String type;
    private final String currencyCode;
    private final String bankName;
    private final Long version;

    public Account toDomain() {
        return Account.builder()
                .id(id)
                .name(name)
                .typeCode(type)
                .currencyCode(currencyCode)
                .bankCode(bankName)
                .version(version)
                .build();
    }
}
