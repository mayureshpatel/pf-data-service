package com.mayureshpatel.pfdataservice.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * The request payload for updating an existing {@code Category}, identified by {@code id}.
 * {@code parentId} can be changed here, unlike most other update DTOs in this package that treat
 * structural fields as create-only -- re-parenting an existing category is a supported edit.
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class CategoryUpdateRequest {

    @NotNull(message = "Category ID cannot be null.")
    @Positive(message = "Category ID must be a positive number.")
    private final Long id;

    @NotNull(message = "User ID cannot be null.")
    @Positive(message = "User ID must be a positive number.")
    private final Long userId;

    @NotBlank(message = "Category name cannot be blank.")
    @Size(max = 50, message = "Category name must be less than 50 characters.")
    private final String name;

    @Size(max = 20, message = "Category type must be less than 20 characters.")
    private final String type;

    @Size(max = 20, message = "Category color must be less than 20 characters.")
    private final String color;

    @Size(max = 50, message = "Category icon must be less than 50 characters.")
    private final String icon;

    @Positive(message = "Parent ID must be a positive number.")
    private final Long parentId;
}
