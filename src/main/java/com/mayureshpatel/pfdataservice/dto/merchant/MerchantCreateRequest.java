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
public class MerchantCreateRequest {

    @NotNull(message = "User ID cannot be null.")
    @Positive(message = "User ID must be a positive number.")
    private final Long userId;

    @NotBlank(message = "Merchant name cannot be blank.")
    @Size(max = 255, message = "Merchant name must be less than 255 characters.")
    private final String originalName;

    @NotBlank(message = "Merchant name cannot be blank.")
    @Size(max = 255, message = "Merchant name must be less than 255 characters.")
    private final String cleanName;

    /**
     * Default constructor.
     */
    public MerchantCreateRequest() {
        this.userId = null;
        this.originalName = null;
        this.cleanName = null;
    }

    /**
     * All-args constructor.
     *
     * @param userId       the user id
     * @param originalName the original merchant name
     * @param cleanName    the cleaned merchant name
     */
    public MerchantCreateRequest(Long userId, String originalName, String cleanName) {
        this.userId = userId;
        this.originalName = originalName;
        this.cleanName = cleanName;
    }
}
