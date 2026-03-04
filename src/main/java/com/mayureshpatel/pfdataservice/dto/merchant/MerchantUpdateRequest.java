package com.mayureshpatel.pfdataservice.dto.merchant;

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
public class MerchantUpdateRequest {

    @NotNull(message = "Merchant ID cannot be null.")
    @Positive(message = "Merchant ID must be a positive number.")
    private final Long id;

    @NotBlank(message = "Merchant name cannot be blank.")
    @Size(max = 255, message = "Merchant name must be less than 255 characters.")
    private final String cleanName;

    /**
     * Default constructor.
     */
    public MerchantUpdateRequest() {
        this.id = null;
        this.cleanName = null;
    }

    /**
     * All-args constructor.
     *
     * @param cleanName the cleaned merchant name
     */
    public MerchantUpdateRequest(Long id, String cleanName) {
        this.id = id;
        this.cleanName = cleanName;
    }
}
