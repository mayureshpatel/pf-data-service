package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class MerchantUpdateRequest {

    @NotBlank(message = "Merchant name cannot be blank.")
    @Size(max = 255, message = "Merchant name must be less than 255 characters.")
    private final String cleanName;

    /**
     * Default constructor.
     */
    public MerchantUpdateRequest() {
        this.cleanName = null;
    }

    /**
     * All-args constructor.
     *
     * @param cleanName the cleaned merchant name
     */
    public MerchantUpdateRequest(String cleanName) {
        this.cleanName = cleanName;
    }
}
