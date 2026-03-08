package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class AccountUpdateRequest {

    @NotNull(message = "Account ID cannot be null.")
    @Positive(message = "Account ID must be a positive number.")
    private final Long id;

    @NotBlank(message = "Account name cannot be blank.")
    @Size(max = 100, message = "Account name must be less than 100 characters.")
    private final String name;

    @NotBlank(message = "Account type cannot be blank.")
    @Size(max = 20, message = "Account type must be less than 20 characters.")
    private final String type;

    @NotBlank(message = "Currency code cannot be blank.")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters.")
    private final String currencyCode;

    @Size(max = 50, message = "Bank name must be less than 50 characters.")
    private final String bankName;

    @NotNull(message = "Version cannot be null.")
    @Positive(message = "Version must be a positive number.")
    private final Long version;

    /**
     * Returns the domain object representation of this request.
     *
     * @return the domain object representation of this request.
     */
    public Account toDomain() {
        return Account.builder()
                .id(id)
                .name(name)
                .type(AccountType.builder().code(type).build())
                .currency(Currency.builder().code(currencyCode).build())
                .bankCode(bankName)
                .version(version)
                .build();
    }
}
