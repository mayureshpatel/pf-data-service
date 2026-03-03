package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@ToString(exclude = "startingBalance")
public class AccountCreateRequest {

    private final Long userId;
    private final String name;
    private final String type;
    private BigDecimal startingBalance;
    private final String currencyCode;
    private final String bankName;

    public Account toDomain() {
        return Account.builder()
                .userId(userId)
                .name(name)
                .typeCode(type)
                .currentBalance(startingBalance)
                .currencyCode(currencyCode)
                .bankCode(bankName)
                .build();
    }
}
