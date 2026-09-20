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
    private final String name;

    @Size(max = 120, message = "City must be less than 120 characters.")
    private final String city;

    @Size(max = 120, message = "State must be less than 120 characters.")
    private final String state;

    @Size(max = 20, message = "Postal code must be less than 20 characters.")
    private final String postalCode;

    @Size(max = 60, message = "Country must be less than 60 characters.")
    private final String country;

    /**
     * Default constructor.
     */
    public MerchantCreateRequest() {
        this.userId = null;
        this.name = null;
        this.city = null;
        this.state = null;
        this.postalCode = null;
        this.country = null;
    }

    /**
     * All-args constructor.
     *
     * @param userId     the user id
     * @param name       the merchant's deliberately-chosen name
     * @param city       the merchant's city, optional
     * @param state      the merchant's state/province, optional
     * @param postalCode the merchant's postal code, optional
     * @param country    the merchant's country, optional
     */
    public MerchantCreateRequest(Long userId, String name, String city, String state, String postalCode, String country) {
        this.userId = userId;
        this.name = name;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
}
