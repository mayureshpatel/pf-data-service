package com.mayureshpatel.pfdataservice.dto.transaction.tags;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/** The request payload for updating an existing {@code Tag}'s name/color, identified by {@code id}. */
@Getter
@Builder(toBuilder = true)
@ToString
public class TagUpdateRequest {

    @NotNull(message = "Tag ID cannot be null.")
    @Positive(message = "Tag ID must be a positive number.")
    private final Long id;

    @NotBlank(message = "Name cannot be blank.")
    @Size(max = 50, message = "Name cannot exceed 50 characters.")
    private final String name;

    private final String color;
}
